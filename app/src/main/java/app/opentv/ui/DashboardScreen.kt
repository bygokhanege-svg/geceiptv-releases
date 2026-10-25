package app.opentv.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.ui.text.style.TextAlign
import app.opentv.ui.favorites.FavoritesDialog
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import app.opentv.R
import app.opentv.core.AppSettings
import app.opentv.data.model.Movie
import app.opentv.data.model.Series
import app.opentv.data.model.Source
import app.opentv.data.parser.displayTitle
import app.opentv.ui.SourcesViewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    onOpenMovie: (Movie) -> Unit,
    onOpenSeries: (Series) -> Unit,
    onResume: (mediaKey: String, url: String, title: String) -> Unit,
    onOpenLicense: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenMultiView: () -> Unit = {},
    onAddSource: () -> Unit = {},
    activeProfileName: String = "Me",
    onOpenProfiles: () -> Unit = {},
    isTelevision: Boolean = true,
    onNavigateTab: ((Tab) -> Unit)? = null,
    onOpenWebManager: () -> Unit = {},
    vodViewModel: VodViewModel = viewModel(),
    sourcesViewModel: SourcesViewModel = viewModel(),
) {
    val context = LocalContext.current
    val settings = remember { AppSettings.get(context) }
    val activeSourceId by settings.activeSourceId.collectAsState()
    val sourcesUi by sourcesViewModel.ui.collectAsState()
    val activeHealth by sourcesViewModel.activeProviderHealth.collectAsState()

    val themeMode by settings.themeMode.collectAsState()
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppSettings.ThemeMode.DARK -> true
        AppSettings.ThemeMode.LIGHT -> false
        AppSettings.ThemeMode.SYSTEM -> isTelevision || systemInDark
    }

    val recentMovies by vodViewModel.recentlyAddedMovies.collectAsState()
    val recentSeries by vodViewModel.recentlyAddedSeries.collectAsState()
    val recommendedMovies by vodViewModel.recommendedMovies.collectAsState()
    val continueWatching by vodViewModel.continueWatching.collectAsState()
    val favouriteMovies by vodViewModel.favouriteMovies.collectAsState()
    val favouriteSeries by vodViewModel.favouriteSeries.collectAsState()
    var showFavoritesDialog by remember { mutableStateOf(false) }
    var showClearAllConfirm by remember { mutableStateOf(false) }
    var itemToRemove by remember { mutableStateOf<VodViewModel.ResumeItem?>(null) }
    var showAllContinueWatching by remember { mutableStateOf(false) }

    val hasSources = sourcesUi.sources.isNotEmpty()
    LaunchedEffect(hasSources) {
        if (hasSources) {
            vodViewModel.ensureVodLoaded()
            vodViewModel.loadHomeFeeds()
        }
    }

    var currentTime by remember { mutableStateOf(Calendar.getInstance().time) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = Calendar.getInstance().time
        }
    }
    
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("dd MMMM yyyy EEEE", Locale.getDefault()) }

    val totalMoviesCount by vodViewModel.totalMoviesCount.collectAsState()
    val totalSeriesCount by vodViewModel.totalSeriesCount.collectAsState()
    val totalChannelsCount by vodViewModel.totalChannelsCount.collectAsState()
    val accountExpiryMillis by vodViewModel.accountExpiryMillis.collectAsState()
    val expiryFormatter = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    val lifetimeText = stringResource(R.string.dashboard_expiry_lifetime)
    val expiryDisplayText = remember(accountExpiryMillis, totalChannelsCount, lifetimeText) {
        when {
            accountExpiryMillis > 0L -> expiryFormatter.format(Date(accountExpiryMillis))
            totalChannelsCount > 0 -> lifetimeText
            else -> "-"
        }
    }
    val numberFormatter = remember { java.text.NumberFormat.getInstance(Locale.getDefault()) }
    val formattedSeries = remember(totalSeriesCount) { numberFormatter.format(totalSeriesCount) }
    val formattedMovies = remember(totalMoviesCount) { numberFormatter.format(totalMoviesCount) }
    val formattedChannels = remember(totalChannelsCount) { numberFormatter.format(totalChannelsCount) }
    val formattedShowcase = remember(recentMovies, recentSeries) {
        val total = recentMovies.size + recentSeries.size
        numberFormatter.format(if (total > 0) total else recentMovies.size)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!isTelevision) {
            PhoneDashboardContent(
            recentMovies = recentMovies,
            recentSeries = recentSeries,
            recommendedMovies = recommendedMovies,
            continueWatching = continueWatching,
            formattedSeries = formattedSeries,
            formattedMovies = formattedMovies,
            formattedChannels = formattedChannels,
            formattedShowcase = formattedShowcase,
            expiryDisplayText = expiryDisplayText,
            currentTime = currentTime,
            dateFormatter = dateFormatter,
            timeFormatter = timeFormatter,
            onOpenMovie = onOpenMovie,
            onOpenSeries = onOpenSeries,
            onResume = onResume,
            onOpenLicense = onOpenLicense,
            onOpenSearch = onOpenSearch,
            onOpenFavorites = { showFavoritesDialog = true },
            onShowAllContinueWatching = { showAllContinueWatching = true },
            onRemoveResumeItem = { itemToRemove = it },
            onClearContinueWatching = { showClearAllConfirm = true },
            onOpenSettings = onOpenSettings,
            onOpenMultiView = onOpenMultiView,
            onAddSource = onAddSource,
            activeProfileName = activeProfileName,
            onOpenProfiles = onOpenProfiles,
            onOpenWebManager = onOpenWebManager,
            sources = sourcesUi.sources,
            activeSourceId = activeSourceId,
            onSelectSource = { settings.setActiveSourceId(it.id) },
            onNavigateTab = onNavigateTab,
            health = activeHealth,
        )
    } else {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.background,
                    ),
                    radius = 1600f,
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
        ) {
            // Top Header & Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Logo & Title Area
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF090D16))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_opentv_logo),
                            contentDescription = "Logo",
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "GECE İPTV Player",
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(R.string.dashboard_brand_subtitle),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))

                // Modern Stat Cards & Status & Clock Group
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    StatCard(stringResource(R.string.dashboard_stat_series), formattedSeries, stringResource(R.string.dashboard_unit_items))
                    StatCard(stringResource(R.string.dashboard_stat_movies), formattedMovies, stringResource(R.string.dashboard_unit_items))
                    StatCard(stringResource(R.string.dashboard_stat_channels), formattedChannels, stringResource(R.string.dashboard_unit_channels))
                    StatCard(stringResource(R.string.dashboard_stat_showcase), formattedShowcase, stringResource(R.string.dashboard_unit_showcase))
                    
                    // Active Status Card with Expiry Date & Quick License Access
                    val statusInteractionSource = remember { MutableInteractionSource() }
                    val isStatusFocused by statusInteractionSource.collectIsFocusedAsState()

                    Box(
                        modifier = Modifier
                            .height(46.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isStatusFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surface,
                            )
                            .border(
                                1.dp,
                                if (isStatusFocused) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                RoundedCornerShape(8.dp),
                            )
                            .clickable(
                                interactionSource = statusInteractionSource,
                                indication = null,
                                onClick = onOpenLicense,
                            )
                            .focusable(interactionSource = statusInteractionSource)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    stringResource(R.string.dashboard_status_label),
                                    color = if (isStatusFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Box(
                                    modifier = Modifier
                                        .size(6.5.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF22C55E)),
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = expiryDisplayText,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.labelMedium,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    // Clock Card - Directly next to Durum card
                    Box(
                        modifier = Modifier
                            .height(46.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Column {
                            Text(
                                dateFormatter.format(currentTime),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 7.5.sp,
                                maxLines = 1,
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                timeFormatter.format(currentTime),
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.labelLarge,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    // Theme Toggle Button (Gece / Gündüz) - Next to Clock Card
                    val themeInteractionSource = remember { MutableInteractionSource() }
                    val isThemeFocused by themeInteractionSource.collectIsFocusedAsState()

                    Box(
                        modifier = Modifier
                            .height(46.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isThemeFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surface,
                            )
                            .border(
                                1.dp,
                                if (isThemeFocused) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                RoundedCornerShape(8.dp),
                            )
                            .clickable(
                                interactionSource = themeInteractionSource,
                                indication = null,
                                onClick = {
                                    settings.setThemeMode(
                                        if (isDark) AppSettings.ThemeMode.LIGHT else AppSettings.ThemeMode.DARK
                                    )
                                },
                            )
                            .focusable(interactionSource = themeInteractionSource)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(
                                imageVector = if (isDark) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                                contentDescription = "Tema Değiştir",
                                tint = if (isDark) Color(0xFFFACC15) else Color(0xFFF59E0B),
                                modifier = Modifier.size(17.dp),
                            )
                            Text(
                                text = if (isDark) stringResource(R.string.dashboard_theme_dark) else stringResource(R.string.dashboard_theme_light),
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sağlayıcı Seçimi Hızlı Çip Menüsü (Tek tıkla anında geçiş)
            ProviderQuickChipsBar(
                sources = sourcesUi.sources,
                activeSourceId = activeSourceId,
                onSelectSource = { settings.setActiveSourceId(it.id) },
                onAddSource = onAddSource,
                health = activeHealth,
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Şık ve Modern Ana Dizin Gridleri (CANLI TV, FİLMLER, DİZİLER)
            DashboardMediaHubGrid(
                formattedChannels = formattedChannels,
                formattedMovies = formattedMovies,
                formattedSeries = formattedSeries,
                onNavigateLive = { onNavigateTab?.invoke(Tab.LIVE) },
                onNavigateMovies = { onNavigateTab?.invoke(Tab.MOVIES) },
                onNavigateSeries = { onNavigateTab?.invoke(Tab.SHOWS) },
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Hızlı Araçlar ve İşlem Paneli (Çoklu TV, Arama, Favoriler, QR ile Bağlan, Ayarlar)
            DashboardQuickToolsRow(
                onOpenMultiView = onOpenMultiView,
                onOpenSearch = onOpenSearch,
                onOpenFavorites = { showFavoritesDialog = true },
                onOpenQr = onOpenWebManager,
                onOpenSettings = onOpenSettings,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Main Content Area (3 Columns: Yeni Diziler, İzlemeye Devam Et, Yeni Filmler)
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Left Column: Yeni Diziler
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    ListColumn(
                        title = stringResource(R.string.dashboard_section_new_series),
                        badge = stringResource(R.string.dashboard_section_latest_badge, recentSeries.take(10).size),
                        items = recentSeries.take(10),
                        isMovie = false,
                        onItemClick = { onOpenSeries(it as Series) },
                    )
                }

                // Center Column: İzlemeye Devam Et (Continue Watching)
                Box(modifier = Modifier.weight(1.5f).fillMaxHeight()) {
                    ContinueWatchingColumn(
                        continueWatching = continueWatching,
                        recommendedFallback = recommendedMovies.firstOrNull(),
                        onResume = onResume,
                        onOpenMovie = onOpenMovie,
                        onRemoveItem = { itemToRemove = it },
                        onClearAll = { showClearAllConfirm = true },
                        onShowAll = { showAllContinueWatching = true },
                    )
                }

                // Right Column: Yeni Filmler
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    ListColumn(
                        title = stringResource(R.string.dashboard_section_new_movies),
                        badge = stringResource(R.string.dashboard_section_latest_badge, recentMovies.take(10).size),
                        items = recentMovies.take(10),
                        isMovie = true,
                        onItemClick = { onOpenMovie(it as Movie) },
                    )
                }
            }
        }

        // Sağ Alt Köşe: Sağlayıcı Bağlantı / Hata Bildirim Paneli
        AnimatedVisibility(
            visible = activeHealth?.isConnected == false,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E0A0A).copy(alpha = 0.96f))
                    .border(1.5.dp, Color(0xFFEF4444), RoundedCornerShape(12.dp))
                    .padding(14.dp),
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("⚠️", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.dashboard_alert_title),
                            color = Color(0xFFFCA5A5),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.dashboard_alert_desc),
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp,
                        )
                        if (!activeHealth?.errorMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.dashboard_alert_error, activeHealth?.errorMessage ?: ""),
                                color = Color(0xFFF87171),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                            )
                        }
                    }
                }
            }
        }
    }
    }

    if (showFavoritesDialog) {
        FavoritesDialog(
            favouriteMovies = favouriteMovies,
            favouriteSeries = favouriteSeries,
            onOpenMovie = { movie ->
                showFavoritesDialog = false
                onOpenMovie(movie)
            },
            onOpenSeries = { series ->
                showFavoritesDialog = false
                onOpenSeries(series)
            },
            onToggleMovieFavourite = { movie ->
                vodViewModel.toggleMovieFavourite(movie)
            },
            onToggleSeriesFavourite = { series ->
                vodViewModel.toggleSeriesFavourite(series)
            },
            onDismiss = { showFavoritesDialog = false },
            isTelevision = isTelevision,
        )
    }

    if (showClearAllConfirm) {
        ContinueWatchingClearConfirmDialog(
            onConfirm = {
                vodViewModel.clearContinueWatching()
                showClearAllConfirm = false
            },
            onDismiss = { showClearAllConfirm = false },
        )
    }

    itemToRemove?.let { item ->
        ContinueWatchingRemoveConfirmDialog(
            itemTitle = item.title,
            onConfirm = {
                vodViewModel.removeContinueWatching(item.mediaKey)
                itemToRemove = null
            },
            onDismiss = { itemToRemove = null },
        )
    }

        if (showAllContinueWatching) {
            AllContinueWatchingDialog(
                continueWatching = continueWatching,
                onResume = { mediaKey, url, title ->
                    showAllContinueWatching = false
                    onResume(mediaKey, url, title)
                },
                onRemoveItem = { item ->
                    itemToRemove = item
                },
                onClearAll = {
                    showClearAllConfirm = true
                },
                onDismiss = { showAllContinueWatching = false },
                isTelevision = isTelevision,
            )
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, unit: String) {
    Box(
        modifier = Modifier
            .width(88.dp)
            .height(46.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    title,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    value,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    unit,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 7.5.sp,
                    modifier = Modifier.padding(bottom = 1.dp),
                )
            }
        }
    }
}

