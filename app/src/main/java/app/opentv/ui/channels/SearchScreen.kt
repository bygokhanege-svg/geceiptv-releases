/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.ui.channels

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.opentv.R
import app.opentv.core.TvCastHelper
import app.opentv.data.model.Channel
import app.opentv.data.model.Movie
import app.opentv.data.model.Series
import app.opentv.data.model.shownName
import app.opentv.data.parser.displayTitle
import app.opentv.ui.ChannelsViewModel
import app.opentv.ui.VodViewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.CoroutineScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SearchScope {
    ALL,
    CHANNELS,
    MOVIES,
    SERIES,
}

/**
 * Channel search with an on-screen keyboard.
 *
 * Android TV's default text field punts to "type on your phone", which is useless if your phone
 * isn't to hand. This screen draws its own d-pad keyboard so search works with the remote alone,
 * and shows matching channels live as you type.
 */
@Composable
fun SearchScreen(
    initialQuery: String = "",
    initialScope: SearchScope = SearchScope.ALL,
    onPlayChannel: (Channel) -> Unit,
    onPlayMovie: (Movie) -> Unit,
    onOpenSeries: (Series) -> Unit,
    onBack: () -> Unit,
    viewModel: ChannelsViewModel = viewModel(),
    vodViewModel: VodViewModel = viewModel(),
) {
    val cleanInitialQuery = remember(initialQuery) {
        if (initialQuery == "{q}" || initialQuery == "(q)" || initialQuery.contains("{q}") || initialQuery.isBlank()) ""
        else initialQuery.replace("+", " ")
    }
    var query by remember(cleanInitialQuery) { mutableStateOf(cleanInitialQuery) }
    var selectedScope by remember(initialScope) { mutableStateOf(initialScope) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isTvPaired = remember { TvCastHelper.isTvPaired(context) }
    val isPhone = LocalConfiguration.current.screenWidthDp < 600

    val channelResults by viewModel.searchResults.collectAsState()
    val movieResults by vodViewModel.movieResults.collectAsState()
    val seriesResults by vodViewModel.seriesResults.collectAsState()
    val anyResults = channelResults.isNotEmpty() || movieResults.isNotEmpty() || seriesResults.isNotEmpty()

    val filteredChannels = if (selectedScope == SearchScope.ALL || selectedScope == SearchScope.CHANNELS) channelResults else emptyList()
    val filteredMovies = if (selectedScope == SearchScope.ALL || selectedScope == SearchScope.MOVIES) movieResults else emptyList()
    val filteredSeries = if (selectedScope == SearchScope.ALL || selectedScope == SearchScope.SERIES) seriesResults else emptyList()
    val hasResultsForScope = filteredChannels.isNotEmpty() || filteredMovies.isNotEmpty() || filteredSeries.isNotEmpty()

    val totalCount = channelResults.size + movieResults.size + seriesResults.size

    LaunchedEffect(query) {
        val clean = query.replace("+", " ")
        viewModel.setSearchQuery(clean)
        vodViewModel.setVodSearchQuery(clean)
    }
    BackHandler { onBack() }

    if (isPhone) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.nav_search),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = onBack) { Text(stringResource(R.string.common_done)) }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { if (it.length <= 40) query = it.replace("+", " ") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.search_type_name)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.common_cancel))
                        }
                    }
                },
            )

            Spacer(Modifier.height(10.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(SearchScope.values()) { scopeItem ->
                    val label = when (scopeItem) {
                        SearchScope.ALL -> stringResource(R.string.search_scope_all)
                        SearchScope.CHANNELS -> stringResource(R.string.nav_live_tv)
                        SearchScope.MOVIES -> stringResource(R.string.nav_movies)
                        SearchScope.SERIES -> stringResource(R.string.nav_shows)
                    }
                    val count = if (query.trim().length >= 2) {
                        when (scopeItem) {
                            SearchScope.ALL -> totalCount
                            SearchScope.CHANNELS -> channelResults.size
                            SearchScope.MOVIES -> movieResults.size
                            SearchScope.SERIES -> seriesResults.size
                        }
                    } else null
                    ScopeChip(
                        label = label,
                        isSelected = selectedScope == scopeItem,
                        count = count,
                        onClick = { selectedScope = scopeItem },
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    query.isBlank() -> Hint(stringResource(R.string.search_start_hint))
                    query.trim().length < 2 -> Hint(stringResource(R.string.common_keep_typing))
                    !hasResultsForScope -> {
                        if (anyResults) {
                            val scopeName = when (selectedScope) {
                                SearchScope.ALL -> stringResource(R.string.search_scope_all)
                                SearchScope.CHANNELS -> stringResource(R.string.nav_live_tv)
                                SearchScope.MOVIES -> stringResource(R.string.nav_movies)
                                SearchScope.SERIES -> stringResource(R.string.nav_shows)
                            }
                            Hint(stringResource(R.string.search_no_results_in_scope, scopeName, query))
                        } else {
                            Hint(stringResource(R.string.search_no_results, query))
                        }
                    }
                    else -> SearchResultsList(
                        selectedScope = selectedScope,
                        filteredChannels = filteredChannels,
                        filteredMovies = filteredMovies,
                        filteredSeries = filteredSeries,
                        onPlayChannel = onPlayChannel,
                        onPlayMovie = onPlayMovie,
                        onOpenSeries = onOpenSeries,
                        isTvPaired = isTvPaired,
                        context = context,
                        scope = scope,
                    )
                }
            }
        }
    } else {
        // TV Layout
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.nav_search), style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.width(20.dp))
                Text(
                    query.ifEmpty { stringResource(R.string.search_type_name) },
                    style = MaterialTheme.typography.titleLarge,
                    color = if (query.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = onBack) { Text(stringResource(R.string.common_done)) }
            }

            Spacer(Modifier.height(20.dp))

            Row(Modifier.fillMaxSize()) {
                OnScreenKeyboard(
                    onKey = { if (query.length < 40) query = (query + it).replace("+", " ") },
                    onSpace = { if (query.length < 40) query = (query + " ").replace("+", " ") },
                    onBackspace = { query = query.dropLast(1) },
                    onClear = { query = "" },
                )

                Spacer(Modifier.width(28.dp))

                Column(Modifier.weight(1f).fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SearchScope.values().forEach { scopeItem ->
                            val label = when (scopeItem) {
                                SearchScope.ALL -> stringResource(R.string.search_scope_all)
                                SearchScope.CHANNELS -> stringResource(R.string.nav_live_tv)
                                SearchScope.MOVIES -> stringResource(R.string.nav_movies)
                                SearchScope.SERIES -> stringResource(R.string.nav_shows)
                            }
                            val count = if (query.trim().length >= 2) {
                                when (scopeItem) {
                                    SearchScope.ALL -> totalCount
                                    SearchScope.CHANNELS -> channelResults.size
                                    SearchScope.MOVIES -> movieResults.size
                                    SearchScope.SERIES -> seriesResults.size
                                }
                            } else null
                            ScopeChip(
                                label = label,
                                isSelected = selectedScope == scopeItem,
                                count = count,
                                onClick = { selectedScope = scopeItem },
                            )
                        }
                    }

                    when {
                        query.isBlank() -> Hint(stringResource(R.string.search_start_hint))
                        query.trim().length < 2 -> Hint(stringResource(R.string.common_keep_typing))
                        !hasResultsForScope -> {
                            if (anyResults) {
                                val scopeName = when (selectedScope) {
                                    SearchScope.ALL -> stringResource(R.string.search_scope_all)
                                    SearchScope.CHANNELS -> stringResource(R.string.nav_live_tv)
                                    SearchScope.MOVIES -> stringResource(R.string.nav_movies)
                                    SearchScope.SERIES -> stringResource(R.string.nav_shows)
                                }
                                Hint(stringResource(R.string.search_no_results_in_scope, scopeName, query))
                            } else {
                                Hint(stringResource(R.string.search_no_results, query))
                            }
                        }
                        else -> SearchResultsList(
                            selectedScope = selectedScope,
                            filteredChannels = filteredChannels,
                            filteredMovies = filteredMovies,
                            filteredSeries = filteredSeries,
                            onPlayChannel = onPlayChannel,
                            onPlayMovie = onPlayMovie,
                            onOpenSeries = onOpenSeries,
                            isTvPaired = isTvPaired,
                            context = context,
                            scope = scope,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScopeChip(
    label: String,
    isSelected: Boolean,
    count: Int?,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val bg = when {
        focused -> MaterialTheme.colorScheme.primary
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }
    val contentColor = when {
        focused -> MaterialTheme.colorScheme.onPrimary
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .then(
                if (focused) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))
                else if (isSelected) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))
                else Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
            )
            .clickable(onClick = onClick)
            .onFocusChanged { focused = it.isFocused }
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        val text = if (count != null) "$label ($count)" else label
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected || focused) FontWeight.Bold else FontWeight.Medium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SearchResultsList(
    selectedScope: SearchScope,
    filteredChannels: List<ChannelsViewModel.Row>,
    filteredMovies: List<Movie>,
    filteredSeries: List<Series>,
    onPlayChannel: (Channel) -> Unit,
    onPlayMovie: (Movie) -> Unit,
    onOpenSeries: (Series) -> Unit,
    isTvPaired: Boolean,
    context: Context,
    scope: CoroutineScope,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (filteredChannels.isNotEmpty()) {
            if (selectedScope == SearchScope.ALL) {
                item { SectionHeader(stringResource(R.string.common_channels)) }
            }
            items(filteredChannels, key = { "c${it.key}" }) { row ->
                SearchResultRow(
                    row = row,
                    onClick = { onPlayChannel(row.primary) },
                    onCast = if (isTvPaired) {
                        { TvCastHelper.castChannel(context, scope, row.primary.id, row.primary.shownName) }
                    } else null,
                )
            }
        }
        if (filteredMovies.isNotEmpty()) {
            if (selectedScope == SearchScope.ALL) {
                item { SectionHeader(stringResource(R.string.nav_movies)) }
            }
            items(filteredMovies, key = { "m${it.id}" }) { movie ->
                VodResultRow(
                    name = movie.displayTitle,
                    posterUrl = movie.posterUrl,
                    subtitle = movie.year?.toString(),
                    onClick = { onPlayMovie(movie) },
                    onCast = if (isTvPaired) {
                        { TvCastHelper.castMediaUrl(context, scope, movie.streamUrl, movie.displayTitle, false) }
                    } else null,
                )
            }
        }
        if (filteredSeries.isNotEmpty()) {
            if (selectedScope == SearchScope.ALL) {
                item { SectionHeader(stringResource(R.string.nav_shows)) }
            }
            items(filteredSeries, key = { "s${it.id}" }) { show ->
                VodResultRow(
                    name = show.displayTitle,
                    posterUrl = show.posterUrl,
                    subtitle = show.year?.toString(),
                    onClick = { onOpenSeries(show) },
                )
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun VodResultRow(
    name: String,
    posterUrl: String?,
    subtitle: String?,
    onClick: () -> Unit,
    onCast: (() -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(10.dp))
            .background(if (focused) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .then(
                if (focused) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                else Modifier,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = posterUrl,
            contentDescription = null,
            modifier = Modifier.size(width = 34.dp, height = 48.dp).clip(RoundedCornerShape(6.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (onCast != null) {
            IconButton(
                onClick = onCast,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    Icons.Filled.Tv,
                    contentDescription = stringResource(R.string.vod_send_to_tv),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    row: ChannelsViewModel.Row,
    onClick: () -> Unit,
    onCast: (() -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(10.dp))
            .background(if (focused) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .then(
                if (focused) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                else Modifier,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = row.primary.logoUrl,
            contentDescription = null,
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                row.primary.shownName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                row.now?.let { "${searchTime(it.startUtcMillis)}  ${it.title}" } ?: stringResource(R.string.guide_no_info),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onCast != null) {
            IconButton(
                onClick = onCast,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    Icons.Filled.Tv,
                    contentDescription = stringResource(R.string.vod_send_to_tv),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

private val searchTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
private fun searchTime(utcMillis: Long): String = searchTimeFormat.format(Date(utcMillis))
