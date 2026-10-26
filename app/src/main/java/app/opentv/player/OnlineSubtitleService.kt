/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.player

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

data class OnlineSubtitle(
    val id: String,
    val name: String,
    val lang: String,
    val langLabel: String,
    val url: String,
    val source: String = "OpenSubtitles v3",
)

class OnlineSubtitleService(
    private val httpClient: OkHttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true },
) {
    suspend fun searchSubtitles(
        title: String,
        season: Int? = null,
        episode: Int? = null,
        imdbId: String? = null,
        enabledProviders: List<String> = listOf("OpenSubtitles v3", "TheSubtitleDB", "Top Community"),
    ): List<OnlineSubtitle> = withContext(Dispatchers.IO) {
        // If season/episode not passed explicitly, attempt extraction from title
        val (detectedSeason, detectedEpisode) = if (season == null || episode == null) {
            extractSeasonEpisode(title)
        } else {
            Pair(season, episode)
        }
        val actualSeason = season ?: detectedSeason
        val actualEpisode = episode ?: detectedEpisode

        val resolvedImdb = imdbId?.takeIf { it.startsWith("tt") }
            ?: resolveImdbId(title)
            ?: return@withContext emptyList()

        val isSeries = actualSeason != null && actualEpisode != null
        val type = if (isSeries) "series" else "movie"
        val idParam = if (isSeries) "$resolvedImdb:$actualSeason:$actualEpisode" else resolvedImdb

        Log.d(TAG, "Searching subtitles for '$title' -> IMDb: $resolvedImdb, type: $type, idParam: $idParam")

        val allProviders = listOf(
            "OpenSubtitles v3" to "https://opensubtitles-v3.strem.io/subtitles",
            "TheSubtitleDB" to "https://api.thesubtitledb.org/integrations/stremio/subtitles",
            "Top Community" to "https://subpool-diavelin.vercel.app/subtitles",
        )

        // Filter based on enabled provider settings (supporting legacy "SubDL" preference key mapping)
        val activeProviders = allProviders.filter { (name, _) ->
            if (enabledProviders.isEmpty()) true
            else if (name in enabledProviders) true
            else if (name == "TheSubtitleDB" && ("SubDL" in enabledProviders || "TheSubtitleDB" in enabledProviders)) true
            else false
        }

        val allResults: List<OnlineSubtitle> = coroutineScope {
            activeProviders.map { (sourceName, baseUrl) ->
                async {
                    fetchFromEndpoint(baseUrl, sourceName, type, idParam)
                }
            }.awaitAll().flatten()
        }

        // Deduplicate and prioritize Turkish first, then English, then other languages neatly grouped
        val seen = mutableSetOf<String>()
        val deduped = mutableListOf<OnlineSubtitle>()
        for (sub in allResults) {
            if (seen.add(sub.url)) {
                deduped.add(sub)
            }
        }

        deduped.sortedWith(
            compareBy<OnlineSubtitle> { sub ->
                when {
                    isTurkish(sub) -> 0
                    isEnglish(sub) -> 1
                    else -> 2
                }
            }.thenBy { it.langLabel }
                .thenBy { it.source }
                .thenBy { it.name },
        )
    }

    private fun fetchFromEndpoint(
        baseUrl: String,
        sourceName: String,
        type: String,
        idParam: String,
    ): List<OnlineSubtitle> {
        val url = "$baseUrl/$type/$idParam.json"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
            .build()

        return runCatching {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.d(TAG, "[$sourceName] HTTP ${response.code} for $url")
                    return emptyList()
                }
                val body = response.body?.string() ?: return emptyList()
                val root = json.parseToJsonElement(body).jsonObject
                val subtitlesArray = root["subtitles"]?.jsonArray ?: return emptyList()

                val list = mutableListOf<OnlineSubtitle>()
                for (elem in subtitlesArray) {
                    val obj = elem.jsonObject
                    val subUrl = obj["url"]?.jsonPrimitive?.contentOrNull ?: continue
                    val lang = obj["lang"]?.jsonPrimitive?.contentOrNull ?: "und"
                    val subName = obj["subtitleFileName"]?.jsonPrimitive?.contentOrNull
                        ?: obj["movieReleaseName"]?.jsonPrimitive?.contentOrNull
                        ?: obj["id"]?.jsonPrimitive?.contentOrNull
                        ?: "Altyazı"

                    val langLabel = when (lang.lowercase()) {
                        "tur", "tr", "turkish" -> "Türkçe"
                        "eng", "en", "english" -> "English"
                        "ger", "de", "german" -> "Deutsch"
                        "fre", "fr", "french" -> "Français"
                        "spa", "es", "spanish" -> "Español"
                        "ita", "it", "italian" -> "Italiano"
                        "rus", "ru", "russian" -> "Русский"
                        "ara", "ar", "arabic" -> "العربية"
                        else -> lang
                    }

                    list.add(
                        OnlineSubtitle(
                            id = "$sourceName:${obj["id"]?.jsonPrimitive?.contentOrNull ?: subUrl}",
                            name = subName,
                            lang = lang,
                            langLabel = langLabel,
                            url = subUrl,
                            source = sourceName,
                        ),
                    )
                }
                Log.d(TAG, "[$sourceName] Found ${list.size} subtitles for $idParam")
                list
            }
        }.onFailure { Log.d(TAG, "[$sourceName] Fetch failed: ${it.message}") }
            .getOrDefault(emptyList())
    }

    private fun resolveImdbId(rawTitle: String): String? {
        val trimmed = rawTitle.trim()
        if (trimmed.startsWith("tt") && trimmed.length >= 7) {
            return trimmed.substringBefore(":").substringBefore(" ")
        }

        val clean = cleanTitle(trimmed)
        if (clean.isBlank()) return null

        // 1st attempt: with cleaned title
        val firstResult = queryImdbSuggestions(clean)
        if (firstResult != null) return firstResult

        // 2nd attempt: with ASCII transliterated Turkish characters
        val asciiTitle = normalizeTurkishChars(clean)
        if (asciiTitle != clean) {
            val secondResult = queryImdbSuggestions(asciiTitle)
            if (secondResult != null) return secondResult
        }

        // 3rd attempt: if title contains a year, try without year
        val withoutYear = clean.replace(Regex("\\b(19|20)\\d{2}\\b"), "").trim()
        if (withoutYear.isNotBlank() && withoutYear != clean) {
            val thirdResult = queryImdbSuggestions(withoutYear)
            if (thirdResult != null) return thirdResult
        }

        return null
    }

    private fun queryImdbSuggestions(query: String): String? {
        if (query.isBlank()) return null
        val firstChar = toAsciiInitial(query.first())
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://v2.sg.media-imdb.com/suggestion/$firstChar/$encoded.json"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0")
            .build()

        return runCatching {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val root = json.parseToJsonElement(body).jsonObject
                val items = root["d"]?.jsonArray ?: return null
                for (item in items) {
                    val id = item.jsonObject["id"]?.jsonPrimitive?.contentOrNull
                    if (id != null && id.startsWith("tt")) {
                        return id
                    }
                }
                null
            }
        }.getOrNull()
    }

    private fun cleanTitle(title: String): String {
        return title
            // Remove leading country / category tags like "TR |", "EN:", "Film:", "Dizi:"
            .replace(Regex("^(?i)(?:TR|EN|DE|FR|FILM|DIZI|CINEMA|HD|UHD|4K)\\s*[:| -]\\s*"), "")
            // Remove season / episode indicators
            .replace(Regex("(?i)\\b(?:s\\d+\\s*e\\d+|\\d+\\.?\\s*(?:sezon|season)\\s*\\d+\\.?\\s*(?:bölüm|bolum|episode)?|\\d+\\.?\\s*(?:bölüm|bolum|episode))\\b"), " ")
            // Remove video resolution and format tags
            .replace(Regex("(?i)\\b(1080p|720p|4k|uhd|fhd|hd|hevc|x264|x265|bluray|web-dl|webdl|dvdrip|aac|tr|dublaj|altyazili|altyazi|turkce|dual|vod)\\b"), " ")
            // Remove brackets and punctuation
            .replace(Regex("[\\[\\]()._\\-]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun extractSeasonEpisode(title: String): Pair<Int?, Int?> {
        // Pattern 1: S01E02 or S1E2 or S01 E02
        val s1 = Regex("(?i)\\bs(\\d{1,2})\\s*e(\\d{1,3})\\b").find(title)
        if (s1 != null) {
            return Pair(s1.groupValues[1].toIntOrNull(), s1.groupValues[2].toIntOrNull())
        }
        // Pattern 2: 1. Sezon 2. Bölüm or 1.Sezon 2.Bolum
        val s2 = Regex("(?i)(\\d{1,2})\\.?\\s*(?:sezon|season)\\s*(\\d{1,3})\\.?\\s*(?:bölüm|bolum|episode)").find(title)
        if (s2 != null) {
            return Pair(s2.groupValues[1].toIntOrNull(), s2.groupValues[2].toIntOrNull())
        }
        // Pattern 3: Season 1 Episode 2
        val s3 = Regex("(?i)season\\s*(\\d{1,2})\\s*episode\\s*(\\d{1,3})").find(title)
        if (s3 != null) {
            return Pair(s3.groupValues[1].toIntOrNull(), s3.groupValues[2].toIntOrNull())
        }
        return Pair(null, null)
    }

    private fun toAsciiInitial(char: Char): Char {
        return when (char.lowercaseChar()) {
            'ş' -> 's'
            'ç' -> 'c'
            'ı', 'i', 'İ' -> 'i'
            'ö' -> 'o'
            'ü' -> 'u'
            'ğ' -> 'g'
            else -> if (char in 'a'..'z' || char in '0'..'9') char.lowercaseChar() else 'a'
        }
    }

    private fun normalizeTurkishChars(text: String): String {
        return text
            .replace('ş', 's').replace('Ş', 'S')
            .replace('ç', 'c').replace('Ç', 'C')
            .replace('ı', 'i').replace('İ', 'I')
            .replace('ö', 'o').replace('Ö', 'O')
            .replace('ü', 'u').replace('Ü', 'U')
            .replace('ğ', 'g').replace('Ğ', 'G')
    }

    companion object {
        const val TAG = "OnlineSubtitleService"

        fun isTurkish(sub: OnlineSubtitle): Boolean {
            val l = sub.lang.lowercase().trim()
            val label = sub.langLabel.lowercase().trim()
            val name = sub.name.lowercase().trim()
            return l in listOf("tur", "tr", "turkish", "turkce", "tr-tr", "tr_tr") ||
                l.startsWith("tr-") || l.startsWith("tr_") ||
                label.contains("türk") || label.contains("turk") ||
                name.contains("türkçe") || name.contains("turkish") ||
                name.contains("[tr]") || name.contains("(tr)") || name.contains(".tr.")
        }

        fun isEnglish(sub: OnlineSubtitle): Boolean {
            val l = sub.lang.lowercase().trim()
            val label = sub.langLabel.lowercase().trim()
            val name = sub.name.lowercase().trim()
            return l in listOf("eng", "en", "english", "en-us", "en-gb", "en_us", "en_gb") ||
                l.startsWith("en-") || l.startsWith("en_") ||
                label.contains("eng") || label.contains("ingiliz") ||
                name.contains("english") || name.contains("[en]") || name.contains("(en)") || name.contains(".en.")
        }
    }
}
