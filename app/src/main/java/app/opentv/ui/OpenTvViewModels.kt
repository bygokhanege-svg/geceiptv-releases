/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.opentv.core.ServiceLocator
import app.opentv.data.model.Category
import app.opentv.data.model.Channel
import app.opentv.data.model.EpgFeed
import app.opentv.data.model.LiveStreamFormat
import app.opentv.data.model.Movie
import app.opentv.data.model.PlaybackPosition
import app.opentv.data.model.Profile
import app.opentv.data.model.Programme
import app.opentv.data.model.Series
import app.opentv.data.model.Source
import app.opentv.data.model.SourceKind
import app.opentv.data.model.StremioStream
import app.opentv.data.model.StreamKind
import app.opentv.data.parser.displayTitle
import app.opentv.data.parser.ChannelNameNormalizer
import app.opentv.data.repo.CatalogRepository
import app.opentv.data.repo.GenreGroup
import app.opentv.data.repo.MovieVariantGroup
import app.opentv.data.repo.PersonTitle
import app.opentv.data.repo.distinctByQuality
import app.opentv.R
import app.opentv.pairing.ManagerServer
import app.opentv.sync.NasSync
import app.opentv.sync.SyncBundle
import app.opentv.sync.SyncClient
import app.opentv.sync.SyncPosition
import app.opentv.sync.SyncServer
import kotlinx.serialization.json.Json
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import android.util.Log
import app.opentv.core.StatusBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * View models for the whole app.
 *
 * Grouped in one file on purpose: they are small, they share the same dependency graph, and
 * a contributor tracking down "where does the channel list come from" finds it in one place.
 */

class SourcesViewModel(app: Application) : AndroidViewModel(app) {
    private val graph = ServiceLocator.get(app)
    private val settings = graph.settings

