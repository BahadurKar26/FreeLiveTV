package com.rancon.freelivetv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rancon.freelivetv.ChannelViewModel
import com.rancon.freelivetv.data.Channel
import com.rancon.freelivetv.data.Movie
import com.rancon.freelivetv.data.Series

@Composable
fun FavoritesScreen(
    viewModel: ChannelViewModel,
    onItemClick: (String, String, String) -> Unit
) {
    val favorites by viewModel.favorites.collectAsState()
    val movies by viewModel.movies.collectAsState()
    val seriesList by viewModel.series.collectAsState()
    
    var isEditMode by remember { mutableStateOf(false) }

    val favoriteMovies = remember(favorites, movies) {
        movies.filter { movie -> favorites.any { it.id == movie.id } }
    }
    val favoriteSeries = remember(favorites, seriesList) {
        seriesList.filter { s -> favorites.any { it.id == s.id } }
    }
    val favoriteChannels = remember(favorites) {
        // Filter out those that are movies or series based on some ID prefix or meta
        // For now, let's assume if it's in 'favorites' but not movies/series, it's a channel
        favorites.filter { fav -> 
            favoriteMovies.none { it.id == fav.id } && favoriteSeries.none { it.id == fav.id }
        }
    }

    val totalFavorites = favorites.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Header with edit button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "FAVORITES",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            var editFocused by remember { mutableStateOf(false) }
            if (totalFavorites > 0) {
                Button(
                    onClick = { isEditMode = !isEditMode },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (editFocused) Color.White else Color(0xFF2C2C2C)
                    ),
                    modifier = Modifier.onFocusChanged { editFocused = it.isFocused }
                ) {
                    Text(
                        text = if (isEditMode) "DONE" else "EDIT",
                        color = if (editFocused) Color.Black else Color.White
                    )
                }
            }
        }

        if (totalFavorites == 0) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "No favorites yet", color = Color.Gray, fontSize = 18.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Live TV section
                if (favoriteChannels.isNotEmpty()) {
                    item { FavoriteSectionHeader("LIVE TV CHANNELS") }
                    items(favoriteChannels) { channel ->
                        FavoriteItemRow(
                            title = channel.name,
                            subTitle = channel.category,
                            isEditMode = isEditMode,
                            onItemClick = { onItemClick("live", channel.id, channel.name) },
                            onRemoveClick = { viewModel.toggleFavorite(channel.id) }
                        )
                    }
                }

                // Movies section
                if (favoriteMovies.isNotEmpty()) {
                    item { FavoriteSectionHeader("MOVIES") }
                    items(favoriteMovies) { movie ->
                        FavoriteItemRow(
                            title = movie.title,
                            subTitle = "${movie.year} • ${movie.genre.firstOrNull() ?: ""}",
                            isEditMode = isEditMode,
                            onItemClick = { onItemClick("movie", movie.id, movie.title) },
                            onRemoveClick = { viewModel.toggleFavorite(movie.id) }
                        )
                    }
                }

                // Series section
                if (favoriteSeries.isNotEmpty()) {
                    item { FavoriteSectionHeader("SERIES") }
                    items(favoriteSeries) { s ->
                        FavoriteItemRow(
                            title = s.title,
                            subTitle = "${s.year} • ${s.genre.firstOrNull() ?: ""}",
                            isEditMode = isEditMode,
                            onItemClick = { onItemClick("series", s.id, s.title) },
                            onRemoveClick = { viewModel.toggleFavorite(s.id) }
                        )
                    }
                }

                // Clear all button
                if (isEditMode) {
                    item {
                        var clearFocused by remember { mutableStateOf(false) }
                        Button(
                            onClick = { viewModel.resetAllData() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (clearFocused) Color.White else Color.Red.copy(alpha = 0.8f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                                .onFocusChanged { clearFocused = it.isFocused }
                        ) {
                            Text(
                                text = "CLEAR ALL FAVORITES",
                                color = if (clearFocused) Color.Black else Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteSectionHeader(title: String) {
    Text(
        text = title,
        color = Color(0xFFE50914),
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 12.dp)
    )
}

@Composable
fun FavoriteItemRow(
    title: String,
    subTitle: String,
    isEditMode: Boolean,
    onItemClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        onClick = if (isEditMode) onRemoveClick else onItemClick,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused },
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) {
                if (isEditMode) Color.Red.copy(alpha = 0.2f) else Color.White
            } else Color(0xFF1A1A1A)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (isFocused && !isEditMode) Color.Black else Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = subTitle,
                    color = if (isFocused && !isEditMode) Color.DarkGray else Color.Gray,
                    fontSize = 12.sp
                )
            }
            
            if (isEditMode) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = Color.Red,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
