package com.rancon.freelivetv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rancon.freelivetv.ChannelViewModel
import com.rancon.freelivetv.data.Channel
import com.rancon.freelivetv.ui.components.ChannelRow

@Composable
fun RanconHomeScreen(
    viewModel: ChannelViewModel,
    banglaChannels: List<Channel>,
    globalChannels: List<Channel>,
    onChannelClick: (String, String) -> Unit
) {
    val listState = rememberLazyListState()

    // Use UI-optimized states from ViewModel (QC-009)
    val mostWatchedChannels by viewModel.homeMostWatched.collectAsState()
    val recommendedChannels by viewModel.homeRecommended.collectAsState()

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (mostWatchedChannels.isNotEmpty()) {
            item(key = "home_most_watched") {
                ChannelRow(
                    title = "MOST WATCHED (${mostWatchedChannels.size})",
                    channels = mostWatchedChannels,
                    isLarge = true,
                    onChannelClick = onChannelClick
                )
            }
        }

        if (banglaChannels.isNotEmpty()) {
            item(key = "home_bangla") {
                ChannelRow(
                    title = "BANGLA CHANNELS (${banglaChannels.size})",
                    channels = banglaChannels,
                    isLarge = false,
                    onChannelClick = onChannelClick
                )
            }
        }

        if (globalChannels.isNotEmpty()) {
            item(key = "home_global") {
                ChannelRow(
                    title = "GLOBAL CHANNELS (${globalChannels.size})",
                    channels = globalChannels,
                    isLarge = false,
                    onChannelClick = onChannelClick
                )
            }
        }

        if (recommendedChannels.isNotEmpty()) {
            item(key = "home_recommended") {
                ChannelRow(
                    title = "RECOMMENDED FOR YOU (${recommendedChannels.size})",
                    channels = recommendedChannels,
                    isLarge = false,
                    onChannelClick = onChannelClick
                )
            }
        }
    }
}
