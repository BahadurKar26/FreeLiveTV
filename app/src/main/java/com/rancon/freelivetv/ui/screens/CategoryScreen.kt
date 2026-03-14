package com.rancon.freelivetv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rancon.freelivetv.data.Channel
import com.rancon.freelivetv.ui.components.ChannelRow

@Composable
fun CategoryScreen(
    channels: List<Channel>,
    onChannelClick: (String, String) -> Unit
) {
    val uniqueChannels = remember(channels) { channels.distinctBy { channel -> channel.id } }

    if (uniqueChannels.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No channels available",
                color = Color.Gray,
                fontSize = 18.sp
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            item(key = "bangla_header") {
                Text(
                    text = "BANGLA (${uniqueChannels.size} channels)",
                    color = Color(0xFFE50914),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
            }

            val groupedByCategory = uniqueChannels.groupBy { channel -> channel.category }
            groupedByCategory.forEach { (category, categoryChannels) ->
                item(key = "category_$category") {
                    ChannelRow(
                        title = "$category (${categoryChannels.size} channels)",
                        channels = categoryChannels,
                        isLarge = false,
                        onChannelClick = onChannelClick
                    )
                }
            }
        }
    }
}