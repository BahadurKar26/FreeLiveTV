package com.rancon.freelivetv.ui.components

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
import com.rancon.freelivetv.data.Channel

@Composable
fun LiveChannelRow(
    channel: Channel,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged {
                isFocused = it.isFocused
                showPreview = it.isFocused
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) Color(0xFF2C2C2C) else Color(0xFF1A1A1A)
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Channel logo
                Box(
                    modifier = Modifier.size(40.dp)
                ) {
                    if (channel.logo.isNotEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(channel.logo)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF2C2C2C), RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = channel.name.take(2).uppercase(),
                                fontSize = 14.sp,
                                color = Color(0xFFE50914)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Channel info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = channel.name,
                        color = if (isFocused) Color.Yellow else Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${channel.category} • ${channel.region}",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }

                // Live indicator
                Surface(
                    modifier = Modifier.size(8.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Red
                ) {}
            }

            // Preview (shown on focus)
            if (showPreview) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    color = Color(0xFF2C2C2C),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Now Playing: Live Program",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Current program description would appear here",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                        )
                        Button(
                            onClick = onClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE50914)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("WATCH NOW")
                        }
                    }
                }
            }
        }
    }
}
