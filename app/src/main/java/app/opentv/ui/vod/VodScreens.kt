/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.ui.vod

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Close
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import app.opentv.R
import app.opentv.data.model.Movie
import app.opentv.data.model.Series
import app.opentv.data.model.Source
import app.opentv.data.parser.displayTitle
import app.opentv.ui.VodViewModel
import coil.compose.AsyncImage

/** Options for sorting VOD catalogs. */
enum class SortOption { DEFAULT, RATING, YEAR, NAME }

/**
 * Movies: a modern, row-based home — Continue Watching, Recommended, Recently added and a row per
 * genre, each a horizontally-scrolling shelf of poster cards. A category chip strip along the top
 * keeps whole-category browsing one press away without a permanent rail eating the width. Clicking
 * a film opens its detail page rather than playing straight away, the streaming-app convention.
 */
@Composable
fun MoviesScreen(
    onOpenMovie: (Movie) -> Unit,
    onResume: (mediaKey: String, url: String, title: String) -> Unit,
    onOpenSearch: () -> Unit,
    hasSources: Boolean,
    isSyncing: Boolean,
    viewModel: VodViewModel = viewModel(),
) {
    val categories by viewModel.movieCategories.collectAsState()
    val resume by viewModel.continueWatching.collectAsState()
    val recommended by viewModel.recommendedMovies.collectAsState()
    val recentlyAdded by viewModel.recentlyAddedMovies.collectAsState()
    val genreRows by viewModel.movieGenreRows.collectAsState()
    val categoryMovies by viewModel.movies.collectAsState()
    val vodLoading by viewModel.vodLoading.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val selectedSource by viewModel.selectedVodSource.collectAsState()

    // Pull the movie library the first time this tab is opened, not at login; refresh the computed
    // home rows (recommended, by-genre) on open too — cheap, and covers a library already on disk.
    LaunchedEffect(hasSources) {
        if (hasSources) viewModel.ensureVodLoaded()
        viewModel.loadHomeFeeds()
    }

    // null = the curated home rows; a category id = that category's full grid.
    var browseCategory by remember { mutableStateOf<String?>(null) }
    var sortOption by remember { mutableStateOf(SortOption.DEFAULT) }

    val sortedMovies = remember(categoryMovies, sortOption) {
        when (sortOption) {
            SortOption.RATING -> categoryMovies.sortedByDescending { it.rating ?: 0.0 }
            SortOption.YEAR -> categoryMovies.sortedByDescending { it.year ?: 0 }
            SortOption.NAME -> categoryMovies.sortedBy { it.name }
            SortOption.DEFAULT -> categoryMovies
        }
    }

    val hasContent = resume.isNotEmpty() || recommended.isNotEmpty() ||
        recentlyAdded.isNotEmpty() || genreRows.isNotEmpty()

    val heroItems = remember(recommended, recentlyAdded) {
        val candidates = (recommended + recentlyAdded).distinctBy { it.id }
        val withBackdrop = candidates.filter { !it.backdropUrl.isNullOrBlank() }
        val pool = if (withBackdrop.size >= 3) withBackdrop else candidates
        pool.take(6).map { movie ->
            VodHeroItem(
                id = movie.id,
                title = movie.displayTitle,
                backdropUrl = movie.backdropUrl,
                posterUrl = movie.posterUrl,
                rating = movie.rating,
                year = movie.year,
                genre = movie.genre,
                plot = movie.plot,
                isSeries = false,
                onClick = { onOpenMovie(movie) },
            )
        }
    }

    val isPhone = LocalConfiguration.current.screenWidthDp < 600
    var showCategoryDialog by remember { mutableStateOf(false) }
    var phoneActionTarget by remember { mutableStateOf<VodActionTarget?>(null) }

    if (isPhone) {
        Column(Modifier.fillMaxSize()) {
            VodPhoneTopBar(
                onOpenSearch = onOpenSearch,
                browseCategory = browseCategory,
                sortOption = sortOption,
                onNextSort = {
                    sortOption = SortOption.entries[(sortOption.ordinal + 1) % SortOption.entries.size]
                },
                onOpenAllCategories = { showCategoryDialog = true },
            )

            VodPhoneCategoryRow(
                categories = categories.map { it.id to it.name },
                selectedCategory = browseCategory,
                onSelectHome = { browseCategory = null },
                onSelectCategory = { id -> browseCategory = id; viewModel.selectMovieCategory(id) },
                onRequestAction = { phoneActionTarget = it },
            )

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    browseCategory != null -> MovieCategoryGrid(sortedMovies, viewModel, onOpenMovie)
                    !hasContent -> when {
                        vodLoading || isSyncing -> LoadingVod(stringResource(R.string.vod_loading_movies))
                        hasSources -> EmptyVod(stringResource(R.string.vod_no_movies), stringResource(R.string.vod_no_movies_provider))
                        else -> EmptyVod(stringResource(R.string.vod_no_movies), stringResource(R.string.vod_no_movies_add))
                    }
                    else -> LazyColumn(
                        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (heroItems.isNotEmpty()) item(key = "hero_carousel") {
                            VodHeroCarousel(items = heroItems)
                        }
                        if (resume.isNotEmpty()) item(key = "cw") {
                            ContinueWatchingRow(
                                items = resume,
                                onResume = onResume,
                                onRemove = { viewModel.removeContinueWatching(it.mediaKey) },
                            )
                        }
                        if (recommended.isNotEmpty()) item(key = "rec") {
                            MoviePosterRow(stringResource(R.string.vod_recommended), recommended, onOpenMovie)
                        }
                        if (recentlyAdded.isNotEmpty()) item(key = "recent") {
                            MoviePosterRow(stringResource(R.string.vod_recently_added), recentlyAdded, onOpenMovie)
                        }
                        items(genreRows, key = { "g:${it.genre}" }) { group ->
                            MoviePosterRow(group.genre, group.items, onOpenMovie)
                        }
                    }
                }
            }
        }
    } else {
        Row(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .width(260.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                SearchAffordance(onOpenSearch)
                if (browseCategory != null) {
                    SortAffordance(sortOption) {
                        sortOption = SortOption.entries[(sortOption.ordinal + 1) % SortOption.entries.size]
                    }
                }
                if (sources.size > 1 && selectedSource == null) {
                    ProviderMenu(
                        sources = sources,
                        selected = selectedSource,
                        onSelectAll = { browseCategory = null; viewModel.selectVodSource(null) },
                        onSelectSource = { id -> browseCategory = null; viewModel.selectVodSource(id) },
                    )
                }
                CategoryMenu(
                    entries = categories.map { it.id to it.name },
                    selected = browseCategory,
                    onSelectHome = { browseCategory = null },
                    onSelectCategory = { id -> browseCategory = id; viewModel.selectMovieCategory(id) },
                    onHideCategories = { ids, _ ->
                        if (browseCategory in ids) {
                            browseCategory = null
                            viewModel.selectMovieCategory(null)
                        }
                        viewModel.hideCategories(ids)
                    },
                )
            }

            Box(Modifier.weight(1f).fillMaxHeight()) {
                when {
                    browseCategory != null -> MovieCategoryGrid(sortedMovies, viewModel, onOpenMovie)
                    !hasContent -> when {
                        vodLoading || isSyncing -> LoadingVod(stringResource(R.string.vod_loading_movies))
                        hasSources -> EmptyVod(stringResource(R.string.vod_no_movies), stringResource(R.string.vod_no_movies_provider))
                        else -> EmptyVod(stringResource(R.string.vod_no_movies), stringResource(R.string.vod_no_movies_add))
                    }
                    else -> LazyColumn(
                        contentPadding = PaddingValues(vertical = 16.dp, horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (heroItems.isNotEmpty()) item(key = "hero_carousel") {
                            VodHeroCarousel(items = heroItems)
                        }
                        if (resume.isNotEmpty()) item(key = "cw") {
                            ContinueWatchingRow(
                                items = resume,
                                onResume = onResume,
                                onRemove = { viewModel.removeContinueWatching(it.mediaKey) },
                            )
                        }
                        if (recommended.isNotEmpty()) item(key = "rec") {
                            MoviePosterRow(stringResource(R.string.vod_recommended), recommended, onOpenMovie)
                        }
                        if (recentlyAdded.isNotEmpty()) item(key = "recent") {
                            MoviePosterRow(stringResource(R.string.vod_recently_added), recentlyAdded, onOpenMovie)
                        }
                        items(genreRows, key = { "g:${it.genre}" }) { group ->
                            MoviePosterRow(group.genre, group.items, onOpenMovie)
                        }
                    }
                }
            }
        }
    }

    if (showCategoryDialog) {
        VodAllCategoriesDialog(
            categories = categories.map { it.id to it.name },
            selectedCategory = browseCategory,
            onDismiss = { showCategoryDialog = false },
            onSelectHome = { browseCategory = null },
            onSelectCategory = { id -> browseCategory = id; viewModel.selectMovieCategory(id) },
            onRequestAction = { phoneActionTarget = it },
        )
    }

    phoneActionTarget?.let { target ->
        VodActionModal(
            target = target,
            onDismiss = { phoneActionTarget = null },
            onHideCategories = { ids, _ ->
                if (browseCategory in ids) {
                    browseCategory = null
                    viewModel.selectMovieCategory(null)
                }
                viewModel.hideCategories(ids)
            },
        )
    }
}

