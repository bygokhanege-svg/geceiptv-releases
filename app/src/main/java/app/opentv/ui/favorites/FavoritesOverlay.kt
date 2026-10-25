package app.opentv.ui.favorites

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.opentv.R
import app.opentv.data.model.Movie
import app.opentv.data.model.Series
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

enum class FavTab {
    ALL, MOVIES, SERIES
}

@Composable
fun FavoritesDialog(
    favouriteMovies: List<Movie>,
    favouriteSeries: List<Series>,
    onOpenMovie: (Movie) -> Unit,
    onOpenSeries: (Series) -> Unit,
    onToggleMovieFavourite: (Movie) -> Unit,
    onToggleSeriesFavourite: (Series) -> Unit,
    onDismiss: () -> Unit,
    isTelevision: Boolean = true,
) {
    BackHandler(onBack = onDismiss)

    var selectedTab by remember { mutableStateOf(FavTab.ALL) }
    val totalCount = favouriteMovies.size + favouriteSeries.size
    val firstTabFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(120)
        runCatching { firstTabFocus.requestFocus() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.88f))
                .then(
                    if (!isTelevision) Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ) else Modifier
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize(if (isTelevision) 0.94f else 0.98f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF0F172A), Color(0xFF0A0F1D))
                        )
                    )
                    .border(
                        1.5.dp,
                        Brush.linearGradient(
                            listOf(
                                Color(0xFFF59E0B).copy(alpha = 0.5f),
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
                            .padding(bottom = 14.dp),
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
                                            listOf(Color(0xFFF59E0B), Color(0xFFD97706))
                                        )
                                    )
                                    .shadow(8.dp, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp),
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = stringResource(R.string.favorites_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                                Text(
                                    text = "${favouriteMovies.size} Film • ${favouriteSeries.size} Dizi",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8),
                                )
                            }
                        }

                        // Close button
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
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = if (closeFocused) MaterialTheme.colorScheme.onPrimary else Color.White,
                            )
                        }
                    }

                    // Categorical Tabs (Tümü, Filmler, Diziler)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        FavTabButton(
                            title = stringResource(R.string.favorites_all, totalCount),
                            icon = Icons.Filled.Star,
                            selected = selectedTab == FavTab.ALL,
                            accentColor = Color(0xFFF59E0B),
                            focusRequester = firstTabFocus,
                            onClick = { selectedTab = FavTab.ALL },
                        )
                        FavTabButton(
                            title = stringResource(R.string.favorites_movies, favouriteMovies.size),
                            icon = Icons.Filled.Movie,
                            selected = selectedTab == FavTab.MOVIES,
                            accentColor = Color(0xFF38BDF8),
                            onClick = { selectedTab = FavTab.MOVIES },
                        )
                        FavTabButton(
                            title = stringResource(R.string.favorites_series, favouriteSeries.size),
                            icon = Icons.Filled.VideoLibrary,
                            selected = selectedTab == FavTab.SERIES,
                            accentColor = Color(0xFFA855F7),
                            onClick = { selectedTab = FavTab.SERIES },
                        )
                    }

                    // Content Area
                    if (totalCount == 0) {
                        // Empty State
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(32.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF59E0B).copy(alpha = 0.12f))
                                        .border(1.5.dp, Color(0xFFF59E0B).copy(alpha = 0.4f), CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.StarBorder,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(44.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.height(18.dp))
                                Text(
                                    text = stringResource(R.string.favorites_empty_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.favorites_empty_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF94A3B8),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(0.7f),
                                )
                            }
                        }
                    } else {
                        when (selectedTab) {
                            FavTab.ALL -> {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(20.dp),
                                    contentPadding = PaddingValues(bottom = 16.dp),
                                ) {
                                    if (favouriteMovies.isNotEmpty()) {
                                        item {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(bottom = 8.dp),
                                            ) {
                                                Text("🎬", fontSize = 18.sp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = stringResource(R.string.favorites_movies, favouriteMovies.size),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                )
                                            }
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                contentPadding = PaddingValues(vertical = 4.dp),
                                            ) {
                                                items(favouriteMovies, key = { "m_${it.id}" }) { movie ->
                                                    MovieFavCard(
                                                        movie = movie,
                                                        isTelevision = isTelevision,
                                                        onClick = { onOpenMovie(movie) },
                                                        onToggleFav = { onToggleMovieFavourite(movie) },
                                                        modifier = Modifier.width(if (isTelevision) 150.dp else 120.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (favouriteSeries.isNotEmpty()) {
                                        item {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(bottom = 8.dp),
                                            ) {
                                                Text("🍿", fontSize = 18.sp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = stringResource(R.string.favorites_series, favouriteSeries.size),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                )
                                            }
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                contentPadding = PaddingValues(vertical = 4.dp),
                                            ) {
                                                items(favouriteSeries, key = { "s_${it.id}" }) { series ->
                                                    SeriesFavCard(
                                                        series = series,
                                                        isTelevision = isTelevision,
                                                        onClick = { onOpenSeries(series) },
                                                        onToggleFav = { onToggleSeriesFavourite(series) },
                                                        modifier = Modifier.width(if (isTelevision) 150.dp else 120.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            FavTab.MOVIES -> {
                                if (favouriteMovies.isEmpty()) {
                                    EmptySubTab(stringResource(R.string.dashboard_empty_movies))
                                } else {
                                    val columns = if (isTelevision) 5 else 3
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(columns),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp),
                                        contentPadding = PaddingValues(bottom = 16.dp),
                                    ) {
                                        items(favouriteMovies, key = { "m_grid_${it.id}" }) { movie ->
                                            MovieFavCard(
                                                movie = movie,
                                                isTelevision = isTelevision,
                                                onClick = { onOpenMovie(movie) },
                                                onToggleFav = { onToggleMovieFavourite(movie) },
                                            )
                                        }
                                    }
                                }
                            }

                            FavTab.SERIES -> {
                                if (favouriteSeries.isEmpty()) {
                                    EmptySubTab(stringResource(R.string.dashboard_empty_series))
                                } else {
                                    val columns = if (isTelevision) 5 else 3
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(columns),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp),
                                        contentPadding = PaddingValues(bottom = 16.dp),
                                    ) {
                                        items(favouriteSeries, key = { "s_grid_${it.id}" }) { series ->
                                            SeriesFavCard(
                                                series = series,
                                                isTelevision = isTelevision,
                                                onClick = { onOpenSeries(series) },
                                                onToggleFav = { onToggleSeriesFavourite(series) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FavTabButton(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    accentColor: Color,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        animationSpec = tween(durationMillis = 150),
        label = "tabScale",
    )

    Row(
        modifier = Modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isFocused) accentColor
                else if (selected) accentColor.copy(alpha = 0.22f)
                else Color.White.copy(alpha = 0.05f)
            )
            .border(
                width = if (isFocused) 2.dp else if (selected) 1.5.dp else 1.dp,
                color = if (isFocused) Color.White else if (selected) accentColor.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected || isFocused) accentColor else Color(0xFF94A3B8),
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected || isFocused) FontWeight.Bold else FontWeight.Medium,
            color = if (selected || isFocused) Color.White else Color(0xFFCBD5E1),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MovieFavCard(
    movie: Movie,
    isTelevision: Boolean,
    onClick: () -> Unit,
    onToggleFav: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1.0f,
        label = "movieScale",
    )

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E293B).copy(alpha = 0.7f))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) Color(0xFFF59E0B) else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onClick)
            .focusable(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.68f)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
        ) {
            AsyncImage(
                model = movie.posterUrl,
                contentDescription = movie.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F172A)),
            )

            // Rating Badge (Top-Start)
            if (movie.rating != null && movie.rating > 0.0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "⭐ ${String.format("%.1f", movie.rating)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFBBF24),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                    )
                }
            }

            // Star Remove Button (Top-End)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleFav,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = stringResource(R.string.favorites_unfavorite_tooltip),
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        // Title and Year Info
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = movie.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (movie.year != null && movie.year > 0) {
                Text(
                    text = "${movie.year}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                )
            }
        }
    }
}

@Composable
private fun SeriesFavCard(
    series: Series,
    isTelevision: Boolean,
    onClick: () -> Unit,
    onToggleFav: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1.0f,
        label = "seriesScale",
    )

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E293B).copy(alpha = 0.7f))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) Color(0xFFF59E0B) else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onClick)
            .focusable(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.68f)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
        ) {
            AsyncImage(
                model = series.posterUrl,
                contentDescription = series.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F172A)),
            )

            // Rating Badge (Top-Start)
            if (series.rating != null && series.rating > 0.0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "⭐ ${String.format("%.1f", series.rating)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFBBF24),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                    )
                }
            }

            // Star Remove Button (Top-End)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleFav,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = stringResource(R.string.favorites_unfavorite_tooltip),
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        // Title and Year Info
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = series.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (series.year != null && series.year > 0) {
                Text(
                    text = "${series.year}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                )
            }
        }
    }
}

@Composable
private fun EmptySubTab(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF94A3B8),
        )
    }
}