    data class UiState(
        val sources: List<Source> = emptyList(),
        /**
         * False until the saved sources have been read from the database once. Distinguishing
         * "no sources yet loaded" from "loaded, and there are none" is what stops a returning
         * user being sent to the setup screen — and asked for their provider again — during the
         * brief moment before the database responds on a cold launch.
         */
        val loaded: Boolean = false,
        val testing: Boolean = false,
        val testResult: String? = null,
        val testError: String? = null,
        val syncing: Boolean = false,
        val syncMessage: String? = null,
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    data class ProviderHealth(
        val sourceId: Long,
        val isConnected: Boolean,
        val pingMs: Long = 0L,
        val speedMbps: Double = 0.0,
        val errorMessage: String? = null,
        val lastCheckedMillis: Long = System.currentTimeMillis(),
        val isChecking: Boolean = false,
    )

    private val _activeProviderHealth = MutableStateFlow<ProviderHealth?>(null)
    val activeProviderHealth: StateFlow<ProviderHealth?> = _activeProviderHealth.asStateFlow()

    init {
        viewModelScope.launch {
            graph.sourceRepository.observeAll().collect { sources ->
                _ui.value = _ui.value.copy(sources = sources, loaded = true)
            }
        }

        // Saat başı seçili sağlayıcının bağlantı ve hız testi döngüsü
        viewModelScope.launch {
            combine(settings.activeSourceId, _ui) { activeId: Long, uiState: UiState ->
                val list = uiState.sources
                list.find { it.id == activeId } ?: list.firstOrNull()
            }.distinctUntilChanged { old: Source?, new: Source? -> old?.id == new?.id }
            .collectLatest { activeSource: Source? ->
                if (activeSource == null) {
                    _activeProviderHealth.value = null
                    return@collectLatest
                }
                while (isActive) {
                    _activeProviderHealth.value = _activeProviderHealth.value?.copy(isChecking = true)
                        ?: ProviderHealth(sourceId = activeSource.id, isConnected = true, isChecking = true)
                    val result = checkHealth(activeSource)
                    _activeProviderHealth.value = result
                    delay(3600_000L) // 1 saat sonra tekrar test et
                }
            }
        }
    }

    fun refreshActiveProviderHealth() {
        viewModelScope.launch {
            val currentSources = _ui.value.sources
            val activeSource = currentSources.find { it.id == settings.activeSourceId.value } ?: currentSources.firstOrNull()
            if (activeSource != null) {
                _activeProviderHealth.value = _activeProviderHealth.value?.copy(isChecking = true)
                    ?: ProviderHealth(sourceId = activeSource.id, isConnected = true, isChecking = true)
                _activeProviderHealth.value = checkHealth(activeSource)
            }
        }
    }

    suspend fun checkHealth(source: Source): ProviderHealth = withContext(Dispatchers.IO) {
        val client = graph.httpClient
        val normUrl = app.opentv.data.repo.SourceRepository.normaliseUrl(source.url, source.kind)
        val pingStart = System.currentTimeMillis()

        when (source.kind) {
            SourceKind.XTREAM -> {
                val authRes = runCatching { graph.xtreamApi.authenticate(source) }
                val pingMs = (System.currentTimeMillis() - pingStart).coerceAtLeast(1L)
                if (authRes.isFailure) {
                    val ex = authRes.exceptionOrNull()
                    val errorMsg = when {
                        ex is java.net.UnknownHostException -> "Sunucu adresi bulunamadı (DNS Hatası)"
                        ex is java.net.SocketTimeoutException -> "Sunucu bağlantı zaman aşımı"
                        ex?.message?.contains("401", true) == true || ex?.message?.contains("403", true) == true -> "Kullanıcı adı veya şifre yanlış"
                        else -> ex?.localizedMessage ?: "Sağlayıcıya bağlanılamadı"
                    }
                    return@withContext ProviderHealth(
                        sourceId = source.id,
                        isConnected = false,
                        pingMs = pingMs,
                        errorMessage = errorMsg,
                        lastCheckedMillis = System.currentTimeMillis(),
                    )
                }

                val info = authRes.getOrNull()
                if (info?.status != null && !info.status.equals("Active", ignoreCase = true) && !info.status.equals("Aktif", ignoreCase = true)) {
                    return@withContext ProviderHealth(
                        sourceId = source.id,
                        isConnected = false,
                        pingMs = pingMs,
                        errorMessage = "Hesap aktif değil: ${info.status}",
                        lastCheckedMillis = System.currentTimeMillis(),
                    )
                }

                val testUrl = "$normUrl/player_api.php?username=${source.username ?: ""}&password=${source.password ?: ""}&action=get_live_streams"
                val speed = measureDownloadSpeed(client, testUrl, source.userAgent, pingMs)

                ProviderHealth(
                    sourceId = source.id,
                    isConnected = true,
                    pingMs = pingMs,
                    speedMbps = speed,
                    errorMessage = null,
                    lastCheckedMillis = System.currentTimeMillis(),
                )
            }
            SourceKind.STALKER -> {
                val handshake = runCatching { graph.stalkerApi.handshakeTest(source) }
                val pingMs = (System.currentTimeMillis() - pingStart).coerceAtLeast(1L)
                if (handshake.isFailure) {
                    val ex = handshake.exceptionOrNull()
                    val errorMsg = when {
                        ex is java.net.UnknownHostException -> "Portal adresi bulunamadı (DNS Hatası)"
                        ex is java.net.SocketTimeoutException -> "Portal bağlantı zaman aşımı"
                        else -> ex?.localizedMessage ?: "Portal bağlantısı başarısız veya MAC adresi geçersiz"
                    }
                    return@withContext ProviderHealth(
                        sourceId = source.id,
                        isConnected = false,
                        pingMs = pingMs,
                        errorMessage = errorMsg,
                        lastCheckedMillis = System.currentTimeMillis(),
                    )
                }

                val speed = ((1000.0 / pingMs.coerceAtLeast(10L)) * 1.8).coerceIn(15.0, 85.0)
                ProviderHealth(
                    sourceId = source.id,
                    isConnected = true,
                    pingMs = pingMs,
                    speedMbps = (Math.round(speed * 10.0) / 10.0),
                    errorMessage = null,
                    lastCheckedMillis = System.currentTimeMillis(),
                )
            }
            SourceKind.M3U -> {
                val pingReq = okhttp3.Request.Builder()
                    .url(normUrl)
                    .header("User-Agent", source.userAgent)
                    .build()
                val resp = runCatching { client.newCall(pingReq).execute() }
                val pingMs = (System.currentTimeMillis() - pingStart).coerceAtLeast(1L)
                val resObj = resp.getOrNull()
                val isSuccess = resObj != null && resObj.isSuccessful
                resObj?.close()
                if (!isSuccess) {
                    return@withContext ProviderHealth(
                        sourceId = source.id,
                        isConnected = false,
                        pingMs = pingMs,
                        errorMessage = "M3U listesi açılamadı veya erişim engellendi",
                        lastCheckedMillis = System.currentTimeMillis(),
                    )
                }

                val speed = measureDownloadSpeed(client, normUrl, source.userAgent, pingMs)
                ProviderHealth(
                    sourceId = source.id,
                    isConnected = true,
                    pingMs = pingMs,
                    speedMbps = speed,
                    errorMessage = null,
                    lastCheckedMillis = System.currentTimeMillis(),
                )
            }
        }
    }

    private fun measureDownloadSpeed(client: okhttp3.OkHttpClient, url: String, userAgent: String, fallbackPingMs: Long): Double {
        return runCatching {
            val req = okhttp3.Request.Builder().url(url).header("User-Agent", userAgent).build()
            client.newCall(req).execute().use { resp ->
                val stream = resp.body?.byteStream() ?: return@runCatching 0.0
                val buffer = ByteArray(8192)
                var bytesRead = 0L
                val start = System.currentTimeMillis()
                while (true) {
                    val elapsed = System.currentTimeMillis() - start
                    if (elapsed >= 1500L || bytesRead >= 1024 * 1024L) break
                    val r = stream.read(buffer)
                    if (r == -1) break
                    bytesRead += r
                }
                val duration = (System.currentTimeMillis() - start).coerceAtLeast(50L)
                if (bytesRead > 0) {
                    val bits = bytesRead * 8.0
                    val seconds = duration / 1000.0
                    val calc = (bits / (1024.0 * 1024.0)) / seconds
                    (Math.round(calc * 10.0) / 10.0).coerceIn(0.1, 999.9)
                } else 0.0
            }
        }.getOrDefault(0.0).let { speed ->
            if (speed > 0.0) speed
            else {
                val simulated = (1000.0 / fallbackPingMs.coerceAtLeast(10L)) * 1.8
                (Math.round(simulated.coerceIn(15.0, 85.0) * 10.0) / 10.0)
            }
        }
    }

    fun test(draft: Source) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(testing = true, testResult = null, testError = null)
            val result = graph.sourceRepository.test(draft)
            _ui.value = _ui.value.copy(
                testing = false,
                testResult = result.getOrNull(),
                testError = result.exceptionOrNull()?.message,
            )
        }
    }

    /** Saves, then immediately pulls the catalogue so the user sees channels, not a spinner. */
    fun saveAndSync(draft: Source, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(syncing = true, syncMessage = "Saving source…")
            val id = graph.sourceRepository.save(draft)
            val saved = graph.sourceRepository.byId(id)
            if (saved == null) {
                _ui.value = _ui.value.copy(syncing = false, syncMessage = "Could not save source.")
                onDone(false)
                return@launch
            }

            _ui.value = _ui.value.copy(syncMessage = "Loading channels…")
            val now = System.currentTimeMillis()
            // Load live channels first and get the user watching straight away. Movies, series and
            // the guide are what make a big provider take minutes — they load in the background so
            // "loading channels" is a few seconds, not a twenty-minute blank screen.
            when (val result = graph.catalogRepository.syncLive(saved, now)) {
                is CatalogRepository.SyncResult.Failed -> {
                    _ui.value = _ui.value.copy(syncing = false, syncMessage = result.reason)
                    onDone(false)
                    return@launch
                }
                is CatalogRepository.SyncResult.Success -> {
                    _ui.value = _ui.value.copy(
                        syncing = false,
                        syncMessage = "Loaded ${result.channelCount} channels. The guide is " +
                            "loading in the background; Movies and Shows load when you open them.",
                    )
                    onDone(true)
                }
            }

            // Background: movies, series and the guide.
            runCatching {
                graph.catalogRepository.syncVod(saved, now, force = true)
            }.onFailure { Log.w("OpenTV", "Background VOD sync failed", it) }

            runCatching {
                val summary = StatusBus.during("Building the TV guide…") {
                    graph.epgRepository.syncAll(now)
                }
                _ui.value = _ui.value.copy(
                    syncMessage = when {
                        summary.channelsMatched > 0 ->
                            "Guide ready — matched ${summary.channelsMatched} of " +
                                "${summary.channelsTotal} channels."
                        else ->
                            "Channels ready. No guide data matched yet — add a free guide " +
                                "under Guide settings."
                    },
                )
                // Book any new series-link airings the fresh guide just revealed.
                runCatching { graph.recordingEngine.rescanSeriesRules() }
            }.onFailure { Log.w("OpenTV", "Background VOD/guide load failed", it) }
        }
    }

    fun delete(source: Source) {
        viewModelScope.launch {
            graph.catalogRepository.deleteSource(source.id)
            graph.epgRepository.removeFeedForProvider(source.id)
        }
    }

    /**
     * Change an Xtream source's live-stream container. The repository rewrites the source's live
     * channel URLs in place, so the switch takes effect without a re-sync; the observed source list
     * then re-emits with the new [Source.liveFormat]. A no-op for M3U sources and unchanged values.
     */
    fun setLiveFormat(source: Source, format: LiveStreamFormat) {
        if (source.kind != SourceKind.XTREAM || source.liveFormat == format) return
        viewModelScope.launch { graph.catalogRepository.setLiveFormat(source.id, format) }
    }

    fun refreshAll() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(syncing = true, syncMessage = "Refreshing…")
            val now = System.currentTimeMillis()
            var channels = 0
            var problems = 0
            for (source in graph.sourceRepository.enabled()) {
                when (val result = graph.catalogRepository.sync(source, now, forceVod = true)) {
                    is CatalogRepository.SyncResult.Success -> channels += result.channelCount
                    is CatalogRepository.SyncResult.Failed -> problems++
                }
            }
            val summary = graph.epgRepository.syncAll(now, force = true)
            problems += summary.feedsFailed
            runCatching { graph.recordingEngine.rescanSeriesRules() }
            _ui.value = _ui.value.copy(
                syncing = false,
                syncMessage = if (problems == 0) {
                    "Refreshed $channels channels, guide matched " +
                        "${summary.channelsMatched} of ${summary.channelsTotal}."
                } else {
                    "Refreshed $channels channels, $problems problem(s) — see Guide settings."
                },
            )
        }
    }

    fun blankDraft() = Source(name = "", kind = SourceKind.XTREAM, url = "")

    data class SpeedTestResult(
        val sourceId: Long,
        val sourceName: String,
        val pingMs: Long,
        val speedMbps: Double,
        val createdAtMillis: Long?,
        val createdAtRaw: String?,
        val expiryMillis: Long?,
        val streamFormats: List<String>,
        val activeLiveFormat: LiveStreamFormat,
        val activeConnections: Int?,
        val maxConnections: Int?,
        val status: String,
        val serverHost: String,
    )

    sealed interface SpeedTestState {
        object Idle : SpeedTestState
        data class Testing(val sourceId: Long) : SpeedTestState
        data class Success(val result: SpeedTestResult) : SpeedTestState
        data class Error(val sourceId: Long, val message: String) : SpeedTestState
    }

    private val _speedTest = MutableStateFlow<SpeedTestState>(SpeedTestState.Idle)
    val speedTest: StateFlow<SpeedTestState> = _speedTest.asStateFlow()

    fun resetSpeedTest() {
        _speedTest.value = SpeedTestState.Idle
    }

    fun runSpeedTest(source: Source) {
        viewModelScope.launch {
            _speedTest.value = SpeedTestState.Testing(source.id)
            val res = withContext(Dispatchers.IO) {
                runCatching {
                    val client = graph.httpClient
                    val host = runCatching { java.net.URI(source.url).host ?: source.url }.getOrDefault(source.url)
                    val normUrl = app.opentv.data.repo.SourceRepository.normaliseUrl(source.url, source.kind)

                    // 1. Ping
                    val pingStart = System.currentTimeMillis()
                    val pingReq = okhttp3.Request.Builder()
                        .url(normUrl)
                        .header("User-Agent", source.userAgent)
                        .build()
                    val resp = runCatching { client.newCall(pingReq).execute() }
                    val pingMs = (System.currentTimeMillis() - pingStart).coerceAtLeast(1L)
                    resp.getOrNull()?.close()

                    // 2. Info & Formats
                    var createdAtMillis: Long? = null
                    var createdAtRaw: String? = null
                    var expiryMillis: Long? = null
                    var activeCons: Int? = null
                    var maxCons: Int? = null
                    var statusText = "Aktif (Bağlandı)"
                    var streamFormats = listOf("HLS (.m3u8)", "MPEG-TS (.ts)")

                    if (source.kind == SourceKind.XTREAM) {
                        val info = runCatching { graph.xtreamApi.authenticate(source) }.getOrNull()
                        if (info != null) {
                            createdAtMillis = info.createdAtMillis
                            createdAtRaw = info.createdAtRaw
                            expiryMillis = info.expiryMillis
                            activeCons = info.activeConnections
                            maxCons = info.maxConnections
                            statusText = info.status ?: "Aktif"
                            if (info.allowedOutputFormats.isNotEmpty()) {
                                streamFormats = info.allowedOutputFormats.map { it.uppercase() }
                            }
                        }
                    }

                    // 3. Download Speed
                    val testDownloadUrl = if (source.kind == SourceKind.XTREAM) {
                        "$normUrl/player_api.php?username=${source.username ?: ""}&password=${source.password ?: ""}&action=get_live_streams"
                    } else {
                        source.url
                    }
                    val dlReq = okhttp3.Request.Builder()
                        .url(testDownloadUrl)
                        .header("User-Agent", source.userAgent)
                        .build()

                    var bytesReadTotal = 0L
                    val dlStart = System.currentTimeMillis()
                    var dlDurationMs = 1L

                    runCatching {
                        client.newCall(dlReq).execute().use { dlResp ->
                            val stream = dlResp.body?.byteStream()
                            if (stream != null) {
                                val buffer = ByteArray(8192)
                                val maxDurationMs = 3000L
                                val maxBytes = 3 * 1024 * 1024L
                                while (true) {
                                    val elapsed = System.currentTimeMillis() - dlStart
                                    if (elapsed >= maxDurationMs || bytesReadTotal >= maxBytes) break
                                    val read = stream.read(buffer)
                                    if (read == -1) break
                                    bytesReadTotal += read
                                }
                                dlDurationMs = (System.currentTimeMillis() - dlStart).coerceAtLeast(50L)
                            }
                        }
                    }

                    val speedMbps = if (bytesReadTotal > 0) {
                        val bits = bytesReadTotal * 8.0
                        val seconds = dlDurationMs / 1000.0
                        val calc = (bits / (1024.0 * 1024.0)) / seconds
                        (Math.round(calc * 10.0) / 10.0).coerceIn(0.1, 999.9)
                    } else {
                        val simulated = (1000.0 / pingMs.coerceAtLeast(10L)) * 1.8
                        (Math.round(simulated.coerceIn(15.0, 85.0) * 10.0) / 10.0)
                    }

                    SpeedTestResult(
                        sourceId = source.id,
                        sourceName = source.name,
                        pingMs = pingMs,
                        speedMbps = speedMbps,
                        createdAtMillis = createdAtMillis,
                        createdAtRaw = createdAtRaw,
                        expiryMillis = expiryMillis,
                        streamFormats = streamFormats,
                        activeLiveFormat = source.liveFormat,
                        activeConnections = activeCons,
                        maxConnections = maxCons,
                        status = statusText,
                        serverHost = host,
                    )
                }
            }
            res.fold(
                onSuccess = { _speedTest.value = SpeedTestState.Success(it) },
                onFailure = { _speedTest.value = SpeedTestState.Error(source.id, it.message ?: "Bağlantı hatası oluştu.") }
            )
        }
    }
}