/**
 * Shows: the same row-based home as Movies (Continue Watching, Recently added, genre rows) with the
 * category chip strip for whole-category browsing. A show opens its detail page — where episodes are
 * fetched on demand, since pulling every episode of every series up front is what makes a first sync
 * take forever.
 */
@Composable
fun SeriesScreen(
    onOpenSeries: (Series) -> Unit,
    onResume: (mediaKey: String, url: String, title: String) -> Unit,
    onOpenSearch: () -> Unit,
    hasSources: Boolean,
    isSyncing: Boolean,
    viewModel: VodViewModel = viewModel(),
) {
    val categories by viewModel.seriesCategories.collectAsState()
    val resume by viewModel.continueWatching.collectAsState()
    val recentlyAdded by viewModel.recentlyAddedSeries.collectAsState()
    val genreRows by viewModel.seriesGenreRows.collectAsState()
    val categorySeries by viewModel.series.collectAsState()
    val vodLoading by viewModel.vodLoading.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val selectedSource by viewModel.selectedVodSource.collectAsState()

    LaunchedEffect(hasSources) {
        if (hasSources) viewModel.ensureVodLoaded()
        viewModel.loadHomeFeeds()
    }

    var browseCategory by remember { mutableStateOf<String?>(null) }
    var sortOption by remember { mutableStateOf(SortOption.DEFAULT) }

    val sortedSeries = remember(categorySeries, sortOption) {
        when (sortOption) {
            SortOption.RATING -> categorySeries.sortedByDescending { it.rating ?: 0.0 }
            SortOption.YEAR -> categorySeries.sortedByDescending { it.year ?: 0 }
            SortOption.NAME -> categorySeries.sortedBy { it.name }
            SortOption.DEFAULT -> categorySeries
        }
    }

    val hasContent = resume.isNotEmpty() || recentlyAdded.isNotEmpty() || genreRows.isNotEmpty()

    val heroItems = remember(recentlyAdded, genreRows) {
        val allSeries = (recentlyAdded + genreRows.flatMap { it.items }).distinctBy { it.id }
        val withBackdrop = allSeries.filter { !it.backdropUrl.isNullOrBlank() }
        val pool = if (withBackdrop.size >= 3) withBackdrop else allSeries
        pool.take(6).map { s ->
            VodHeroItem(
                id = s.id,
                title = s.displayTitle,
                backdropUrl = s.backdropUrl,
                posterUrl = s.posterUrl,
                rating = s.rating,
                year = s.year,
                genre = s.genre,
                plot = s.plot,
                isSeries = true,
                onClick = { onOpenSeries(s) },
            )
        }
    }

    val isPhone = LocalConfiguration.current.screenWidthDp < 600
    var showCategoryDialog by remember { mutableStateOf(false) }
    var phoneActionTarget by remember { mutableStateOf<VodActionTarget?>(null) }

    if (isPhone) {
        Column(Modifier.fillMaxSize()) {
            VodPhoneTopBar(
                onOpenSearch = onOpenSearch,
                browseCategory = browseCategory,
                sortOption = sortOption,
                onNextSort = {
                    sortOption = SortOption.entries[(sortOption.ordinal + 1) % SortOption.entries.size]
                },
                onOpenAllCategories = { showCategoryDialog = true },
            )

            VodPhoneCategoryRow(
                categories = categories.map { it.id to it.name },
                selectedCategory = browseCategory,
                onSelectHome = { browseCategory = null },
                onSelectCategory = { id -> browseCategory = id; viewModel.selectSeriesCategory(id) },
                onRequestAction = { phoneActionTarget = it },
            )

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    browseCategory != null -> SeriesCategoryGrid(sortedSeries, onOpenSeries)
                    !hasContent -> when {
                        vodLoading || isSyncing -> LoadingVod(stringResource(R.string.vod_loading_shows))
                        hasSources -> EmptyVod(stringResource(R.string.vod_no_shows), stringResource(R.string.vod_no_shows_provider))
                        else -> EmptyVod(stringResource(R.string.vod_no_shows), stringResource(R.string.vod_no_shows_add))
                    }
                    else -> LazyColumn(
                        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (heroItems.isNotEmpty()) item(key = "hero_carousel") {
                            VodHeroCarousel(items = heroItems)
                        }
                        if (resume.isNotEmpty()) item(key = "cw") {
                            ContinueWatchingRow(
                                items = resume,
                                onResume = onResume,
                                onRemove = { viewModel.removeContinueWatching(it.mediaKey) },
                            )
                        }
                        if (recentlyAdded.isNotEmpty()) item(key = "recent") {
                            SeriesPosterRow(stringResource(R.string.vod_recently_added), recentlyAdded, onOpenSeries)
                        }
                        items(genreRows, key = { "g:${it.genre}" }) { group ->
                            SeriesPosterRow(group.genre, group.items, onOpenSeries)
                        }
                    }
                }
            }
        }
    } else {
        Row(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .width(260.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                SearchAffordance(onOpenSearch)
                if (browseCategory != null) {
                    SortAffordance(sortOption) {
                        sortOption = SortOption.entries[(sortOption.ordinal + 1) % SortOption.entries.size]
                    }
                }
                if (sources.size > 1 && selectedSource == null) {
                    ProviderMenu(
                        sources = sources,
                        selected = selectedSource,
                        onSelectAll = { browseCategory = null; viewModel.selectVodSource(null) },
                        onSelectSource = { id -> browseCategory = null; viewModel.selectVodSource(id) },
                    )
                }
                CategoryMenu(
                    entries = categories.map { it.id to it.name },
                    selected = browseCategory,
                    onSelectHome = { browseCategory = null },
                    onSelectCategory = { id -> browseCategory = id; viewModel.selectSeriesCategory(id) },
                    onHideCategories = { ids, _ ->
                        if (browseCategory in ids) {
                            browseCategory = null
                            viewModel.selectSeriesCategory(null)
                        }
                        viewModel.hideCategories(ids)
                    },
                )
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                when {
                    browseCategory != null -> SeriesCategoryGrid(sortedSeries, onOpenSeries)
                    !hasContent -> when {
                        vodLoading || isSyncing -> LoadingVod(stringResource(R.string.vod_loading_shows))
                        hasSources -> EmptyVod(stringResource(R.string.vod_no_shows), stringResource(R.string.vod_no_shows_provider))
                        else -> EmptyVod(stringResource(R.string.vod_no_shows), stringResource(R.string.vod_no_shows_add))
                    }
                    else -> LazyColumn(
                        contentPadding = PaddingValues(vertical = 16.dp, horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (heroItems.isNotEmpty()) item(key = "hero_carousel") {
                            VodHeroCarousel(items = heroItems)
                        }
                        if (resume.isNotEmpty()) item(key = "cw") {
                            ContinueWatchingRow(
                                items = resume,
                                onResume = onResume,
                                onRemove = { viewModel.removeContinueWatching(it.mediaKey) },
                            )
                        }
                        if (recentlyAdded.isNotEmpty()) item(key = "recent") {
                            SeriesPosterRow(stringResource(R.string.vod_recently_added), recentlyAdded, onOpenSeries)
                        }
                        items(genreRows, key = { "g:${it.genre}" }) { group ->
                            SeriesPosterRow(group.genre, group.items, onOpenSeries)
                        }
                    }
                }
            }
        }
    }

    if (showCategoryDialog) {
        VodAllCategoriesDialog(
            categories = categories.map { it.id to it.name },
            selectedCategory = browseCategory,
            onDismiss = { showCategoryDialog = false },
            onSelectHome = { browseCategory = null },
            onSelectCategory = { id -> browseCategory = id; viewModel.selectSeriesCategory(id) },
            onRequestAction = { phoneActionTarget = it },
        )
    }

    phoneActionTarget?.let { target ->
        VodActionModal(
            target = target,
            onDismiss = { phoneActionTarget = null },
            onHideCategories = { ids, _ ->
                if (browseCategory in ids) {
                    browseCategory = null
                    viewModel.selectSeriesCategory(null)
                }
                viewModel.hideCategories(ids)
            },
        )
    }
}

