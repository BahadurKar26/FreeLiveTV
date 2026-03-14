package com.rancon.freelivetv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.rancon.freelivetv.ChannelViewModel
import com.rancon.freelivetv.data.Channel
import com.rancon.freelivetv.data.Program

@Composable
fun EnhancedChannelCard(
    channel: Channel,
    viewModel: ChannelViewModel? = null,
    showLiveIndicator: Boolean = false,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val currentProgram by if (viewModel != null) {
        viewModel.getCurrentProgram(channel.id).collectAsState(initial = null)
    } else {
        remember { mutableStateOf(null) }
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .width(160.dp)
            .height(120.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .padding(4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) Color.White.copy(alpha = 0.2f) else Color(0xFF1A1A1A)
        ),
        border = if (isFocused) BorderStroke(2.dp, Color.White) else null
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Logo/Image area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (channel.logo.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(channel.logo)
                            .crossfade(true)
                            .build(),
                        contentDescription = channel.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                Color(0xFF2C2C2C),
                                shape = RoundedCornerShape(4.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = channel.name.take(2).uppercase(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE50914)
                        )
                    }
                }

                // Live indicator
                if (showLiveIndicator && (currentProgram != null || channel.healthStatus == "ACTIVE")) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .size(8.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = if (channel.healthStatus == "ACTIVE") Color.Green else Color.Red
                    ) {}
                }
            }

            // Channel info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = channel.name,
                    color = if (isFocused) Color.Yellow else Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier.padding(bottom = 2.dp)
                )

                if (currentProgram != null) {
                    Text(
                        text = "NOW: ${currentProgram!!.title}",
                        color = Color.Green,
                        fontSize = 9.sp,
                        maxLines = 1,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = channel.category,
                        color = Color.Gray,
                        fontSize = 9.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