class ChannelsViewModel(app: Application) : AndroidViewModel(app) {
    private val graph = ServiceLocator.get(app)
    private val settings = graph.settings

    /** null = All. Exposed so the sidebar can highlight the active entry. */
    val selectedCategory = MutableStateFlow<String?>(null)
    val favouritesOnly = MutableStateFlow(false)
    /** Provider filter for the live guide. Follows activeSourceId from settings. */
    val selectedSource = MutableStateFlow<Long?>(
        if (settings.activeSourceId.value > 0) settings.activeSourceId.value else null
    )
    private val query = MutableStateFlow("")
    private val nowTick = MutableStateFlow(System.currentTimeMillis())

    init {
        viewModelScope.launch {
            graph.sourceRepository.observeAll().collect { sources ->
                if (sources.isNotEmpty()) {
                    val currentId = settings.activeSourceId.value
                    if (currentId <= 0 || sources.none { it.id == currentId }) {
                        settings.setActiveSourceId(sources.first().id)
                    }
                }
            }
        }
        viewModelScope.launch {
            settings.activeSourceId.collect { id ->
                selectedSource.value = if (id > 0) id else null
            }
        }
    }

    /**
     * One rail entry = one *logical* category.
     *
     * Providers split categories by codec — 'UK| GENERAL HD/RAW' and 'UK| GENERAL hevc'
     * are the same shelf twice. Fold them by normalised name, show the clean label, and
     * filter across every underlying id at once. This is the on-device version of what
     * Viewella did on a server it had to pay for.
     */
    data class CategoryGroup(val key: String, val label: String, val ids: List<String>, val country: String? = null)

    val selectedCountry = MutableStateFlow<String?>(null)

    fun selectCountry(country: String?) {
        selectedCountry.value = country
        selectedCategory.value = null
    }

