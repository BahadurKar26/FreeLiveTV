package com.rancon.freelivetv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rancon.freelivetv.ChannelViewModel
import com.rancon.freelivetv.R
import com.rancon.freelivetv.data.Channel
import com.rancon.freelivetv.ui.components.CategoryFilterChips
import com.rancon.freelivetv.ui.components.EnhancedChannelCard
import com.rancon.freelivetv.ui.theme.*

@Composable
fun LiveTVScreen(
    viewModel: ChannelViewModel,
    onChannelClick: (String, String) -> Unit
) {
    val channels by viewModel.channels.collectAsState()
    var selectedCategory by remember { mutableStateOf("All") }
    
    val activeChannels = remember(channels) {
        channels.filter { it.healthStatus == "ACTIVE" }
    }

    val categories = remember(activeChannels) {
        listOf("All") + activeChannels.map { it.category }.distinct().sorted()
    }
    
    val filteredChannels = remember(selectedCategory, activeChannels) {
        if (selectedCategory == "All") activeChannels 
        else activeChannels.filter { it.category == selectedCategory }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
    ) {
        Surface(
            color = Black,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = stringResource(R.string.nav_live_tv),
                    color = White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
                
                CategoryFilterChips(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it }
                )
            }
        }

        if (filteredChannels.isEmpty() && viewModel.isLoading.collectAsState().value) {
            com.rancon.freelivetv.ui.components.ShimmerGrid()
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredChannels) { channel ->
                    EnhancedChannelCard(
                        channel = channel,
                        viewModel = viewModel,
                        onClick = { onChannelClick(channel.id, channel.name) }
                    )
                }
            }
        }
    }
}