// ---- Whole-category browse grids ---------------------------------------------------------------

/** One category's films as a poster grid. Quality variants collapse to one card, badged. */
@Composable
private fun MovieCategoryGrid(movies: List<Movie>, viewModel: VodViewModel, onOpenMovie: (Movie) -> Unit) {
    if (movies.isEmpty()) { LoadingVod(stringResource(R.string.vod_loading_movies)); return }
    val groups = remember(movies) { viewModel.collapseVariants(movies) }
    val isPhone = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 600
    val columns = if (isPhone) GridCells.Fixed(3) else GridCells.Adaptive(minSize = 140.dp)
    LazyVerticalGrid(
        columns = columns,
        contentPadding = PaddingValues(if (isPhone) 8.dp else 16.dp),
        horizontalArrangement = Arrangement.spacedBy(if (isPhone) 8.dp else 12.dp),
        verticalArrangement = Arrangement.spacedBy(if (isPhone) 10.dp else 16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        gridItems(groups, key = { it.primary.id }) { group ->
            val quality = group.variants.firstOrNull()?.qualityLabel?.takeIf { it.isNotBlank() }
            val badge = quality
                ?: if (group.hasMultipleQualities) {
                    stringResource(R.string.guide_qualities_count, group.variants.size)
                } else {
                    null
                }
            PosterCard(
                title = group.primary.displayTitle,
                posterUrl = group.primary.posterUrl,
                subtitle = group.primary.year?.toString(),
                rating = group.primary.rating,
                qualityBadge = badge,
                onClick = { onOpenMovie(group.primary) },
                modifier = if (isPhone) Modifier.fillMaxWidth() else Modifier,
            )
        }
    }
}

/** One category's shows as a poster grid. */
@Composable
private fun SeriesCategoryGrid(series: List<Series>, onOpenSeries: (Series) -> Unit) {
    if (series.isEmpty()) { LoadingVod(stringResource(R.string.vod_loading_shows)); return }
    val isPhone = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 600
    val columns = if (isPhone) GridCells.Fixed(3) else GridCells.Adaptive(minSize = 140.dp)
    LazyVerticalGrid(
        columns = columns,
        contentPadding = PaddingValues(if (isPhone) 8.dp else 16.dp),
        horizontalArrangement = Arrangement.spacedBy(if (isPhone) 8.dp else 12.dp),
        verticalArrangement = Arrangement.spacedBy(if (isPhone) 10.dp else 16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        gridItems(series, key = { it.id }) { item ->
            PosterCard(
                title = item.displayTitle,
                posterUrl = item.posterUrl,
                subtitle = item.year?.toString(),
                rating = item.rating,
                onClick = { onOpenSeries(item) },
                modifier = if (isPhone) Modifier.fillMaxWidth() else Modifier,
            )
        }
    }
}

// ---- Shared shelves ----------------------------------------------------------------------------

/** A titled horizontal shelf of movie poster cards. Shared by the home and the detail's "more like this". */
@Composable
internal fun MoviePosterRow(title: String, movies: List<Movie>, onOpenMovie: (Movie) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        SectionHeader(title)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(movies, key = { it.id }) { movie ->
                PosterCard(
                    title = movie.displayTitle,
                    posterUrl = movie.posterUrl,
                    subtitle = movie.year?.toString(),
                    rating = movie.rating,
                    onClick = { onOpenMovie(movie) },
                )
            }
        }
    }
}

