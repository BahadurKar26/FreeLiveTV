package com.rancon.freelivetv.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rancon.freelivetv.data.Channel

@Composable
fun ChannelRow(
    title: String = "",
    channels: List<Channel>,
    isLarge: Boolean = false,
    onChannelClick: (String, String) -> Unit
) {
    val uniqueChannels = remember(channels) { channels.distinctBy { channel -> channel.id } }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(uniqueChannels) {
        if (uniqueChannels.isNotEmpty()) visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 500))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            if (title.isNotEmpty()) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = if (isLarge) 20.sp else 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(
                    items = uniqueChannels,
                    key = { channel -> channel.id }
                ) { channel ->
                    ChannelCard(
                        channel = channel,
                        onClick = { onChannelClick(channel.id, channel.name) }
                    )
                }
            }
        }
    }
}
