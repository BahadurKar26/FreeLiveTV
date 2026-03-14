package com.rancon.freelivetv.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rancon.freelivetv.data.Channel

@Composable
fun LiveNowRow(
    channels: List<Channel>,
    onChannelClick: (String, String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "LIVE NOW ●●●",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(
                items = channels,
                key = { it.id }
            ) { channel ->
                EnhancedChannelCard(
                    channel = channel,
                    showLiveIndicator = true,
                    onClick = { onChannelClick(channel.id, channel.name) }
                )
            }
        }
    }
}

@Composable
fun TrendingRow(
    channels: List<Channel>,
    onChannelClick: (String, String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "TRENDING ON FREELIVETV",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(
                items = channels,
                key = { it.id }
            ) { channel ->
                EnhancedChannelCard(
                    channel = channel,
                    showLiveIndicator = false,
                    onClick = { onChannelClick(channel.id, channel.name) }
                )
            }
        }
    }
}