@Composable
private fun ContinueWatchingColumn(
    continueWatching: List<VodViewModel.ResumeItem>,
    recommendedFallback: Movie?,
    onResume: (mediaKey: String, url: String, title: String) -> Unit,
    onOpenMovie: (Movie) -> Unit,
    onRemoveItem: (VodViewModel.ResumeItem) -> Unit = {},
    onClearAll: () -> Unit = {},
    onShowAll: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⏱️", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    stringResource(R.string.dashboard_section_continue_watching),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (continueWatching.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .clickable { onClearAll() }
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteSweep,
                                contentDescription = stringResource(R.string.continue_watching_clear_all),
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(13.dp),
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                stringResource(R.string.continue_watching_clear_all),
                                color = Color(0xFFEF4444),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                            )
                        }
                    }
                }
                var badgeFocused by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (badgeFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .border(
                            width = 1.dp,
                            color = if (badgeFocused) Color.White else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp),
                        )
                        .clickable(enabled = continueWatching.isNotEmpty(), onClick = onShowAll)
                        .onFocusChanged { badgeFocused = it.isFocused }
                        .focusable(enabled = continueWatching.isNotEmpty())
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                ) {
                    Text(
                        if (continueWatching.isNotEmpty()) stringResource(R.string.dashboard_content_count, continueWatching.size) + " ▶" else stringResource(R.string.dashboard_recommended).uppercase(),
                        color = if (badgeFocused) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        if (continueWatching.isNotEmpty()) {
            val primary = continueWatching.first()
            var primaryFocused by remember { mutableStateOf(false) }

            // Hero Continue Watching Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (continueWatching.size > 1) 1.4f else 1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (primaryFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        if (primaryFocused) 2.dp else 1.dp,
                        if (primaryFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        RoundedCornerShape(12.dp),
                    )
                    .clickable { onResume(primary.mediaKey, primary.streamUrl, primary.title) }
                    .onFocusChanged { primaryFocused = it.isFocused }
                    .focusable()
                    .padding(10.dp),
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Poster with embedded progress bar
                    Box(
                        modifier = Modifier
                            .weight(0.42f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp)),
                    ) {
                        AsyncImage(
                            model = primary.posterUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().background(Color.DarkGray),
                        )
                        // Dark bottom overlay for progress bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                                .align(Alignment.BottomCenter)
                                .background(Color.Black.copy(alpha = 0.75f)),
                        ) {
                            LinearProgressIndicator(
                                progress = { primary.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .align(Alignment.BottomCenter),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = Color.White.copy(alpha = 0.2f),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // Info Column
                    Column(
                        modifier = Modifier.weight(0.58f).fillMaxHeight(),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            val percent = (primary.progress * 100).toInt().coerceIn(1, 99)
                            Text(
                                stringResource(R.string.dashboard_resume_progress, percent),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = primary.title,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = primary.title,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .padding(horizontal = 12.dp, vertical = 5.dp),
                            ) {
                                Text(
                                    "▶ " + stringResource(R.string.dashboard_resume),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .clickable { onRemoveItem(primary) }
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = stringResource(R.string.continue_watching_remove_item),
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(13.dp),
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        stringResource(R.string.continue_watching_remove_item),
                                        color = Color(0xFFEF4444),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 9.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Secondary Continue Watching Items (if more than 1)
            if (continueWatching.size > 1) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(continueWatching.drop(1).take(2)) { item ->
                        var subFocused by remember { mutableStateOf(false) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (subFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    if (subFocused) 1.5.dp else 0.dp,
                                    if (subFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    RoundedCornerShape(8.dp),
                                )
                                .clickable { onResume(item.mediaKey, item.streamUrl, item.title) }
                                .onFocusChanged { subFocused = it.isFocused }
                                .focusable()
                                .padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(32.dp)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(4.dp)),
                            ) {
                                AsyncImage(
                                    model = item.posterUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().background(Color.DarkGray),
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                                val percent = (item.progress * 100).toInt().coerceIn(1, 99)
                                Text(
                                    text = stringResource(R.string.dashboard_watched_badge, percent),
                                    color = if (subFocused) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = item.title,
                                    color = if (subFocused) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "▶",
                                color = if (subFocused) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.1f))
                                    .clickable { onRemoveItem(item) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = stringResource(R.string.continue_watching_remove_item),
                                    tint = if (subFocused) Color.White else Color(0xFFEF4444),
                                    modifier = Modifier.size(13.dp),
                                )
                            }
                        }
                    }
                    if (continueWatching.size > 3) {
                        item {
                            var moreFocused by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (moreFocused) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f))
                                    .border(
                                        width = if (moreFocused) 1.5.dp else 1.dp,
                                        color = if (moreFocused) Color.White else Color.White.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp),
                                    )
                                    .clickable { onShowAll() }
                                    .onFocusChanged { moreFocused = it.isFocused }
                                    .focusable()
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(R.string.continue_watching_see_all_more, continueWatching.size - 3),
                                    color = if (moreFocused) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Fallback when no continue watching items exist yet
            if (recommendedFallback != null) {
                var recFocused by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (recFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            if (recFocused) 2.dp else 1.dp,
                            if (recFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp),
                        )
                        .clickable { onOpenMovie(recommendedFallback) }
                        .onFocusChanged { recFocused = it.isFocused }
                        .focusable()
                        .padding(10.dp),
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .weight(0.42f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp)),
                        ) {
                            AsyncImage(
                                model = recommendedFallback.posterUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().background(Color.DarkGray),
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(
                            modifier = Modifier.weight(0.58f).fillMaxHeight(),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                            ) {
                                Text(
                                    "✨ " + stringResource(R.string.dashboard_recommended).uppercase(),
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = recommendedFallback.displayTitle,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "İzlediğiniz dizi veya filmler yarım kaldığında burada otomatik listelenir.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 3,
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .padding(horizontal = 12.dp, vertical = 5.dp),
                            ) {
                                Text(
                                    "▶ İzlemeye Başla",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "İzlediğiniz dizi ve filmler burada listelenir.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun ListColumn(
    title: String,
    badge: String,
    items: List<Any>,
    isMovie: Boolean,
    onItemClick: (Any) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📺", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .padding(horizontal = 7.dp, vertical = 3.dp),
            ) {
                Text(
                    badge,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(items) { item ->
                ListItemCard(item = item, isMovie = isMovie, onClick = { onItemClick(item) })
            }
        }
    }
}

@Composable
private fun ListItemCard(item: Any, isMovie: Boolean, onClick: () -> Unit) {
    val title = if (isMovie) (item as Movie).displayTitle else (item as Series).name
    val poster = if (isMovie) (item as Movie).posterUrl else (item as Series).posterUrl
    val year = if (isMovie) (item as Movie).year?.toString() ?: "" else (item as Series).year?.toString() ?: ""
    
    var focused by remember { mutableStateOf(false) }
    val bg = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (focused) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val subColor = if (focused) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(
                if (focused) 1.5.dp else 0.dp,
                if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
                RoundedCornerShape(8.dp),
            )
            .clickable { onClick() }
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = poster,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(34.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(4.dp))
                .background(Color.DarkGray),
        )
        Spacer(modifier = Modifier.width(10.dp))
        val tagStr = if (isMovie) stringResource(R.string.dashboard_tag_movie) else stringResource(R.string.dashboard_tag_series)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(
                text = "TR | $year | $tagStr",
                color = subColor,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = title,
                color = contentColor,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Mobile smartphone optimized Dashboard layout.
 * Features a single-column, vertically scrollable feed with touch cards,
 * continue-watching hero, and horizontal carousels (LazyRow).
 */
@Composable
private fun PhoneDashboardContent(
    recentMovies: List<Movie>,
    recentSeries: List<Series>,
    recommendedMovies: List<Movie>,
    continueWatching: List<VodViewModel.ResumeItem>,
    formattedSeries: String,
    formattedMovies: String,
    formattedChannels: String,
    formattedShowcase: String,
    expiryDisplayText: String,
    currentTime: Date,
    dateFormatter: SimpleDateFormat,
    timeFormatter: SimpleDateFormat,
    onOpenMovie: (Movie) -> Unit,
    onOpenSeries: (Series) -> Unit,
    onResume: (mediaKey: String, url: String, title: String) -> Unit,
    onOpenLicense: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenFavorites: () -> Unit = {},
    onShowAllContinueWatching: () -> Unit = {},
    onRemoveResumeItem: (VodViewModel.ResumeItem) -> Unit = {},
    onClearContinueWatching: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenMultiView: () -> Unit,
    onAddSource: () -> Unit,
    activeProfileName: String,
    onOpenProfiles: () -> Unit,
    onOpenWebManager: () -> Unit = {},
    sources: List<Source>,
    activeSourceId: Long,
    onSelectSource: (Source) -> Unit,
    onNavigateTab: ((Tab) -> Unit)?,
    health: SourcesViewModel.ProviderHealth? = null,
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF0A0F1D),
                        MaterialTheme.colorScheme.background,
                    )
                )
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            // 1. Welcome & Status Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF1E293B),
                                Color(0xFF0F172A),
                            )
                        )
                    )
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .clickable(onClick = onOpenLicense)
                    .padding(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF090D16))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_opentv_logo),
                                contentDescription = "Logo",
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "GECE İPTV Player",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.5.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF22C55E)),
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = stringResource(R.string.dashboard_expiry_prefix, expiryDisplayText),
                                    color = Color(0xFF22C55E),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = timeFormatter.format(currentTime),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        )
                        Text(
                            text = dateFormatter.format(currentTime),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.5.sp,
                            maxLines = 1,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. ALT GRİD: Logo, Profil, Arama, Çoklu TV ve Ayarlar Barı (Kullanıcı isteğiyle üste abonelik/saat, alta bu bar alındı)
            PhoneActionBarCard(
                title = "GECE İPTV Player",
                activeProfileName = activeProfileName,
                onOpenSearch = onOpenSearch,
                onOpenMultiView = onOpenMultiView,
                onOpenSettings = onOpenSettings,
                onOpenProfiles = onOpenProfiles,
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Sağlayıcı Seçimi Hızlı Çip Menüsü (Tek dokunuşla anında geçiş)
            ProviderQuickChipsBar(
                sources = sources,
                activeSourceId = activeSourceId,
                onSelectSource = onSelectSource,
                onAddSource = onAddSource,
                health = health,
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. 2x2 Şık ve Modern Ana Dizin Gridleri (CANLI TV, FİLMLER, DİZİLER, ÇOKLU TV)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PhoneMenuCard(
                    title = stringResource(R.string.dashboard_nav_live_tv),
                    count = formattedChannels,
                    unit = stringResource(R.string.dashboard_unit_channels),
                    badge = stringResource(R.string.dashboard_nav_live_badge),
                    icon = Icons.Outlined.LiveTv,
                    gradientColors = listOf(Color(0xFF0369A1), Color(0xFF0284C7)),
                    accentColor = Color(0xFF38BDF8),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTab?.invoke(Tab.LIVE) },
                )
                PhoneMenuCard(
                    title = stringResource(R.string.dashboard_nav_movies),
                    count = formattedMovies,
                    unit = stringResource(R.string.dashboard_unit_items),
                    badge = stringResource(R.string.dashboard_nav_movies_badge),
                    icon = Icons.Outlined.Movie,
                    gradientColors = listOf(Color(0xFF4338CA), Color(0xFF6366F1)),
                    accentColor = Color(0xFF818CF8),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTab?.invoke(Tab.MOVIES) },
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PhoneMenuCard(
                    title = stringResource(R.string.dashboard_nav_series),
                    count = formattedSeries,
                    unit = stringResource(R.string.dashboard_unit_items),
                    badge = stringResource(R.string.dashboard_nav_series_badge),
                    icon = Icons.Outlined.VideoLibrary,
                    gradientColors = listOf(Color(0xFF7C3AED), Color(0xFF8B5CF6)),
                    accentColor = Color(0xFFA78BFA),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTab?.invoke(Tab.SHOWS) },
                )
                PhoneMenuCard(
                    title = stringResource(R.string.dashboard_tool_multiview),
                    count = "4x",
                    unit = "TV",
                    badge = "MULTI",
                    icon = Icons.Default.GridView,
                    gradientColors = listOf(Color(0xFF047857), Color(0xFF10B981)),
                    accentColor = Color(0xFF34D399),
                    modifier = Modifier.weight(1f),
                    onClick = onOpenMultiView,
                )
            }

            // 3. Continue Watching Hero
            if (continueWatching.isNotEmpty()) {
                val primary = continueWatching.first()
                val percent = (primary.progress * 100).toInt().coerceIn(1, 99)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⏱️", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.dashboard_section_continue_watching),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                .clickable { onClearContinueWatching() }
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteSweep,
                                    contentDescription = stringResource(R.string.continue_watching_clear_all),
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(11.dp),
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    stringResource(R.string.continue_watching_clear_all),
                                    color = Color(0xFFEF4444),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.5.sp,
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .clickable { onShowAllContinueWatching() }
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                stringResource(R.string.dashboard_content_count, continueWatching.size) + " ▶",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .clickable { onResume(primary.mediaKey, primary.streamUrl, primary.title) }
                        .padding(10.dp),
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(70.dp)
                                .height(98.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        ) {
                            AsyncImage(
                                model = primary.posterUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().background(Color.DarkGray),
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(Color.Black.copy(alpha = 0.7f)),
                            ) {
                                LinearProgressIndicator(
                                    progress = { primary.progress },
                                    modifier = Modifier.fillMaxSize(),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = Color.White.copy(alpha = 0.2f),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    stringResource(R.string.dashboard_resume_progress, percent),
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = primary.title,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                ) {
                                    Text(
                                        "▶ " + stringResource(R.string.dashboard_watch_now),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .clickable { onRemoveResumeItem(primary) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteOutline,
                                            contentDescription = stringResource(R.string.continue_watching_remove_item),
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(11.dp),
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            stringResource(R.string.continue_watching_remove_item),
                                            color = Color(0xFFEF4444),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 9.sp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (continueWatching.size > 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .clickable { onShowAllContinueWatching() }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.continue_watching_see_all_more, continueWatching.size - 1),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                        )
                    }
                }
            } else if (recommendedMovies.isNotEmpty()) {
                val rec = recommendedMovies.first()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("⭐", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.dashboard_section_daily_pick),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .clickable { onOpenMovie(rec) }
                        .padding(10.dp),
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(70.dp)
                                .height(98.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        ) {
                            AsyncImage(
                                model = rec.posterUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().background(Color.DarkGray),
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "TR | ${rec.year ?: ""} | ÖNERİLEN",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = rec.displayTitle,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    "▶ Detayları Gör",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                )
                            }
                        }
                    }
                }
            }

            // 4. Yeni Diziler Carousel (LazyRow)
            if (recentSeries.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📺", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.dashboard_section_new_series),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        text = stringResource(R.string.dashboard_section_see_all),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.clickable { onNavigateTab?.invoke(Tab.SHOWS) },
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(recentSeries.take(12)) { series ->
                        PhonePosterCard(
                            title = series.name,
                            posterUrl = series.posterUrl,
                            tag = "DİZİ",
                            year = series.year?.toString() ?: "",
                            onClick = { onOpenSeries(series) },
                        )
                    }
                }
            }

            // 5. Yeni Filmler Carousel (LazyRow)
            if (recentMovies.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎬", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.dashboard_section_new_movies),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        text = stringResource(R.string.dashboard_section_see_all),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.clickable { onNavigateTab?.invoke(Tab.MOVIES) },
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(recentMovies.take(12)) { movie ->
                        PhonePosterCard(
                            title = movie.displayTitle,
                            posterUrl = movie.posterUrl,
                            tag = "FİLM",
                            year = movie.year?.toString() ?: "",
                            onClick = { onOpenMovie(movie) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Alt Araçlar ve Hızlı İşlemler Paneli
            DashboardQuickToolsRow(
                onOpenMultiView = onOpenMultiView,
                onOpenSearch = onOpenSearch,
                onOpenFavorites = onOpenFavorites,
                onOpenQr = onOpenWebManager,
                onOpenSettings = onOpenSettings,
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Sağ Alt Köşe / Mobil Bildirim Paneli
        AnimatedVisibility(
            visible = health?.isConnected == false,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E0A0A).copy(alpha = 0.96f))
                    .border(1.5.dp, Color(0xFFEF4444), RoundedCornerShape(12.dp))
                    .padding(12.dp),
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("⚠️", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.dashboard_alert_title),
                            color = Color(0xFFFCA5A5),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.dashboard_alert_desc),
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhoneMenuCard(
    title: String,
    count: String,
    unit: String,
    badge: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(76.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(gradientColors))
            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title,
                    color = Color.White.copy(alpha = 0.92f),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                        .padding(horizontal = 5.dp, vertical = 1.5.dp),
                ) {
                    Text(
                        text = badge,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = count,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        color = Color.White.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PhonePosterCard(
    title: String,
    posterUrl: String?,
    tag: String,
    year: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(105.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .width(105.dp)
                .height(150.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF141926))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
        ) {
            AsyncImage(
                model = posterUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .padding(5.dp)
                    .align(Alignment.TopStart)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            ) {
                Text(
                    text = if (year.isNotBlank()) "$year • $tag" else tag,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Alt Grid İşlem Paneli:
 * Ayarlar, Arama ve Çoklu TV Özelleştirme hızlı işlem kartları.
 * Hem TV kumandası D-Pad ile odaklanabilir (focus border, glow) hem de dokunmatik çalışır.
 */
@Composable
fun DashboardQuickToolsRow(
    onOpenMultiView: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenQr: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        QuickToolCard(
            title = stringResource(R.string.dashboard_tool_multiview),
            subtitle = stringResource(R.string.dashboard_tool_multiview_desc),
            icon = Icons.Default.GridView,
            accentColor = Color(0xFF10B981),
            modifier = Modifier.weight(1.05f),
            onClick = onOpenMultiView,
        )
        QuickToolCard(
            title = stringResource(R.string.dashboard_tool_search),
            subtitle = stringResource(R.string.dashboard_tool_search_desc),
            icon = Icons.Outlined.Search,
            accentColor = Color(0xFF38BDF8),
            modifier = Modifier.weight(1f),
            onClick = onOpenSearch,
        )
        QuickToolCard(
            title = stringResource(R.string.dashboard_tool_favorites),
            subtitle = stringResource(R.string.dashboard_tool_favorites_desc),
            icon = Icons.Filled.Star,
            accentColor = Color(0xFFF59E0B),
            modifier = Modifier.weight(1f),
            onClick = onOpenFavorites,
        )
        QuickToolCard(
            title = stringResource(R.string.dashboard_tool_qr),
            subtitle = stringResource(R.string.dashboard_tool_qr_desc),
            icon = Icons.Filled.QrCode2,
            accentColor = Color(0xFF8B5CF6),
            modifier = Modifier.weight(1f),
            onClick = onOpenQr,
        )
        QuickToolCard(
            title = stringResource(R.string.dashboard_tool_settings),
            subtitle = stringResource(R.string.dashboard_tool_settings_desc),
            icon = Icons.Outlined.Settings,
            accentColor = Color(0xFFA855F7),
            modifier = Modifier.weight(1f),
            onClick = onOpenSettings,
        )
    }
}

@Composable
private fun ContinueWatchingClearConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.activity.compose.BackHandler(onBack = onDismiss)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(150f)
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0F172A))
                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(20.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteSweep,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(26.dp),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.continue_watching_clear_confirm_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.continue_watching_clear_confirm_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable { onDismiss() }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.common_cancel),
                            color = Color(0xFFCBD5E1),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEF4444))
                            .clickable { onConfirm() }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.continue_watching_clear_all),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContinueWatchingRemoveConfirmDialog(
    itemTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.activity.compose.BackHandler(onBack = onDismiss)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(150f)
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0F172A))
                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(20.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(26.dp),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.continue_watching_remove_confirm_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.continue_watching_remove_confirm_desc, itemTitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable { onDismiss() }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.common_cancel),
                            color = Color(0xFFCBD5E1),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEF4444))
                            .clickable { onConfirm() }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.continue_watching_remove_item),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickToolCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.03f else 1.0f,
        animationSpec = tween(durationMillis = 150),
        label = "toolScale",
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .fillMaxHeight()
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.horizontalGradient(
                    if (isFocused) listOf(accentColor.copy(alpha = 0.35f), Color(0xFF1E293B))
                    else listOf(Color(0xFF0F172A), Color(0xFF1E293B).copy(alpha = 0.8f))
                )
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) accentColor else Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(accentColor.copy(alpha = if (isFocused) 0.35f else 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isFocused) Color.White else accentColor,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    color = if (isFocused) Color.White.copy(alpha = 0.85f) else Color(0xFF94A3B8),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Sağlayıcı Seçimi Hızlı Çip Menüsü (ProviderQuickChipsBar):
 * Sağlayıcıları yatay kaydırılabilir çip kartları olarak tek tıkla/tek dokunuşla seçilebilir
 * şekilde sunar. Eski akordiyon menüdeki gibi menüyü açma/kapama gereksizliğini ortadan kaldırır.
 * Kumanda ile sağ-sol yapıp OK'a basıldığında veya ekrana dokunulduğunda anında geçiş yapar.
 */
@Composable
private fun ProviderQuickChipsBar(
    sources: List<Source>,
    activeSourceId: Long,
    onSelectSource: (Source) -> Unit,
    onAddSource: () -> Unit,
    health: SourcesViewModel.ProviderHealth? = null,
    modifier: Modifier = Modifier,
) {
    if (sources.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF0D1527))
                .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                .clickable(onClick = onAddSource)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("➕", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(R.string.dashboard_provider_empty),
                    color = Color(0xFF38BDF8),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                )
            }
        }
        return
    }

    val activeSource = remember(sources, activeSourceId) {
        sources.find { it.id == activeSourceId } ?: sources.firstOrNull()
    }
    val isDisconnected = health != null && !health.isConnected

    val borderColor = if (isDisconnected) {
        Color(0xFFEF4444)
    } else {
        Color(0xFF38BDF8).copy(alpha = 0.2f)
    }

    val barBg = if (isDisconnected) Color(0xFF1E1010) else Color(0xFF0D1527)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(barBg)
            .border(
                if (isDisconnected) 1.5.dp else 1.dp,
                borderColor,
                RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        // Mini Başlık: İkon + Sağlayıcı Sayısı + Hız/Ping Rozeti
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (isDisconnected) "⚠️" else "🌐", fontSize = 13.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    stringResource(R.string.dashboard_providers_title),
                    color = if (isDisconnected) Color(0xFFFCA5A5) else Color(0xFF94A3B8),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "(${sources.size})",
                    color = Color(0xFF64748B),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.5.sp,
                )

                // Hız Sonucu Rozeti (Saatlik test sonucu)
                if (health != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isDisconnected) Color(0xFFEF4444).copy(alpha = 0.18f)
                                else Color(0xFF10B981).copy(alpha = 0.2f)
                            )
                            .border(
                                1.dp,
                                if (isDisconnected) Color(0xFFEF4444).copy(alpha = 0.4f)
                                else Color(0xFF10B981).copy(alpha = 0.4f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 1.5.dp),
                    ) {
                        Text(
                            text = if (isDisconnected) stringResource(R.string.dashboard_provider_disconnected)
                            else "⚡ ${health.speedMbps} Mbps • ${health.pingMs}ms",
                            color = if (isDisconnected) Color(0xFFFCA5A5) else Color(0xFF34D399),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.dashboard_providers_quick_switch),
                color = Color(0xFF64748B),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
            )
        }

        Spacer(modifier = Modifier.height(7.dp))

        // Yatay Hızlı Sağlayıcı Çipleri
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(sources, key = { it.id }) { src ->
                val isSelected = activeSource?.id == src.id
                val itemInteraction = remember { MutableInteractionSource() }
                val isItemFocused by itemInteraction.collectIsFocusedAsState()

                Box(
                    modifier = Modifier
                        .widthIn(min = 140.dp, max = 180.dp)
                        .height(50.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                isItemFocused -> Color(0xFF0284C7).copy(alpha = 0.35f)
                                isSelected -> Color(0xFF1E293B)
                                else -> Color(0xFF131C2E)
                            }
                        )
                        .border(
                            width = when {
                                isItemFocused -> 2.dp
                                isSelected -> 1.5.dp
                                else -> 1.dp
                            },
                            color = when {
                                isItemFocused -> Color(0xFF38BDF8)
                                isSelected -> Color(0xFF22C55E)
                                else -> Color.White.copy(alpha = 0.12f)
                            },
                            shape = RoundedCornerShape(8.dp),
                        )
                        .clickable(
                            interactionSource = itemInteraction,
                            indication = null,
                            onClick = { onSelectSource(src) },
                        )
                        .focusable(interactionSource = itemInteraction)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = src.name,
                                color = if (isSelected) Color.White else Color(0xFFE2E8F0),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color(0xFF22C55E).copy(alpha = 0.22f))
                                        .padding(horizontal = 4.dp, vertical = 1.dp),
                                ) {
                                    Text(
                                        stringResource(R.string.dashboard_provider_active),
                                        color = Color(0xFF22C55E),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 7.5.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${src.kind.name} · ${hostOf(src.url)}",
                            color = if (isSelected) Color(0xFF86EFAC) else Color(0xFF94A3B8),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            // Yeni Sağlayıcı Ekle Butonu
            item {
                val addInteraction = remember { MutableInteractionSource() }
                val isAddFocused by addInteraction.collectIsFocusedAsState()

                Box(
                    modifier = Modifier
                        .height(50.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isAddFocused) Color(0xFF0284C7).copy(alpha = 0.35f) else Color(0xFF0F172A))
                        .border(
                            if (isAddFocused) 2.dp else 1.dp,
                            if (isAddFocused) Color(0xFF38BDF8) else Color(0xFF0284C7).copy(alpha = 0.35f),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable(
                            interactionSource = addInteraction,
                            indication = null,
                            onClick = onAddSource,
                        )
                        .focusable(interactionSource = addInteraction)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("➕", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            stringResource(R.string.dashboard_provider_add),
                            color = Color(0xFF38BDF8),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }
    }
}

/** Host only — the full URL carries the login and has no place on a settings list. */
private fun hostOf(url: String): String =
    runCatching { java.net.URI(url).host ?: url }.getOrDefault(url)

/**
 * Mobil / Tablet Aksiyon Barı:
 * Logo, Profil ve hızlı işlem butonları (Arama, Çoklu TV, Ayarlar).
 * Kullanıcının isteği üzerine abonelik kartının hemen altına yerleştirilmiştir.
 */
@Composable
private fun PhoneActionBarCard(
    title: String,
    activeProfileName: String,
    onOpenSearch: () -> Unit,
    onOpenMultiView: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfiles: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xF90A0F1D))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(46.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // App Logo & Brand Title + Profile
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onOpenProfiles),
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF090D16))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_opentv_logo),
                        contentDescription = "Logo",
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF22C55E)),
                        )
                    }
                    if (activeProfileName.isNotBlank() && activeProfileName != "Varsayılan") {
                        Text(
                            text = activeProfileName,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                        )
                    }
                }
            }

            // Action Icons (Arama, Çoklu TV, Ayarlar)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                IconButton(
                    onClick = onOpenSearch,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Arama",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(
                    onClick = onOpenMultiView,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = "Çoklu TV",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Ayarlar",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/**
 * Modern Ana Dizin Hero Grid (CANLI TV, FİLMLER, DİZİLER):
 * IPTV'nin en temel 3 medya alanını geniş, şık, camgöbeği/mor/indigo gradyanlı,
 * zengin rozetli ve D-pad kumanda odak efektli olarak sunar.
 */