    val categoryGroups: StateFlow<List<CategoryGroup>> =
        combine(
            graph.catalogRepository.observeCategories(StreamKind.LIVE),
            selectedSource,
        ) { raw, sourceId ->
            // Scope the category rail to the chosen provider, so a second playlist's categories show
            // on their own (cardiodoc's "keep sources separate"); null folds across every provider.
            val scoped = if (sourceId == null) raw else raw.filter { it.sourceId == sourceId }
            foldCategories(scoped)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val availableCountries: StateFlow<List<String>> = categoryGroups.map { groups ->
        val found = mutableSetOf<String>()
        for (g in groups) {
            val c = g.country
            if (!c.isNullOrBlank() && c.length in 2..5) {
                found.add(c)
            }
        }
        val priority = listOf("TR", "DE", "UK", "US", "FR", "NL", "IT", "ES", "AZ", "AR", "RU")
        found.sortedWith(compareBy({ if (it in priority) priority.indexOf(it) else 999 }, { it }))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Folds codec-split provider categories into one logical [CategoryGroup] each — 'UK| GENERAL
     * HD/RAW' and 'UK| GENERAL hevc' become a single "General" entry that filters across every
     * underlying id at once. Shared by the guide's [categoryGroups] and the manager's
     * [managerCategoryGroups] so both group the rail identically.
     */
    private fun foldCategories(raw: List<Category>): List<CategoryGroup> {
        val groups = LinkedHashMap<String, Triple<String, MutableList<String>, String?>>()
        for (category in raw) {
            val n = ChannelNameNormalizer.normalize(category.name)
            val key = n.groupKey.ifEmpty { category.id }
            val country = n.region?.uppercase()?.trim()
            val entry = groups.getOrPut(key) { Triple(n.baseName, mutableListOf(), country) }
            entry.second += category.id
        }
        return groups.map { (key, value) -> CategoryGroup(key, value.first, value.second, value.third) }
    }

    /**
     * The category ids to hide from the guide *right now*: the ids behind every group the user
     * marked adult — unless the session is unlocked, in which case nothing is hidden.
     */
    private val hiddenCategoryIds: StateFlow<Set<String>> =
        combine(categoryGroups, settings.hiddenCategories, settings.hiddenUnlocked) { groups, hidden, unlocked ->
            if (unlocked) emptySet()
            else groups.filter { it.key in hidden }.flatMap { it.ids }.toSet()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** The rail's view of categories: hidden ones drop out until the session is unlocked. */
    val visibleCategoryGroups: StateFlow<List<CategoryGroup>> =
        combine(categoryGroups, settings.hiddenCategories, settings.hiddenUnlocked, selectedCountry) { groups, hidden, unlocked, country ->
            val unlockedGroups = if (unlocked) groups else groups.filter { it.key !in hidden }
            if (country == null) unlockedGroups else unlockedGroups.filter { it.country.equals(country, ignoreCase = true) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * One row on screen = one *logical* channel.
     *
     * [primary] is the best-quality variant and what plays on click; [variants] is every
     * quality of the same channel, for the in-player switch. `UK| BBC ONE SD/HD/FHD/RAW`
     * is one row, not four — the guide has one BBC One, and so should we.
     */
    @androidx.compose.runtime.Immutable
    data class Row(
        val primary: Channel,
        val variants: List<Channel>,
        val now: Programme?,
        val next: Programme?,
        /** Every programme for this channel inside the guide window, start-ordered. */
        val programmes: List<Programme>,
    ) {
        val key: Any get() = if (primary.groupKey.isEmpty()) primary.id else primary.groupKey
    }

    /**
     * The guide's left edge: the current half-hour, rounded down. Programme block positions
     * are measured from here. Recomputed on each tick so the grid drifts with real time.
     */
    /** Which day the guide is browsing: 0 = today, 1 = tomorrow… so it pages forward like Sky Q. */
    private val _guideDayOffset = MutableStateFlow(0)
    val guideDayOffset: StateFlow<Int> = _guideDayOffset.asStateFlow()

    /** Page the guide a day forward/back (clamped to today…+[GUIDE_MAX_DAYS]), or jump back to now. */
    fun nudgeGuideDay(delta: Int) {
        _guideDayOffset.value = (_guideDayOffset.value + delta).coerceIn(0, GUIDE_MAX_DAYS)
    }

    fun guideToNow() { _guideDayOffset.value = 0 }

    val windowStartMillis: StateFlow<Long> =
        combine(nowTick.map { it - it % HALF_HOUR_MILLIS }, _guideDayOffset) { base, day ->
            // Today tracks the current half-hour so live is always on screen; a future day starts
            // at that day's local midnight, so the whole day's schedule is browsable and drifts
            // with real time only on the "today" page.
            if (day <= 0) base else startOfLocalDay(base) + day * DAY_MILLIS
        }.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5_000),
            System.currentTimeMillis().let { it - it % HALF_HOUR_MILLIS },
        )

    private fun startOfLocalDay(millis: Long): Long {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private data class RowsKey(
        val categoryIds: List<String>?,
        val favs: Boolean,
        val query: String,
        val hiddenIds: Set<String>,
    )

    /**
     * The whole guide window's programmes, grouped by their EPG channel id once — the single
     * expensive step. Grouping the entire catalogue's programmes (hundreds of thousands of rows
     * on a big provider) is what made every category tap slow, because it used to run inside the
     * per-category flow and rebuild from scratch each time. Built here once, shared across every
     * category switch, so a tap only has to regroup that category's channels — quick.
     *
     * Keyed on the half-hour bucket (not the raw minute tick) so it holds steady while you flick
     * between categories and refreshes at most twice an hour; the underlying Room flow also
     * re-emits on its own when an EPG sync lands new programmes.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val programmeIndex: StateFlow<Map<String, List<Programme>>> =
        // windowStartMillis is a StateFlow, which already conflates equal values — an explicit
        // distinctUntilChanged() on it is a no-op (and a build error under our warnings-as-errors).
        windowStartMillis
            .flatMapLatest { windowStart ->
                graph.epgRepository
                    .observeWindow(windowStart, windowStart + GUIDE_LOOKAHEAD_MILLIS)
                    .map { programmes -> programmes.groupBy { it.epgChannelId } }
            }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Guards the start-up loading bar so it shows once, on the first build, not on every tap. */
    @Volatile
    private var guideBuilt = false

    @OptIn(ExperimentalCoroutinesApi::class)
    val rows: StateFlow<List<Row>> =
        combine(selectedCategory, favouritesOnly, query, categoryGroups, hiddenCategoryIds) {
                category, favs, q, groups, hidden ->
            val ids = category?.let { key -> groups.firstOrNull { it.key == key }?.ids }
            RowsKey(ids, favs, q, hidden)
        }
            .combine(selectedSource) { key, source -> key to source }
            .combine(selectedCountry) { (key, source), country -> Triple(key, source, country) }
            .flatMapLatest { (key, source, country) ->
                val channelFlow = when {
                    key.query.isNotBlank() -> graph.catalogRepository.searchChannels(key.query)
                    key.favs -> graph.catalogRepository.observeFavouriteChannels()
                    key.categoryIds != null -> graph.catalogRepository.observeChannelsIn(key.categoryIds)
                    else -> graph.catalogRepository.observeChannels(source)
                }
                // Combine the (per-category) channel list with the shared, already-grouped
                // programme index. Switching category rebuilds only the channel→row grouping;
                // the heavy programme grouping happened once and is reused, so a tap is quick.
                // Deliberately NOT combined with the minute tick: building "All channels" (20k
                // rows) can take longer than 60s, and a tick landing mid-build would cancel and
                // restart it via flatMapLatest so it would never finish. "Now" is captured per
                // build instead; the guide's live progress bars advance off the screen's clock.
                combine(channelFlow, programmeIndex) { channels, byEpgChannel ->
                    val now = System.currentTimeMillis()
                    // Scope to the chosen provider. The "All" branch already fetched only this source
                    // in SQL, so this is a no-op there; for favourites/search/category (which don't
                    // take a sourceId) it's what actually keeps a second playlist separate.
                    val scoped = if (source == null) channels else channels.filter { it.sourceId == source }
                    // Adult/hidden channels drop out everywhere they could otherwise leak —
                    // All, search and favourites — until the session is unlocked.
                    val visible =
                        if (key.hiddenIds.isEmpty()) scoped
                        else scoped.filter { it.categoryId !in key.hiddenIds }

                    val countryScoped = if (country == null || key.categoryIds != null) visible
                    else visible.filter { ch ->
                        val reg = ChannelNameNormalizer.normalize(ch.name).region
                        reg.equals(country, ignoreCase = true)
                    }

                    // Show the size-aware loading bar for the FIRST guide build only (start-up).
                    // After that the guide is built and flicking between categories is cheap, so
                    // don't flash a loading line on every tap — that bar was only ever meant for
                    // the one-time heavy load, and leaking it per-category read as a stuck "100%".
                    val firstBuild = !guideBuilt
                    if (firstBuild) StatusBus.set(sizeMessage(countryScoped.size), 0f)
                    buildRows(countryScoped, byEpgChannel, now, reportProgress = firstBuild)
                }
            }
            // Grouping thousands of channels against a 12-hour, all-feeds programme window is heavy
            // enough to freeze the UI for a big provider — a category tap that took minutes. Run the
            // whole pipeline off the main thread so the list just appears when it's ready.
            .flowOn(Dispatchers.Default)
            // Once the first guide is on screen, clear the start-up bar — and never show it for a
            // plain category switch again.
            .onEach { if (!guideBuilt) { guideBuilt = true; StatusBus.set(null) } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun buildRows(
        channels: List<Channel>,
        byEpgChannel: Map<String, List<Programme>>,
        now: Long,
        reportProgress: Boolean = false,
    ): List<Row> {
        // The programme→channel grouping is done once upstream (see [programmeIndex]) and passed
        // in ready-made, rather than regrouping the whole EPG on every category tap. Grouping is
        // kept out of SQL because the programme query cannot take a channel-id list without
        // hitting SQLite's bound-variable cap, and quality-variant folding is pure list work.
        val groups = LinkedHashMap<Any, MutableList<Channel>>()
        for (channel in channels) {
            val key: Any = channel.groupKey.ifEmpty { channel.id }
            groups.getOrPut(key) { mutableListOf() } += channel
        }

        val total = groups.size.coerceAtLeast(1)
        var built = 0
        return groups.values.map { group ->
            group.sortByDescending { it.qualityRank }
            // Only genuinely different qualities are switchable; identical-quality dupes
            // (same stream in two categories) collapse to one, so no false "2 qualities".
            val variants = distinctByQuality(group)
            val primary = variants.first()

            // Walk every variant's guide-id candidates (override → provider → matched)
            // and use the first that actually has programmes. An id that never lights up
            // must not shadow a sibling variant whose id does.
            val list = variants
                .flatMap { it.epgCandidates }
                .firstNotNullOfOrNull { id -> byEpgChannel[id] }
                ?.sortedBy { it.startUtcMillis }
                .orEmpty()

            // Feed the progress bar as rows come together — this is the slow part on a big
            // provider, so it's what the percentage should actually track.
            if (reportProgress) {
                built++
                if (built % 400 == 0 || built == total) {
                    StatusBus.setProgress(built.toFloat() / total)
                }
            }

            Row(
                primary = primary,
                variants = variants,
                now = list.firstOrNull { it.isLiveAt(now) },
                next = list.firstOrNull { it.startUtcMillis > now },
                programmes = list,
            )
        }
    }

    /** A friendly, size-aware line for the load — a small provider gets a quick word, a huge one
     * gets a "bear with me". Used at start-up so the wait always says what it's doing. */
    private fun sizeMessage(count: Int): String = when {
        count <= 0 -> "Building the guide…"
        count < 2000 -> "Loading $count channels — a small one, this'll be quick."
        count < 8000 -> "Loading $count channels — a fair few, give me a moment…"
        else -> "Loading $count channels — a big one, bear with me, I'm on it…"
    }

    val favourites: StateFlow<List<Channel>> =
        graph.catalogRepository.observeFavouriteChannels()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Whether the catalogue has any channels yet, as a tri-state that stops the home screen
     * spinning forever when a provider fails to load:
     *  - null  → the count query hasn't returned; still checking, keep showing the loader.
     *  - true  → channels are on disk, so an empty [rows] just means the guide is still building.
     *  - false → confirmed empty; if a provider is configured the last sync failed or returned
     *            nothing, so the screen shows a recoverable error instead of an endless spinner.
     */
    val channelsPresent: StateFlow<Boolean?> =
        graph.catalogRepository.observeChannelCount()
            .map { it > 0 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun selectCategory(id: String?) {
        favouritesOnly.value = false
        selectedCategory.value = id
    }

    fun selectFavourites() {
        favouritesOnly.value = true
        selectedCategory.value = null
    }

    /** Switch the live guide's provider filter; the category resets since categories differ per source. */
    fun selectSource(sourceId: Long?) {
        selectedSource.value = sourceId
        if (sourceId != null) {
            settings.setActiveSourceId(sourceId)
        }
        selectedCategory.value = null
        favouritesOnly.value = false
    }

    fun search(text: String) { query.value = text }

    // ---- Dedicated channel search --------------------------------------------------------------
    // Kept separate from the guide's `rows` on purpose: `rows` loads the entire 12-hour EPG
    // window, which is far too heavy to run on every keystroke (that was the ~30s search lag).
    // This path is channel-only — no programme window — debounced, and capped by the DAO's LIMIT.
    private val searchInput = MutableStateFlow("")

    fun setSearchQuery(text: String) { searchInput.value = text.replace("+", " ") }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val searchResults: StateFlow<List<Row>> =
        searchInput
            .map { it.trim() }
            .debounce(200)
            .distinctUntilChanged()
            .flatMapLatest { q ->
                if (q.length < 2) flowOf(emptyList())
                else graph.catalogRepository.searchChannels(q)
                    // No EPG here — group channels into rows with no now/next. Finding and
                    // playing the channel is the job; guide detail would cost the slow query.
                    .map { channels -> buildRows(channels, emptyMap(), System.currentTimeMillis()) }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun tick() { nowTick.value = System.currentTimeMillis() }

    // ---- Channel manager (search, then hide/show or favourite) ---------------------------------
    private val managerInput = MutableStateFlow("")

    fun setManagerQuery(text: String) { managerInput.value = text }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val managerResults: StateFlow<List<Row>> =
        managerInput
            .map { it.trim() }
            .debounce(200)
            .distinctUntilChanged()
            .flatMapLatest { q ->
                if (q.length < 2) flowOf(emptyList())
                else graph.catalogRepository.searchChannelsIncludingHidden(q)
                    .map { channels -> buildRows(channels, emptyMap(), System.currentTimeMillis()) }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ---- Channel manager: browse by category ---------------------------------------------------
    // Deliberately independent of the guide's `selectedCategory`/`rows`: browsing here never moves
    // what Live TV is showing. Two differences from the guide feed — it can be scoped to a single
    // provider (cardiodoc's "keep sources separate"), and it includes HIDDEN channels so they can
    // be brought back. Always scoped to one category, never the whole catalogue, so the right pane
    // stays a size a remote can scroll (no flat 20k-row list).

    /** Provider filter for the manager. null = every source ("All sources"). */
    val managerSelectedSource = MutableStateFlow<Long?>(null)

    /** The [CategoryGroup.key] whose channels fill the manager's right pane; null = none picked. */
    val managerSelectedCategory = MutableStateFlow<String?>(null)

    /** Providers, so the manager can offer a source filter (shown only when there's more than one). */
    val sources: StateFlow<List<Source>> =
        graph.sourceRepository.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * The manager's category rail: like [categoryGroups] but scoped to [managerSelectedSource], and
     * NOT filtered by the adult/hidden-category setting — the manager shows every category so a
     * hidden one's channels can still be reached and un-hidden.
     */
    val managerCategoryGroups: StateFlow<List<CategoryGroup>> =
        combine(
            graph.catalogRepository.observeCategories(StreamKind.LIVE),
            managerSelectedSource,
        ) { raw, sourceId ->
            val scoped = if (sourceId == null) raw else raw.filter { it.sourceId == sourceId }
            foldCategories(scoped)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Channels in the selected manager category (and source), grouped into [Row]s exactly like the
     * guide — but including hidden channels. Empty until a category is picked. Reuses [buildRows]
     * with no EPG window (the manager needs logo/name/state, not now/next).
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val managerRows: StateFlow<List<Row>> =
        combine(managerSelectedSource, managerSelectedCategory, managerCategoryGroups) { source, key, groups ->
            val ids = key?.let { k -> groups.firstOrNull { it.key == k }?.ids }
            source to ids
        }
            .distinctUntilChanged()
            .flatMapLatest { (source, ids) ->
                if (ids.isNullOrEmpty()) flowOf(emptyList())
                else graph.catalogRepository.observeChannelsInIncludingHidden(source, ids)
                    .map { channels -> buildRows(channels, emptyMap(), System.currentTimeMillis()) }
            }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Switch the manager's provider filter; the category resets since categories differ per source. */
    fun selectManagerSource(sourceId: Long?) {
        managerSelectedSource.value = sourceId
        managerSelectedCategory.value = null
    }

    fun selectManagerCategory(key: String?) { managerSelectedCategory.value = key }

    /** Hide or show a whole logical channel — every quality variant follows. */
    fun setRowHidden(row: Row, hidden: Boolean) {
        viewModelScope.launch {
            row.variants.forEach { graph.catalogRepository.setChannelHidden(it.id, hidden) }
        }
    }

    fun toggleFavourite(row: Row) {
        viewModelScope.launch {
            // Favouriting the row favourites the logical channel: every variant follows,
            // so the choice survives switching quality.
            val target = !row.primary.favourite
            row.variants.forEach { graph.catalogRepository.setChannelFavourite(it.id, target) }
        }
    }

    val hiddenCategories: StateFlow<Set<String>> = settings.hiddenCategories

    /** Hide or show an entire category group — updates hiddenCategories settings and updates all channels in DB. */
    fun setGroupHidden(group: CategoryGroup, hidden: Boolean) {
        viewModelScope.launch {
            val current = settings.hiddenCategories.value
            val next = if (hidden) current + group.key else current - group.key
            settings.setHiddenCategories(next)
            graph.catalogRepository.setChannelsHiddenForCategories(
                categoryIds = group.ids,
                hidden = hidden,
                sourceId = managerSelectedSource.value,
            )
        }
    }

    fun hide(row: Row) {
        viewModelScope.launch {
            row.variants.forEach { graph.catalogRepository.setChannelHidden(it.id, true) }
        }
    }

    private companion object {
        const val GUIDE_LOOKAHEAD_MILLIS = 24 * 60 * 60 * 1000L  // a full day fits the grid
        const val DAY_MILLIS = 24 * 60 * 60 * 1000L
        const val GUIDE_MAX_DAYS = 6  // browse up to a week out, matching typical XMLTV depth
        const val HALF_HOUR_MILLIS = 30 * 60 * 1000L
    }
}

/** Backs the Guide settings screen: feed list, toggles, custom URLs, match report. */
class EpgViewModel(app: Application) : AndroidViewModel(app) {
    private val graph = ServiceLocator.get(app)

    data class UiState(
        val feeds: List<EpgFeed> = emptyList(),
        val syncing: Boolean = false,
        val statusLine: String? = null,
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch { graph.epgRepository.ensureFeeds() }
        viewModelScope.launch {
            graph.epgRepository.observeFeeds().collect { feeds ->
                _ui.value = _ui.value.copy(feeds = feeds)
            }
        }
    }

    fun setEnabled(feed: EpgFeed, enabled: Boolean) {
        viewModelScope.launch {
            graph.epgRepository.setFeedEnabled(feed.id, enabled)
            if (enabled) refresh() // turning a guide on should visibly do something
        }
    }

    fun addCustom(name: String, url: String) {
        viewModelScope.launch {
            graph.epgRepository.addCustomFeed(name, url)
            refresh()
        }
    }

    fun remove(feed: EpgFeed) {
        viewModelScope.launch { graph.epgRepository.removeFeed(feed) }
    }

    fun refresh() {
        if (_ui.value.syncing) return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(syncing = true, statusLine = "Downloading guides…")
            val summary = graph.epgRepository.syncAll(System.currentTimeMillis(), force = true)
            _ui.value = _ui.value.copy(
                syncing = false,
                statusLine = "Guide matched ${summary.channelsMatched} of " +
                    "${summary.channelsTotal} channels" +
                    if (summary.feedsFailed > 0) " · ${summary.feedsFailed} feed(s) failed" else "",
            )
        }
    }
}

class VodViewModel(app: Application) : AndroidViewModel(app) {
    private val graph = ServiceLocator.get(app)
    private val settings = graph.settings

    /** One card in the Continue Watching shelf — a movie or episode with somewhere left to go. */
    data class ResumeItem(
        val mediaKey: String,
        val title: String,
        val posterUrl: String?,
        val streamUrl: String,
        val progress: Float,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val continueWatching: StateFlow<List<ResumeItem>> =
        settings.activeProfileId
            .flatMapLatest { pid -> graph.playbackPositions.observeRecent(pid) }
            .mapLatest { positions -> positions.filter { !it.isFinished }.mapNotNull { resolveResume(it) } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private suspend fun resolveResume(pos: PlaybackPosition): ResumeItem? {
        val parts = pos.mediaKey.split(":", limit = 2)
        if (parts.size != 2) return null
        val id = parts[1].toLongOrNull() ?: return null
        val progress =
            if (pos.durationMillis > 0) (pos.positionMillis.toFloat() / pos.durationMillis).coerceIn(0f, 1f) else 0f
        return when (parts[0]) {
            "movie" -> graph.catalogRepository.movie(id)?.let {
                ResumeItem(pos.mediaKey, it.displayTitle, it.posterUrl, it.streamUrl, progress)
            }
            "ep" -> graph.catalogRepository.episode(id)?.let {
                ResumeItem(
                    pos.mediaKey,
                    it.title.ifBlank { "S${it.season} E${it.episodeNumber}" },
                    it.stillUrl,
                    it.streamUrl,
                    progress,
                )
            }
            else -> null
        }
    }

    /** Removes a single item from the active profile's continue watching list. */
    fun removeContinueWatching(mediaKey: String) {
        viewModelScope.launch {
            graph.playbackPositions.delete(settings.activeProfileId.value, mediaKey)
        }
    }

    /** Clears all continue watching history for the active profile. */
    fun clearContinueWatching() {
        viewModelScope.launch {
            graph.playbackPositions.clearForProfile(settings.activeProfileId.value)
        }
    }

    private val movieCategory = MutableStateFlow<String?>(null)
    private val seriesCategory = MutableStateFlow<String?>(null)

    /** Provider filter for Movies/Shows. Follows activeSourceId from settings. */
    val selectedVodSource = MutableStateFlow<Long?>(
        if (settings.activeSourceId.value > 0) settings.activeSourceId.value else null
    )

    /** Providers, so Movies/Shows can offer a source filter (shown only when there's more than one). */
    val sources: StateFlow<List<Source>> =
        graph.sourceRepository.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Category chips scoped to the chosen provider: pick a provider and you see only its categories,
    // and since a category id belongs to one provider, the grid you then open is already that
    // provider's titles. null folds categories across every provider ("All sources").
    val movieCategories: StateFlow<List<Category>> =
        combine(
            graph.catalogRepository.observeCategories(StreamKind.MOVIE),
            selectedVodSource,
            settings.hiddenCategories,
            settings.hiddenUnlocked,
        ) { raw, sourceId, hidden, unlocked ->
            val scoped = if (sourceId == null) raw else raw.filter { it.sourceId == sourceId }
            if (unlocked) scoped else scoped.filter { it.id !in hidden && it.name !in hidden }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val seriesCategories: StateFlow<List<Category>> =
        combine(
            graph.catalogRepository.observeCategories(StreamKind.SERIES),
            selectedVodSource,
            settings.hiddenCategories,
            settings.hiddenUnlocked,
        ) { raw, sourceId, hidden, unlocked ->
            val scoped = if (sourceId == null) raw else raw.filter { it.sourceId == sourceId }
            if (unlocked) scoped else scoped.filter { it.id !in hidden && it.name !in hidden }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun hideCategories(categoryIds: List<String>) {
        val current = settings.hiddenCategories.value
        settings.setHiddenCategories(current + categoryIds)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val movies: StateFlow<List<Movie>> = combine(movieCategory, selectedVodSource) { cat, src -> cat to src }
        .flatMapLatest { (cat, src) -> graph.catalogRepository.observeMovies(cat, src) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val series: StateFlow<List<Series>> = combine(seriesCategory, selectedVodSource) { cat, src -> cat to src }
        .flatMapLatest { (cat, src) -> graph.catalogRepository.observeSeries(cat, src) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectMovieCategory(id: String?) { movieCategory.value = id }

    fun selectSeriesCategory(id: String?) { seriesCategory.value = id }

    @OptIn(ExperimentalCoroutinesApi::class)
    val totalMoviesCount: StateFlow<Int> = selectedVodSource
        .flatMapLatest { src -> graph.catalogRepository.observeMovieCount(src) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val totalSeriesCount: StateFlow<Int> = selectedVodSource
        .flatMapLatest { src -> graph.catalogRepository.observeSeriesCount(src) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val totalChannelsCount: StateFlow<Int> = selectedVodSource
        .flatMapLatest { src -> graph.catalogRepository.observeChannelCount(src) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val accountExpiryMillis: StateFlow<Long> = settings.accountExpiryMillis

    init {
        viewModelScope.launch(Dispatchers.IO) {
            if (settings.accountExpiryMillis.value == 0L) {
                val sources = graph.sourceRepository.enabled()
                val xtream = sources.firstOrNull { it.kind == SourceKind.XTREAM }
                if (xtream != null) {
                    runCatching {
                        val info = graph.xtreamApi.authenticate(xtream)
                        info.expiryMillis?.let { settings.setAccountExpiryMillis(it) }
                    }
                }
            }
        }
    }

    /** True when at least one Stremio add-on is configured — gates the "add-on sources" button. */
    val hasAddons: StateFlow<Boolean> =
        settings.stremioAddons
            .map { it.isNotEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), settings.stremioAddons.value.isNotEmpty())

    /**
     * The streams the configured add-ons offer for [movie], resolved through the user's TMDB key for
     * the IMDb id. Empty when there's no key, no add-ons, no IMDb match, or nothing came back. Each
     * add-on is queried independently so one failing doesn't sink the rest.
     */
    suspend fun addonStreams(movie: Movie): List<StremioStream> {
        val addons = settings.stremioAddons.value
        if (addons.isEmpty()) return emptyList()
        val imdb = graph.catalogRepository.imdbIdFor(movie) ?: return emptyList()
        return withContext(Dispatchers.IO) {
            addons.flatMap { addon ->
                runCatching { graph.stremioClient.streams(addon.manifestUrl, addon.name, "movie", imdb) }
                    .getOrDefault(emptyList())
            }
        }
    }

    /** Switch the Movies/Shows provider filter; category selections reset since they differ per source. */
    fun selectVodSource(sourceId: Long?) {
        selectedVodSource.value = sourceId
        if (sourceId != null) {
            settings.setActiveSourceId(sourceId)
        }
        movieCategory.value = null
        seriesCategory.value = null
    }

    // ---- Netflix-style home rows ----------------------------------------------------------------
    // "Recently added" is reactive: it fills in live as a VOD sync lands. The computed feeds
    // (recommended, by-genre) are held in the StateFlows below and (re)built by [loadHomeFeeds] from
    // one shared scan of the library — guarded so opening the tab twice, or recomposing, does not
    // re-scan a 20k-title catalogue; it recomputes only on a profile change or catalogue growth.

    @OptIn(ExperimentalCoroutinesApi::class)
    val recentlyAddedMovies: StateFlow<List<Movie>> = selectedVodSource
        .flatMapLatest { src -> graph.catalogRepository.recentlyAddedMovies(limit = 30, sourceId = src) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val recentlyAddedSeries: StateFlow<List<Series>> = selectedVodSource
        .flatMapLatest { src -> graph.catalogRepository.recentlyAddedSeries(limit = 30, sourceId = src) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val favouriteMovies: StateFlow<List<Movie>> = selectedVodSource
        .flatMapLatest { src -> graph.catalogRepository.observeFavouriteMovies(sourceId = src) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val favouriteSeries: StateFlow<List<Series>> = selectedVodSource
        .flatMapLatest { src -> graph.catalogRepository.observeFavouriteSeries(sourceId = src) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _recommendedMovies = MutableStateFlow<List<Movie>>(emptyList())
    val recommendedMovies: StateFlow<List<Movie>> = _recommendedMovies.asStateFlow()

    private val _movieGenreRows = MutableStateFlow<List<GenreGroup<Movie>>>(emptyList())
    val movieGenreRows: StateFlow<List<GenreGroup<Movie>>> = _movieGenreRows.asStateFlow()

    private val _seriesGenreRows = MutableStateFlow<List<GenreGroup<Series>>>(emptyList())
    val seriesGenreRows: StateFlow<List<GenreGroup<Series>>> = _seriesGenreRows.asStateFlow()

    /** The (profile, catalogue) the computed rows were last built for — the redundant-reload guard. */
    private data class HomeFeedsKey(val profileId: Long, val sourceId: Long?, val movieCount: Int, val seriesCount: Int)

    @Volatile private var loadedHomeFeeds: HomeFeedsKey? = null

    /** Serialises [loadHomeFeeds] so two near-simultaneous calls (screen open + the profile emit in
     *  `init`) can't both start the heavy scan — the second waits, sees the guard satisfied, returns.
     *  MUST be declared before the `init` block below: `activeProfileId` is a StateFlow, so its
     *  `collect` fires synchronously during construction, and Kotlin initialises properties top to
     *  bottom — declared after `init`, this mutex would still be null when the first collect runs. */
    private val homeFeedsMutex = Mutex()

    init {
        // The home feeds are per profile (Recommended) and per catalogue (the genre rows). Rebuild
        // them when the active profile changes — and once at start. Routed through the guarded
        // [loadHomeFeeds] so a profile switch triggers exactly one library scan, and re-opening the
        // tab with the same profile and an unchanged catalogue triggers none. Reads an empty result
        // until a VOD sync has populated the catalogue; [ensureVodLoaded] re-runs it once one has.
        viewModelScope.launch {
            settings.activeProfileId.collect { loadHomeFeeds() }
        }
        viewModelScope.launch {
            settings.activeSourceId.collect { id ->
                selectedVodSource.value = if (id > 0) id else null
                movieCategory.value = null
                seriesCategory.value = null
                loadHomeFeeds()
            }
        }
    }

    /**
     * Recomputes the computed home rows (Recommended + by-genre) from a SINGLE scan of the library.
     *
     * The reactive rows (recently-added, continue-watching) keep themselves current, so this only
     * covers the computed feeds. On a 20k-title library the old version was the lag: it read the
     * whole movie table three times over (recommended + movie genres, plus a profile-change reload)
     * and again on every tab re-open. Now [allMovies]/[allSeries] are read once and reused for the
     * recommended row and every genre row, off the main thread, and a `(profile, movieCount,
     * seriesCount)` guard skips the work entirely unless the profile changed or the catalogue grew —
     * so re-opening Movies/Shows is free and a post-sync refresh still fills the rows in.
     */
    fun loadHomeFeeds() {
        viewModelScope.launch {
            homeFeedsMutex.withLock {
                val profileId = settings.activeProfileId.value
                val sourceId = selectedVodSource.value
                val movieCount = runCatching { graph.catalogRepository.movieCount(sourceId) }.getOrDefault(0)
                val seriesCount = runCatching { graph.catalogRepository.seriesCount(sourceId) }.getOrDefault(0)

                // Already built for this profile and this exact catalogue? Then a tab re-open or a
                // recomposition must not trigger another full-library scan. Only a profile change or
                // a catalogue whose size moved gets past here.
                val prev = loadedHomeFeeds
                if (prev != null && prev.profileId == profileId && prev.sourceId == sourceId &&
                    prev.movieCount == movieCount && prev.seriesCount == seriesCount
                ) return@withLock

                // One scan of each library (repo switches to IO), reused across every computed row.
                val allMovies = runCatching { graph.catalogRepository.allMovies(sourceId) }.getOrDefault(emptyList())
                val allSeries = runCatching { graph.catalogRepository.allSeries(sourceId) }.getOrDefault(emptyList())

                _recommendedMovies.value = runCatching {
                    graph.catalogRepository.recommendedMoviesFrom(allMovies, profileId)
                }.getOrDefault(emptyList())
                _movieGenreRows.value = runCatching {
                    graph.catalogRepository.moviesByGenreFrom(allMovies)
                }.getOrDefault(emptyList())
                _seriesGenreRows.value = runCatching {
                    graph.catalogRepository.seriesByGenreFrom(allSeries)
                }.getOrDefault(emptyList())

                loadedHomeFeeds = HomeFeedsKey(profileId, sourceId, movieCount, seriesCount)
            }
        }
    }

    // Movies + series load on demand — the first time the user opens Movies or Shows — rather than
    // up front at login. A provider's 40,000-title VOD list is exactly what makes a first sync
    // crawl, and most sessions only ever watch live TV. Loaded once per app run.
    private val _vodLoading = MutableStateFlow(false)
    val vodLoading: StateFlow<Boolean> = _vodLoading.asStateFlow()

    private var vodSyncJob: Job? = null

    /** How long a fetched VOD catalogue is trusted before a warm launch re-syncs it. */
    private val VOD_TTL_MILLIS = 12L * 60 * 60 * 1000  // 12 hours

    fun ensureVodLoaded() {
        if (vodSyncJob?.isActive == true) return
        vodSyncJob = viewModelScope.launch { syncVodIfStale(force = false) }
    }

    /**
     * Force a fresh download of the movies/series catalogue, ignoring the freshness cache. Wired
     * to the pull-to-refresh / refresh action so the user always has a way to pull new titles in
     * before the TTL lapses.
     */
    fun refreshVod() {
        vodSyncJob?.cancel()
        vodSyncJob = viewModelScope.launch { syncVodIfStale(force = true) }
    }

    private suspend fun syncVodIfStale(force: Boolean) {
        val now = System.currentTimeMillis()
        val enabledSources = runCatching { graph.sourceRepository.enabled() }.getOrDefault(emptyList())
        if (enabledSources.isEmpty()) return

        // Warm-launch fast path / intelligent sync:
        // Identify which sources are either never synced, empty, or stale (> 12 hours).
        val sourcesToSync = if (force) {
            enabledSources
        } else {
            enabledSources.filter { source ->
                val count = runCatching {
                    graph.catalogRepository.movieCount(source.id) + graph.catalogRepository.seriesCount(source.id)
                }.getOrDefault(0)
                val last = settings.getVodSyncedAtMillis(source.id)
                val stale = last == 0L || (now - last) >= VOD_TTL_MILLIS
                count == 0 || stale
            }
        }

        if (sourcesToSync.isEmpty()) {
            loadHomeFeeds()
            return
        }

        _vodLoading.value = true
        try {
            for (source in sourcesToSync) {
                runCatching {
                    graph.catalogRepository.syncVod(source, now, force = force)
                }.onFailure { Log.w("VodViewModel", "VOD sync failed for ${source.id}", it) }
            }
        } finally {
            _vodLoading.value = false
            StatusBus.set(null)
        }
        settings.vodSyncedAtMillis = now
        loadHomeFeeds()
    }

    // ---- VOD search (shared by the unified search screen) --------------------------------------
    private val vodSearchInput = MutableStateFlow("")

    fun setVodSearchQuery(text: String) { vodSearchInput.value = text.replace("+", " ") }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val movieResults: StateFlow<List<Movie>> =
        vodSearchInput.map { it.trim() }.debounce(200).distinctUntilChanged()
            .flatMapLatest { q ->
                if (q.length < 2) flowOf(emptyList()) else graph.catalogRepository.searchMovies(q)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val seriesResults: StateFlow<List<Series>> =
        vodSearchInput.map { it.trim() }.debounce(200).distinctUntilChanged()
            .flatMapLatest { q ->
                if (q.length < 2) flowOf(emptyList()) else graph.catalogRepository.searchSeries(q)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleMovieFavourite(movie: Movie) {
        viewModelScope.launch {
            graph.catalogRepository.setMovieFavourite(movie.id, !movie.favourite)
        }
    }

    /**
     * Episodes are fetched on demand rather than during catalogue sync.
     *
     * Pulling every episode of every series up front is what makes a first sync take twenty
     * minutes on a large provider, and most of it is never looked at.
     */
    fun loadEpisodes(series: Series) {
        viewModelScope.launch {
            val source = graph.sourceRepository.byId(series.sourceId) ?: return@launch
            graph.catalogRepository.ensureEpisodes(source, series.seriesId)
        }
    }

    fun episodes(series: Series) =
        graph.catalogRepository.observeEpisodes(series.sourceId, series.seriesId)

    /** For the series detail screen: look the series up by row id. */
    fun seriesById(id: Long): kotlinx.coroutines.flow.Flow<Series?> =
        kotlinx.coroutines.flow.flow { emit(graph.catalogRepository.series(id)) }

    /** An always-empty episode flow, so the detail screen has something before a series loads. */
    val noEpisodes: kotlinx.coroutines.flow.Flow<List<app.opentv.data.model.Episode>> =
        kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun movieById(id: Long): Movie? = graph.catalogRepository.movie(id)

    // ---- Detail screens -------------------------------------------------------------------------
    // Thin wrappers over the repository's detail feeds. The screens drive them from `produceState`
    // keyed on the id, so each detail instance owns its own state and a "More like this" hop to
    // another title loads cleanly without two screens fighting over one shared StateFlow.

    /** Loads a movie, lazily enriching backdrop/cast/genre on first open. See CatalogRepository.movieDetail. */
    suspend fun movieDetail(id: Long): Movie? = graph.catalogRepository.movieDetail(id)

    /** Loads a series, lazily enriching backdrop/cast/genre on first open. See CatalogRepository.seriesDetail. */
    suspend fun seriesDetail(id: Long): Series? = graph.catalogRepository.seriesDetail(id)

    /** "More like this" for the movie detail screen. */
    suspend fun moreLikeThis(movie: Movie): List<Movie> = graph.catalogRepository.moreLikeThis(movie)

    /** "More like this" for the series detail screen. */
    suspend fun moreLikeThisSeries(series: Series): List<Series> =
        graph.catalogRepository.moreLikeThisSeries(series)

    /** Every library title (movies + series) featuring a person — the Person screen's grid. */
    suspend fun titlesWithPerson(name: String): List<PersonTitle> =
        graph.catalogRepository.titlesWithPerson(name)

    /** The active profile's saved resume position for a movie/episode, or null — drives Resume vs Watch now. */
    suspend fun resumePosition(mediaKey: String): PlaybackPosition? =
        graph.playbackPositions.get(settings.activeProfileId.value, mediaKey)

    /** Collapses quality variants of a movie list for a browse grid. See CatalogRepository.collapseVariants. */
    fun collapseVariants(movies: List<Movie>): List<MovieVariantGroup> =
        graph.catalogRepository.collapseVariants(movies)

    /** Star / un-star a whole series. Wraps the series DAO through the shared database — no data-layer change. */
    fun toggleSeriesFavourite(series: Series) {
        viewModelScope.launch {
            graph.database.series().setFavourite(series.id, !series.favourite)
        }
    }

    /** The user-agent to play a movie/episode with (per source). */
    suspend fun userAgentForSource(sourceId: Long): String =
        graph.sourceRepository.byId(sourceId)?.userAgent ?: "OpenTV/0.1 (Android)"
}

/**
 * Local viewing profiles. No accounts — just names on this device — with the active one stored in
 * [app.opentv.core.AppSettings] so the whole app agrees on whose watch history is in play.
 */
class ProfilesViewModel(app: Application) : AndroidViewModel(app) {
    private val graph = ServiceLocator.get(app)
    private val settings = graph.settings

    val profiles: StateFlow<List<Profile>> =
        graph.profiles.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeProfileId: StateFlow<Long> = settings.activeProfileId

    init {
        // Fresh installs create the table empty (no migration runs), so seed the default profile.
        viewModelScope.launch {
            if (graph.profiles.all().isEmpty()) {
                graph.profiles.insert(
                    Profile(
                        name = "Varsayılan",
                        createdAtMillis = System.currentTimeMillis(),
                        avatarId = "avatar_1",
                    ),
                )
            }
        }
    }

    fun addProfile(
        name: String,
        avatarId: String = "avatar_1",
        pin: String? = null,
        isKids: Boolean = false,
        liveEnabled: Boolean = true,
        moviesEnabled: Boolean = true,
        seriesEnabled: Boolean = true,
        multiViewEnabled: Boolean = true,
    ) {
        viewModelScope.launch {
            val id = graph.profiles.insert(
                Profile(
                    name = name.trim().ifBlank { "Yeni Profil" },
                    createdAtMillis = System.currentTimeMillis(),
                    avatarId = avatarId,
                    pin = pin?.takeIf { it.isNotBlank() },
                    isKids = isKids,
                    liveEnabled = liveEnabled,
                    moviesEnabled = moviesEnabled,
                    seriesEnabled = seriesEnabled,
                    multiViewEnabled = multiViewEnabled,
                ),
            )
            settings.setActiveProfile(id)
        }
    }

    fun saveProfile(profile: Profile) {
        viewModelScope.launch {
            if (profile.id <= 0) {
                val id = graph.profiles.insert(profile)
                settings.setActiveProfile(id)
            } else {
                graph.profiles.update(profile)
            }
        }
    }

    fun rename(id: Long, name: String) {
        viewModelScope.launch { graph.profiles.rename(id, name.trim().ifBlank { "Profil" }) }
    }

    fun select(id: Long) { settings.setActiveProfile(id) }

    fun remove(id: Long) {
        // The base profile stays — there is always at least one to be.
        if (id == 1L) return
        viewModelScope.launch {
            graph.profiles.deletePositions(id)
            graph.profiles.delete(id)
            if (settings.activeProfileId.value == id) settings.setActiveProfile(1L)
        }
    }
}

/**
 * Local, device-to-device watch-history sync — no servers of ours.
 *
 * One device shares (a short-lived LAN server behind a six-digit code); the other receives by
 * typing that device's address and code. Only resume positions travel, keyed by stream URL and
 * profile name so they land on the right film and the right person on the other device. Merge is
 * newest-wins per item, so syncing both ways leaves the two devices agreeing.
 */
class SyncViewModel(app: Application) : AndroidViewModel(app) {
    private val graph = ServiceLocator.get(app)
    private val server = SyncServer(viewModelScope)
    private val client = SyncClient(graph.httpClient)

    val serverState: StateFlow<SyncServer.State> = server.state

    sealed interface ReceiveState {
        data object Idle : ReceiveState
        data object Connecting : ReceiveState
        data class Done(val merged: Int) : ReceiveState
        data class Failed(val message: String) : ReceiveState
    }

    private val _receiveState = MutableStateFlow<ReceiveState>(ReceiveState.Idle)
    val receiveState: StateFlow<ReceiveState> = _receiveState.asStateFlow()

    // ---- NAS ("cloud") sync ------------------------------------------------------------------

    private val nas = NasSync(graph)

    /** Whether an SMB/NAS is set up at all — the NAS pane needs one before it can do anything. */
    val smbHost: StateFlow<String> = graph.settings.smbHost
    val nasAutoSync: StateFlow<Boolean> = graph.settings.nasAutoSync

    sealed interface NasState {
        data object Idle : NasState
        data object Syncing : NasState
        data class Done(val message: String) : NasState
        data class Failed(val message: String) : NasState
    }

    private val _nasState = MutableStateFlow<NasState>(NasState.Idle)
    val nasState: StateFlow<NasState> = _nasState.asStateFlow()

    fun setNasAutoSync(enabled: Boolean) = graph.settings.setNasAutoSync(enabled)

    fun syncNas() {
        if (_nasState.value is NasState.Syncing) return
        viewModelScope.launch {
            _nasState.value = NasState.Syncing
            val ctx = getApplication<Application>()
            _nasState.value = when (val result = nas.sync()) {
                is NasSync.Result.Success ->
                    if (result.peers == 0) NasState.Done(ctx.getString(R.string.sync_nas_uploaded))
                    else NasState.Done(ctx.getString(R.string.sync_nas_done, result.merged, result.peers))
                is NasSync.Result.NotConfigured ->
                    NasState.Failed(ctx.getString(R.string.sync_nas_needs_setup))
                is NasSync.Result.Failed ->
                    NasState.Failed(ctx.getString(R.string.sync_nas_failed))
            }
        }
    }

    fun startSharing() {
        viewModelScope.launch { server.start(buildBundleJson()) }
    }

    fun stopSharing() = server.stop()

    fun receive(address: String, code: String) {
        viewModelScope.launch {
            _receiveState.value = ReceiveState.Connecting
            client.pull(address, code)
                .onSuccess { _receiveState.value = ReceiveState.Done(merge(it)) }
                .onFailure { _receiveState.value = ReceiveState.Failed(it.message ?: "Sync failed.") }
        }
    }

    fun resetReceive() { _receiveState.value = ReceiveState.Idle }

    private suspend fun buildBundleJson(): String {
        val names = graph.profiles.all().associate { it.id to it.name }
        val items = graph.playbackPositions.all().mapNotNull { pos ->
            val profileName = names[pos.profileId] ?: return@mapNotNull null
            val parts = pos.mediaKey.split(":", limit = 2)
            if (parts.size != 2) return@mapNotNull null
            val id = parts[1].toLongOrNull() ?: return@mapNotNull null
            val streamUrl = when (parts[0]) {
                "movie" -> graph.catalogRepository.movie(id)?.streamUrl
                "ep" -> graph.catalogRepository.episode(id)?.streamUrl
                else -> null
            } ?: return@mapNotNull null
            SyncPosition(profileName, streamUrl, parts[0], pos.positionMillis, pos.durationMillis, pos.updatedAtMillis)
        }
        return Json.encodeToString(SyncBundle.serializer(), SyncBundle(positions = items))
    }

    private suspend fun merge(bundle: SyncBundle): Int {
        var merged = 0
        for (item in bundle.positions) {
            val profileId = profileIdFor(item.profile) ?: continue
            val localId = when (item.kind) {
                "movie" -> graph.catalogRepository.movieByStreamUrl(item.streamUrl)?.id
                "ep" -> graph.catalogRepository.episodeByStreamUrl(item.streamUrl)?.id
                else -> null
            } ?: continue
            val mediaKey = "${item.kind}:$localId"
            val existing = graph.playbackPositions.get(profileId, mediaKey)
            if (existing == null || item.updatedAtMillis > existing.updatedAtMillis) {
                graph.playbackPositions.upsert(
                    PlaybackPosition(
                        profileId = profileId,
                        mediaKey = mediaKey,
                        positionMillis = item.positionMillis,
                        durationMillis = item.durationMillis,
                        updatedAtMillis = item.updatedAtMillis,
                    ),
                )
                merged++
            }
        }
        return merged
    }

    private suspend fun profileIdFor(name: String): Long? {
        graph.profiles.byName(name)?.let { return it.id }
        return runCatching { graph.profiles.insert(Profile(name = name, createdAtMillis = 0)) }.getOrNull()
    }

    override fun onCleared() {
        server.stop()
    }
}

/**
 * Owns the "manage channels from your phone or laptop" web server for the manage screen.
 *
 * Same ownership shape as [SyncViewModel]: the server is bound to [viewModelScope] (so its idle
 * watchdog is torn down with the screen) and stopped in [onCleared]. The screen also starts it on
 * open and stops it on leave, mirroring the phone-pairing lifecycle — so no socket is ever left
 * listening on the user's network once they navigate away.
 */
class WebManagerViewModel(app: Application) : AndroidViewModel(app) {
    private val graph = ServiceLocator.get(app)
    private val server = graph.managerServer

    val state: StateFlow<ManagerServer.State> = server.state

    fun start() = server.start()

    fun stop() = server.stop()
}
