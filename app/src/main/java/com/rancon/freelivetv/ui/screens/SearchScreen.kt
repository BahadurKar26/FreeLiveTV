package com.rancon.freelivetv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rancon.freelivetv.ChannelViewModel
import com.rancon.freelivetv.R
import com.rancon.freelivetv.data.Channel
import com.rancon.freelivetv.data.Movie
import com.rancon.freelivetv.data.Series
import com.rancon.freelivetv.ui.components.CategoryFilterChips
import com.rancon.freelivetv.ui.components.ChannelCard
import com.rancon.freelivetv.ui.components.MovieCard
import com.rancon.freelivetv.ui.components.SeriesCard
import com.rancon.freelivetv.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SearchScreen(
    viewModel: ChannelViewModel,
    onItemClick: (String, String, String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val channels by viewModel.channels.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    
    var filteredChannels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var filteredMovies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var filteredSeries by remember { mutableStateOf<List<Series>>(emptyList()) }
    
    var selectedType by remember { mutableStateOf("All") }
    val typeFilters = listOf("All", "Channels", "Movies", "Series")

    LaunchedEffect(query) {
        if (query.length >= 2) {
            delay(300) // Debounce (P1-019)
            filteredChannels = channels.filter { 
                it.name.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true) 
            }
            filteredMovies = viewModel.searchMovies(query)
            filteredSeries = viewModel.searchSeries(query)
        } else {
            filteredChannels = emptyList()
            filteredMovies = emptyList()
            filteredSeries = emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.search_hint), color = Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Red) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = White,
                unfocusedTextColor = White,
                cursorColor = Red,
                focusedBorderColor = Red,
                unfocusedBorderColor = Gray
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (query.isEmpty()) {
            // Recent Searches (P1-024)
            if (recentSearches.isNotEmpty()) {
                Text(
                    "RECENT SEARCHES",
                    color = LightGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    items(recentSearches) { search ->
                        AssistChip(
                            onClick = { query = search },
                            label = { Text(search) },
                            leadingIcon = { Icon(Icons.Default.History, null, modifier = Modifier.size(16.dp)) },
                            colors = AssistChipDefaults.assistChipColors(labelColor = White, leadingIconContentColor = Red)
                        )
                    }
                }
            }

            Text(
                stringResource(R.string.title_most_watched),
                color = White,
                fontSize = 18.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            val mostWatched = viewModel.getMostWatchedChannels()
            LazyVerticalGrid(
                columns = GridCells.Adaptive(120.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(mostWatched) { channel ->
                    ChannelCard(
                        channel = channel,
                        onClick = { onItemClick("live", channel.id, channel.name) }
                    )
                }
            }
        } else {
            // Type Filters (P1-021)
            CategoryFilterChips(
                categories = typeFilters,
                selectedCategory = selectedType,
                onCategorySelected = { selectedType = it }
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(120.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Channels Section
                if (filteredChannels.isNotEmpty() && (selectedType == "All" || selectedType == "Channels")) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text("Live Channels", color = Red, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    }
                    items(filteredChannels) { channel ->
                        ChannelCard(
                            channel = channel,
                            onClick = { onItemClick("live", channel.id, channel.name) }
                        )
                    }
                }

                // Movies Section
                if (filteredMovies.isNotEmpty() && (selectedType == "All" || selectedType == "Movies")) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text("Movies", color = Red, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
                    }
                    items(filteredMovies) { movie ->
                        MovieCard(
                            movie = movie,
                            onClick = { onItemClick("movie", movie.id, movie.title) }
                        )
                    }
                }

                // Series Section
                if (filteredSeries.isNotEmpty() && (selectedType == "All" || selectedType == "Series")) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text("Series", color = Red, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
                    }
                    items(filteredSeries) { series ->
                        SeriesCard(
                            series = series,
                            onClick = { onItemClick("series", series.id, series.title) }
                        )
                    }
                }
                
                if (filteredChannels.isEmpty() && filteredMovies.isEmpty() && filteredSeries.isEmpty() && query.length >= 2) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.no_results, query), color = Gray)
                        }
                    }
                }
            }
        }
    }
}
