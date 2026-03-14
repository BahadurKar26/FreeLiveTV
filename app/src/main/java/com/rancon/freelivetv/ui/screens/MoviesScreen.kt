package com.rancon.freelivetv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rancon.freelivetv.ChannelViewModel
import com.rancon.freelivetv.R
import com.rancon.freelivetv.data.Movie
import com.rancon.freelivetv.ui.components.CategoryFilterChips
import com.rancon.freelivetv.ui.components.MovieCard
import com.rancon.freelivetv.ui.components.FeaturedMovieCard
import com.rancon.freelivetv.ui.components.ShimmerRow
import com.rancon.freelivetv.ui.theme.*

@Composable
fun MoviesScreen(
    viewModel: ChannelViewModel,
    onMovieClick: (String, String) -> Unit
) {
    var selectedGenre by remember { mutableStateOf("All") }
    val movies by viewModel.movies.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    val genres = remember(movies) {
        listOf("All") + movies.flatMap { it.genre }.distinct().sorted()
    }

    val filteredMovies = remember(selectedGenre, movies) {
        if (selectedGenre == "All") movies 
        else movies.filter { it.genre.contains(selectedGenre) }
    }

    if (isLoading && movies.isEmpty()) {
        Column(modifier = Modifier.fillMaxSize().background(Black)) {
            ShimmerRow()
            ShimmerRow()
            ShimmerRow()
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Black),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Sticky Header with Filters
            item {
                Column(modifier = Modifier.fillMaxWidth().background(Black)) {
                    Text(
                        text = stringResource(R.string.nav_movies),
                        color = White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                    CategoryFilterChips(
                        categories = genres,
                        selectedCategory = selectedGenre,
                        onCategorySelected = { selectedGenre = it }
                    )
                }
            }

            // Featured Movie (P2-001)
            if (movies.isNotEmpty()) {
                item {
                    FeaturedMovieCard(
                        movie = movies.first(),
                        onPlayClick = { onMovieClick(movies.first().id, movies.first().title) },
                        onFavoriteClick = { /* Handled in Details if added */ }
                    )
                }
            }

            // Trending Movies Row
            item {
                MovieSectionRow(
                    title = "TRENDING MOVIES",
                    movies = movies.sortedByDescending { it.rating }.take(10),
                    onMovieClick = onMovieClick
                )
            }

            // New Releases Row
            item {
                MovieSectionRow(
                    title = "NEW RELEASES",
                    movies = movies.filter { it.isNew }.take(10),
                    onMovieClick = onMovieClick
                )
            }

            // Genre Specific Row based on selection
            if (selectedGenre != "All") {
                item {
                    MovieSectionRow(
                        title = "POPULAR IN $selectedGenre",
                        movies = filteredMovies,
                        onMovieClick = onMovieClick
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun MovieSectionRow(
    title: String,
    movies: List<Movie>,
    onMovieClick: (String, String) -> Unit
) {
    if (movies.isEmpty()) return
    
    Column {
        Text(
            text = title,
            color = White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(movies) { movie ->
                MovieCard(
                    movie = movie,
                    onClick = { onMovieClick(movie.id, movie.title) }
                )
            }
        }
    }
}