@Composable
fun DashboardMediaHubGrid(
    formattedChannels: String,
    formattedMovies: String,
    formattedSeries: String,
    onNavigateLive: () -> Unit,
    onNavigateMovies: () -> Unit,
    onNavigateSeries: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 1. CANLI TV (LIVE TV)
        DashboardMediaHeroCard(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.dashboard_nav_live_tv),
            subtitle = if (formattedChannels.isNotBlank() && formattedChannels != "0") {
                stringResource(R.string.dashboard_nav_live_unit, formattedChannels)
            } else {
                stringResource(R.string.dashboard_nav_live_sub_fallback)
            },
            badge = stringResource(R.string.dashboard_nav_live_badge),
            isLiveBadge = true,
            icon = Icons.Outlined.LiveTv,
            gradientColors = listOf(Color(0xFF0369A1), Color(0xFF0284C7), Color(0xFF075985)),
            accentColor = Color(0xFF38BDF8),
            onClick = onNavigateLive,
        )

        // 2. FİLMLER (MOVIES)
        DashboardMediaHeroCard(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.dashboard_nav_movies),
            subtitle = if (formattedMovies.isNotBlank() && formattedMovies != "0") {
                stringResource(R.string.dashboard_nav_movies_unit, formattedMovies)
            } else {
                stringResource(R.string.dashboard_nav_movies_sub_fallback)
            },
            badge = stringResource(R.string.dashboard_nav_movies_badge),
            isLiveBadge = false,
            icon = Icons.Outlined.Movie,
            gradientColors = listOf(Color(0xFF4338CA), Color(0xFF6366F1), Color(0xFF312E81)),
            accentColor = Color(0xFF818CF8),
            onClick = onNavigateMovies,
        )

        // 3. DİZİLER (SERIES)
        DashboardMediaHeroCard(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.dashboard_nav_series),
            subtitle = if (formattedSeries.isNotBlank() && formattedSeries != "0") {
                stringResource(R.string.dashboard_nav_series_unit, formattedSeries)
            } else {
                stringResource(R.string.dashboard_nav_series_sub_fallback)
            },
            badge = stringResource(R.string.dashboard_nav_series_badge),
            isLiveBadge = false,
            icon = Icons.Outlined.VideoLibrary,
            gradientColors = listOf(Color(0xFF7C3AED), Color(0xFF8B5CF6), Color(0xFF4C1D95)),
            accentColor = Color(0xFFA78BFA),
            onClick = onNavigateSeries,
        )
    }
}