/** A titled horizontal shelf of series poster cards. */
@Composable
internal fun SeriesPosterRow(title: String, series: List<Series>, onOpenSeries: (Series) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        SectionHeader(title)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(series, key = { it.id }) { item ->
                PosterCard(
                    title = item.displayTitle,
                    posterUrl = item.posterUrl,
                    subtitle = item.year?.toString(),
                    rating = item.rating,
                    onClick = { onOpenSeries(item) },
                )
            }
        }
    }
}

@Composable
internal fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
    )
}

/**
 * The reusable poster card: art, title and an optional year, with a rating chip, a quality badge and
 * a resume progress bar drawn over the art where the data is there. The focused card scales up and
 * gains a primary border — the app's established focus cue — and, being focusable, the lazy row
 * brings it into view on its own.
 */
@Composable
internal fun PosterCard(
    title: String,
    posterUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    rating: Double? = null,
    qualityBadge: String? = null,
    progress: Float? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, label = "posterScale")
    val cardModifier = if (modifier == Modifier) Modifier.width(POSTER_WIDTH) else modifier
    Column(
        cardModifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .then(
                    if (focused) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                    else Modifier,
                ),
        ) {
            AsyncImage(
                model = posterUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            rating?.takeIf { it > 0.0 }?.let {
                Badge(
                    text = "★ ${formatRating(it)}",
                    modifier = Modifier.align(Alignment.BottomStart).padding(6.dp),
                )
            }
            qualityBadge?.let {
                Badge(
                    text = it,
                    highlight = true,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                )
            }
            progress?.let {
                LinearProgressIndicator(
                    progress = { it },
                    modifier = Modifier.fillMaxWidth().height(4.dp).align(Alignment.BottomCenter),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (focused) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        subtitle?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

/** A small rounded chip drawn over poster art — a rating or a quality label. */
@Composable
private fun Badge(text: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    val bg = if (highlight) MaterialTheme.colorScheme.primary
    else androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.66f)
    val fg = if (highlight) MaterialTheme.colorScheme.onPrimary else androidx.compose.ui.graphics.Color.White
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = fg,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

// ---- Continue watching -------------------------------------------------------------------------

@Composable
internal fun ContinueWatchingRow(
    items: List<VodViewModel.ResumeItem>,
    onResume: (mediaKey: String, url: String, title: String) -> Unit,
    onRemove: ((VodViewModel.ResumeItem) -> Unit)? = null,
) {
    Column(Modifier.fillMaxWidth()) {
        SectionHeader(stringResource(R.string.vod_continue_watching))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { it.mediaKey }) { item ->
                ResumeCard(
                    item = item,
                    onClick = { onResume(item.mediaKey, item.streamUrl, item.title) },
                    onRemove = if (onRemove != null) { { onRemove(item) } } else null,
                )
            }
        }
    }
}

/** A landscape resume thumbnail with a progress fill — a movie or an episode part-way through. */
@Composable
private fun ResumeCard(
    item: VodViewModel.ResumeItem,
    onClick: () -> Unit,
    onRemove: (() -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, label = "resumeScale")
    Column(
        Modifier
            .width(190.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(107.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .then(
                    if (focused) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                    else Modifier,
                ),
        ) {
            AsyncImage(
                model = item.posterUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (onRemove != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.7f))
                        .clickable { onRemove() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.continue_watching_remove_item),
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            LinearProgressIndicator(
                progress = { item.progress },
                modifier = Modifier.fillMaxWidth().height(4.dp).align(Alignment.BottomStart),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            item.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (focused) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ---- Category chips ----------------------------------------------------------------------------

private data class VodActionTarget(
    val title: String,
    val subtitle: String,
    val categoryIds: List<String>,
    val onOpen: () -> Unit,
)

/**
 * A horizontal chip strip along the top of the home: an "All" chip returns to the curated rows, and
 * each following chip opens that category's full grid. Keeps whole-category browsing reachable on a
 * d-pad without a permanent side rail taking the width.
 */
@Composable
private fun CategoryMenu(
    entries: List<Pair<String, String>>,
    selected: String?,
    onSelectHome: () -> Unit,
    onSelectCategory: (String) -> Unit,
    onHideCategories: (List<String>, String) -> Unit = { _, _ -> },
) {
    var actionDialogTarget by remember { mutableStateOf<VodActionTarget?>(null) }
    val grouped = remember(entries) {
        val groups = mutableMapOf<String, MutableList<Pair<String, String>>>()
        val singles = mutableListOf<Pair<String, String>>()
        
        entries.forEach { (id, name) ->
            val match = Regex("^(.+?)\\s*[|:\\-]\\s*(.+)$").find(name)
            if (match != null) {
                groups.getOrPut(match.groupValues[1].trim()) { mutableListOf() }.add(id to match.groupValues[2].trim())
            } else {
                val spaceMatch = Regex("^(\\S+)\\s+(.+)$").find(name)
                if (spaceMatch != null) {
                    groups.getOrPut(spaceMatch.groupValues[1].trim()) { mutableListOf() }.add(id to spaceMatch.groupValues[2].trim())
                } else {
                    singles.add(id to name)
                }
            }
        }
        
        val finalGroups = mutableMapOf<String, List<Pair<String, String>>>()
        val finalSingles = mutableListOf<Pair<String, String>>()
        groups.forEach { (prefix, list) ->
            if (list.size > 1) {
                finalGroups[prefix] = list
            } else {
                val orig = entries.first { it.first == list.first().first }
                finalSingles.add(orig)
            }
        }
        finalSingles.addAll(singles)
        Pair(finalGroups, finalSingles)
    }

    val (groups, singles) = grouped
    val expanded = remember { mutableStateMapOf<String, Boolean>() }

    LazyColumn(
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
    ) {
        item(key = "label") {
            Text(
                stringResource(R.string.vod_categories),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        item(key = "all") { MenuItem(stringResource(R.string.vod_all), selected == null, onSelectHome) }
        
        groups.forEach { (prefix, list) ->
            val isExpanded = expanded[prefix] == true
            val isAnyChildSelected = list.any { it.first == selected }
            item(key = "group_$prefix") {
                MenuItem(
                    label = if (isExpanded) "▼ $prefix" else "▶ $prefix",
                    selected = isAnyChildSelected && !isExpanded,
                    onClick = { expanded[prefix] = !isExpanded },
                    onLongClick = {
                        actionDialogTarget = VodActionTarget(
                            title = prefix,
                            subtitle = "Kategori Grubu (${list.size} kategori)",
                            categoryIds = list.map { it.first },
                            onOpen = {
                                expanded[prefix] = true
                                list.firstOrNull()?.first?.let { onSelectCategory(it) }
                            },
                        )
                    },
                )
            }
            if (isExpanded) {
                items(list, key = { it.first }) { (id, name) ->
                    Row {
                        Spacer(Modifier.width(16.dp))
                        Box(Modifier.weight(1f)) {
                            MenuItem(
                                label = name,
                                selected = selected == id,
                                onClick = { onSelectCategory(id) },
                                onLongClick = {
                                    actionDialogTarget = VodActionTarget(
                                        title = name,
                                        subtitle = "$prefix / $name",
                                        categoryIds = listOf(id),
                                        onOpen = { onSelectCategory(id) },
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }

        items(singles, key = { it.first }) { (id, name) ->
            MenuItem(
                label = name,
                selected = selected == id,
                onClick = { onSelectCategory(id) },
                onLongClick = {
                    actionDialogTarget = VodActionTarget(
                        title = name,
                        subtitle = "Kategori",
                        categoryIds = listOf(id),
                        onOpen = { onSelectCategory(id) },
                    )
                },
            )
        }
    }

    // Modal dialog with 3 options: Aç, Gizle, İptal
    actionDialogTarget?.let { target ->
        VodActionModal(
            target = target,
            onDismiss = { actionDialogTarget = null },
            onHideCategories = onHideCategories,
        )
    }
}

@Composable
private fun VodActionModal(
    target: VodActionTarget,
    onDismiss: () -> Unit,
    onHideCategories: (List<String>, String) -> Unit,
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .width(420.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp),
        ) {
            Text(
                text = target.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = target.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(18.dp))

            // Seçenek 1: Aç
            VodActionRow(stringResource(R.string.category_action_open), primary = true) {
                target.onOpen()
                onDismiss()
            }

            // Seçenek 2: Gizle
            VodActionRow(stringResource(R.string.category_action_hide)) {
                onHideCategories(target.categoryIds, target.title)
                Toast.makeText(context, context.getString(R.string.category_action_hidden_toast, target.title), Toast.LENGTH_SHORT).show()
                onDismiss()
            }

            // Seçenek 3: İptal
            VodActionRow(stringResource(R.string.common_cancel)) {
                onDismiss()
            }
        }
    }
}

@Composable
private fun VodPhoneTopBar(
    onOpenSearch: () -> Unit,
    browseCategory: String?,
    sortOption: SortOption,
    onNextSort: () -> Unit,
    onOpenAllCategories: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Arama Butonu (Hap şeklinde)
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .clickable(onClick = onOpenSearch)
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.vod_search_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Sıralama Butonu (kategori seçiliyse)
        if (browseCategory != null) {
            val sortLabel = when (sortOption) {
                SortOption.RATING -> "★ Puan"
                SortOption.YEAR -> "📅 Yıl"
                SortOption.NAME -> "A-Z"
                SortOption.DEFAULT -> "⇅ Sırala"
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f))
                    .clickable(onClick = onNextSort)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    sortLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        // Tüm Kategoriler Butonu
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                .clickable(onClick = onOpenAllCategories)
                .padding(horizontal = 12.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "📁 Kategoriler",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VodPhoneCategoryRow(
    categories: List<Pair<String, String>>,
    selectedCategory: String?,
    onSelectHome: () -> Unit,
    onSelectCategory: (String) -> Unit,
    onRequestAction: (VodActionTarget) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Tümü Chip
        item(key = "phone_all") {
            val isSelected = selectedCategory == null
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                    .clickable(onClick = onSelectHome)
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Text(
                    text = "⭐ " + stringResource(R.string.vod_all),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(categories, key = { "phone_${it.first}" }) { (id, name) ->
            val isSelected = selectedCategory == id
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                    .combinedClickable(
                        onClick = { onSelectCategory(id) },
                        onLongClick = {
                            onRequestAction(
                                VodActionTarget(
                                    title = name,
                                    subtitle = "Kategori",
                                    categoryIds = listOf(id),
                                    onOpen = { onSelectCategory(id) },
                                )
                            )
                        },
                    )
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VodAllCategoriesDialog(
    categories: List<Pair<String, String>>,
    selectedCategory: String?,
    onDismiss: () -> Unit,
    onSelectHome: () -> Unit,
    onSelectCategory: (String) -> Unit,
    onRequestAction: (VodActionTarget) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(categories, searchQuery) {
        if (searchQuery.isBlank()) categories
        else categories.filter { it.second.contains(searchQuery, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "📁 " + stringResource(R.string.vod_categories),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Kapat",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Kategori ara...", fontSize = 14.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )

            Spacer(Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (searchQuery.isBlank()) {
                    item(key = "dlg_all") {
                        MenuItem(
                            label = "⭐ " + stringResource(R.string.vod_all),
                            selected = selectedCategory == null,
                            onClick = {
                                onSelectHome()
                                onDismiss()
                            },
                        )
                    }
                }

                items(filtered, key = { "dlg_${it.first}" }) { (id, name) ->
                    MenuItem(
                        label = name,
                        selected = selectedCategory == id,
                        onClick = {
                            onSelectCategory(id)
                            onDismiss()
                        },
                        onLongClick = {
                            onRequestAction(
                                VodActionTarget(
                                    title = name,
                                    subtitle = "Kategori",
                                    categoryIds = listOf(id),
                                    onOpen = {
                                        onSelectCategory(id)
                                        onDismiss()
                                    },
                                )
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun VodActionRow(label: String, primary: Boolean = false, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val bg = when {
        focused -> MaterialTheme.colorScheme.primary
        primary -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val fg = when {
        focused -> MaterialTheme.colorScheme.onPrimary
        primary -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = if (primary) FontWeight.SemiBold else FontWeight.Normal,
        color = fg,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun ProviderMenu(
    sources: List<Source>,
    selected: Long?,
    onSelectAll: () -> Unit,
    onSelectSource: (Long) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.channels_manager_source_header),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        MenuItem(stringResource(R.string.channels_manager_all_sources), selected == null, onSelectAll)
        sources.forEach { source ->
            MenuItem(source.name, selected == source.id, onClick = { onSelectSource(source.id) })
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MenuItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val bg = when {
        focused -> MaterialTheme.colorScheme.primary
        selected -> MaterialTheme.colorScheme.primaryContainer
        else -> androidx.compose.ui.graphics.Color.Transparent
    }
    val fg = when {
        focused -> MaterialTheme.colorScheme.onPrimary
        selected -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        label,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        color = fg,
        maxLines = 1,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .onPreviewKeyEvent { e ->
                if (e.type == KeyEventType.KeyDown &&
                    (e.key == Key.DirectionCenter || e.key == Key.Enter) &&
                    (e.nativeKeyEvent.isLongPress || e.nativeKeyEvent.repeatCount == 1)
                ) {
                    if (onLongClick != null) {
                        onLongClick()
                        true
                    } else false
                } else false
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

// ---- Search / loading / empty ------------------------------------------------------------------

/**
 * The search entry at the top of the Movies and Shows home. The rail's global search already covers
 * movies and series; this makes it reachable without leaving the tab. Focusable for d-pad on TV and
 * tappable on touch, it just opens the existing search screen.
 */
@Composable
private fun SearchAffordance(onOpenSearch: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (focused) MaterialTheme.colorScheme.primary
                else androidx.compose.ui.graphics.Color.Transparent,
            )
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onOpenSearch)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tint = if (focused) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant
        Icon(Icons.Filled.Search, contentDescription = null, tint = tint)
        Spacer(Modifier.width(12.dp))
        Text(
            stringResource(R.string.vod_search_hint),
            style = MaterialTheme.typography.titleMedium,
            color = tint,
        )
    }
}

@Composable
private fun EmptyVod(title: String, body: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LoadingVod(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text(message, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.vod_large_provider_note),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SortAffordance(current: SortOption, onNextSort: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (focused) MaterialTheme.colorScheme.primary
                else androidx.compose.ui.graphics.Color.Transparent,
            )
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onNextSort)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tint = if (focused) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant
        val text = when (current) {
            SortOption.DEFAULT -> stringResource(R.string.vod_sort_default)
            SortOption.RATING -> stringResource(R.string.vod_sort_rating)
            SortOption.YEAR -> stringResource(R.string.vod_sort_year)
            SortOption.NAME -> stringResource(R.string.vod_sort_name)
        }
        Text(text, style = MaterialTheme.typography.titleMedium, color = tint)
    }
}

/** Poster shelf card width; the grid uses an adaptive min size close to this. */
private val POSTER_WIDTH = 140.dp

/** Rating to one decimal place, locale-independent (the "★" is drawn beside it). */
internal fun formatRating(rating: Double): String = String.format(java.util.Locale.US, "%.1f", rating)
