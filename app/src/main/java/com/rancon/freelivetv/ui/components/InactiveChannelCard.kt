package com.rancon.freelivetv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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

@Composable
fun InactiveChannelCard(
    channel: Channel,
    viewModel: ChannelViewModel
) {
    var showDetails by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    Card(
        onClick = { showDetails = true },
        modifier = Modifier
            .width(80.dp)
            .height(100.dp)
            .onFocusChanged { isFocused = it.isFocused },
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) Color.White.copy(alpha = 0.1f) else Color(0xFF2C2C2C).copy(alpha = 0.5f)
        ),
        border = if (isFocused) BorderStroke(1.dp, Color.Gray) else null
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (channel.logo.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(channel.logo)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(50.dp)
                        .alpha(0.5f),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(
                    text = channel.name.take(2).uppercase(),
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
            
            // Show countdown badge if it's close to removal
            val daysLeft = channel.daysRemaining()
            if (daysLeft <= 3) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                    color = Color.Red,
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text(
                        text = "${daysLeft}d",
                        color = Color.White,
                        fontSize = 8.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }

    if (showDetails) {
        AlertDialog(
            onDismissRequest = { showDetails = false },
            title = {
                Text(
                    text = channel.name,
                    color = Color.White
                )
            },
            text = {
                Column {
                    Text(
                        text = "Status: Currently Offline",
                        color = Color.Red,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Failures: ${viewModel.getFailureCount(channel.id)} attempts",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val daysLeft = channel.daysRemaining()
                    Text(
                        text = "Removal in: $daysLeft days",
                        color = if (daysLeft <= 2) Color.Yellow else Color.Gray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Inactive channels are automatically removed after 7 days to keep the list clean.",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetails = false }) {
                    Text("OK", color = Color(0xFFE50914))
                }
            },
            containerColor = Color(0xFF1A1A1A)
        )
    }
}
