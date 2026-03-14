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
import com.rancon.freelivetv.data.Series
import com.rancon.freelivetv.ui.components.CategoryFilterChips
import com.rancon.freelivetv.ui.components.SeriesCard
import com.rancon.freelivetv.ui.components.ShimmerRow
import com.rancon.freelivetv.ui.theme.*

@Composable
fun SeriesScreen(
    viewModel: ChannelViewModel,
    onSeriesClick: (String, String) -> Unit
) {
    var selectedGenre by remember { mutableStateOf("All") }
    val seriesList by viewModel.series.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    val genres = remember(seriesList) {
        listOf("All") + seriesList.flatMap { it.genre }.distinct().sorted()
    }

    val filteredSeries = remember(selectedGenre, seriesList) {
        if (selectedGenre == "All") seriesList 
        else seriesList.filter { it.genre.contains(selectedGenre) }
    }

    if (isLoading && seriesList.isEmpty()) {
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
                        text = stringResource(R.string.nav_series),
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

            // Popular Series Row
            item {
                SeriesSectionRow(
                    title = "POPULAR SERIES",
                    seriesList = seriesList.sortedByDescending { it.rating }.take(10),
                    onSeriesClick = onSeriesClick
                )
            }

            // New Episodes Row
            item {
                SeriesSectionRow(
                    title = "RECENTLY ADDED",
                    seriesList = seriesList.sortedByDescending { it.year }.take(10),
                    onSeriesClick = onSeriesClick
                )
            }

            // Genre Specific Row based on selection
            if (selectedGenre != "All") {
                item {
                    SeriesSectionRow(
                        title = "POPULAR IN $selectedGenre",
                        seriesList = filteredSeries,
                        onSeriesClick = onSeriesClick
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun SeriesSectionRow(
    title: String,
    seriesList: List<Series>,
    onSeriesClick: (String, String) -> Unit
) {
    if (seriesList.isEmpty()) return
    
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
            items(seriesList) { series ->
                SeriesCard(
                    series = series,
                    onClick = { onSeriesClick(series.id, series.title) }
                )
            }
        }
    }
}