/** Eski referans uyumluluğu için yönlendirici fonksiyon */
@Composable
fun DashboardNavCardsRow(
    formattedChannels: String,
    formattedMovies: String,
    formattedSeries: String,
    onNavigateLive: () -> Unit,
    onNavigateMovies: () -> Unit,
    onNavigateSeries: () -> Unit,
    onOpenQr: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    DashboardMediaHubGrid(
        formattedChannels = formattedChannels,
        formattedMovies = formattedMovies,
        formattedSeries = formattedSeries,
        onNavigateLive = onNavigateLive,
        onNavigateMovies = onNavigateMovies,
        onNavigateSeries = onNavigateSeries,
        modifier = modifier,
    )
}

@Composable
private fun DashboardMediaHeroCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    badge: String,
    isLiveBadge: Boolean,
    icon: ImageVector,
    gradientColors: List<Color>,
    accentColor: Color,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.04f else 1.0f,
        animationSpec = tween(durationMillis = 150),
        label = "mediaHeroScale",
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .height(78.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.horizontalGradient(
                    if (isFocused) gradientColors.map { it.copy(alpha = 0.95f) }
                    else gradientColors.map { it.copy(alpha = 0.50f) }
                )
            )
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) Color.White else accentColor.copy(alpha = 0.40f),
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                // Elevated Frosted Glass Icon
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isFocused) Color.White.copy(alpha = 0.25f)
                            else Color.Black.copy(alpha = 0.35f)
                        )
                        .border(
                            1.dp,
                            if (isFocused) Color.White.copy(alpha = 0.6f)
                            else accentColor.copy(alpha = 0.35f),
                            RoundedCornerShape(10.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (isFocused) Color.White else accentColor,
                        modifier = Modifier.size(26.dp),
                    )
                }

                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = if (isFocused) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.70f),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Right Status Badge Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isFocused) Color.White.copy(alpha = 0.22f)
                        else if (isLiveBadge) Color(0xFF22C55E).copy(alpha = 0.25f)
                        else accentColor.copy(alpha = 0.22f)
                    )
                    .border(
                        1.dp,
                        if (isFocused) Color.White.copy(alpha = 0.6f)
                        else if (isLiveBadge) Color(0xFF22C55E).copy(alpha = 0.5f)
                        else accentColor.copy(alpha = 0.4f),
                        RoundedCornerShape(6.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = badge,
                    color = if (isFocused) Color.White
                    else if (isLiveBadge) Color(0xFF4ADE80)
                    else Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun AllContinueWatchingDialog(
    continueWatching: List<VodViewModel.ResumeItem>,
    onResume: (mediaKey: String, url: String, title: String) -> Unit,
    onRemoveItem: (VodViewModel.ResumeItem) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit,
    isTelevision: Boolean = true,
) {
    androidx.activity.compose.BackHandler(onBack = onDismiss)

    val firstItemFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(120)
        runCatching { firstItemFocus.requestFocus() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f)
            .background(Color.Black.copy(alpha = 0.88f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(if (isTelevision) 0.94f else 0.98f)
                .clip(RoundedCornerShape(20.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF0F172A), Color(0xFF0A0F1D))
                        )
                    )
                    .border(
                        1.5.dp,
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                Color(0xFF38BDF8).copy(alpha = 0.2f),
                                Color.White.copy(alpha = 0.05f),
                            )
                        ),
                        RoundedCornerShape(20.dp),
                    )
                    .padding(if (isTelevision) 24.dp else 16.dp),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(MaterialTheme.colorScheme.primary, Color(0xFF0284C7))
                                        )
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("⏱️", fontSize = 22.sp)
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = stringResource(R.string.continue_watching_all_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                                Text(
                                    text = stringResource(R.string.dashboard_content_count, continueWatching.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8),
                                )
                            }
                        }

                        // Right actions (Clear All & Close)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            if (continueWatching.isNotEmpty()) {
                                var clearFocused by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (clearFocused) Color(0xFFEF4444) else Color(0xFFEF4444).copy(alpha = 0.15f))
                                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .clickable { onClearAll() }
                                        .onFocusChanged { clearFocused = it.isFocused }
                                        .focusable()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteSweep,
                                            contentDescription = stringResource(R.string.continue_watching_clear_all),
                                            tint = if (clearFocused) Color.White else Color(0xFFEF4444),
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = stringResource(R.string.continue_watching_clear_all),
                                            color = if (clearFocused) Color.White else Color(0xFFEF4444),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }

                            var closeFocused by remember { mutableStateOf(false) }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(38.dp)
                                    .onFocusChanged { closeFocused = it.isFocused }
                                    .clip(CircleShape)
                                    .background(if (closeFocused) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f))
                                    .border(
                                        width = if (closeFocused) 2.dp else 1.dp,
                                        color = if (closeFocused) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f),
                                        shape = CircleShape,
                                    )
                                    .focusable(),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Close",
                                    tint = if (closeFocused) MaterialTheme.colorScheme.onPrimary else Color.White,
                                )
                            }
                        }
                    }

                    // Content: Empty State or Grid
                    if (continueWatching.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text("⏱️", fontSize = 42.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(R.string.continue_watching_empty),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    } else {
                        val columns = if (isTelevision) 4 else 2
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(continueWatching, key = { it.mediaKey }) { item ->
                                val isFirst = continueWatching.firstOrNull()?.mediaKey == item.mediaKey
                                AllContinueWatchingCard(
                                    item = item,
                                    isTelevision = isTelevision,
                                    focusRequester = if (isFirst) firstItemFocus else null,
                                    onResume = { onResume(item.mediaKey, item.streamUrl, item.title) },
                                    onRemove = { onRemoveItem(item) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

@Composable
private fun AllContinueWatchingCard(
    item: VodViewModel.ResumeItem,
    isTelevision: Boolean,
    focusRequester: FocusRequester? = null,
    onResume: () -> Unit,
    onRemove: () -> Unit,
) {
    var playFocused by remember { mutableStateOf(false) }
    var deleteFocused by remember { mutableStateOf(false) }

    val isAnyFocused = playFocused || deleteFocused

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isAnyFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color(0xFF1E293B).copy(alpha = 0.7f))
            .border(
                width = if (isAnyFocused) 2.dp else 1.dp,
                color = if (isAnyFocused) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(10.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Poster with embedded progress bar & badge (Clickable to Play)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.5f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onResume() },
            ) {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A)),
                )

                // Dark gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                startY = 40f,
                            )
                        )
                )

                // Percent badge (top right)
                val percent = (item.progress * 100).toInt().coerceIn(1, 99)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "%$percent",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                // Play icon overlay in center
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(if (playFocused) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Progress Bar along the bottom
                LinearProgressIndicator(
                    progress = { item.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .align(Alignment.BottomCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.25f),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = item.title,
                color = if (isAnyFocused) Color.White else Color(0xFFF1F5F9),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Action row (Resume button & Delete button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Resume button (D-pad focusable)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (playFocused) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f))
                        .border(
                            width = if (playFocused) 1.5.dp else 1.dp,
                            color = if (playFocused) Color.White else Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                        )
                        .clickable { onResume() }
                        .onFocusChanged { playFocused = it.isFocused }
                        .focusable()
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "▶ " + stringResource(R.string.dashboard_resume),
                        color = if (playFocused) MaterialTheme.colorScheme.onPrimary else Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                // Delete button (D-pad focusable)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (deleteFocused) Color(0xFFEF4444) else Color(0xFFEF4444).copy(alpha = 0.15f))
                        .border(
                            width = if (deleteFocused) 1.5.dp else 1.dp,
                            color = if (deleteFocused) Color.White else Color(0xFFEF4444).copy(alpha = 0.35f),
                            shape = RoundedCornerShape(8.dp),
                        )
                        .clickable { onRemove() }
                        .onFocusChanged { deleteFocused = it.isFocused }
                        .focusable(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = stringResource(R.string.continue_watching_remove_item),
                        tint = if (deleteFocused) Color.White else Color(0xFFEF4444),
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
        }
    }
}

