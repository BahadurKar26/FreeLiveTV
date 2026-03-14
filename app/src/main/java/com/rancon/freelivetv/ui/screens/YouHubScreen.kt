package com.rancon.freelivetv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rancon.freelivetv.ChannelViewModel
import com.rancon.freelivetv.R
import com.rancon.freelivetv.data.Channel
import com.rancon.freelivetv.ui.theme.*

@Composable
fun YouHubScreen(
    viewModel: ChannelViewModel,
    inactiveChannels: List<Channel>,
    onNavigateToFavorites: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Black),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text(
                text = "Welcome back, Guest",
                color = White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                YouStatCard(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.title_favorites),
                    icon = Icons.Default.Star,
                    onClick = onNavigateToFavorites
                )
                YouStatCard(
                    modifier = Modifier.weight(1f),
                    label = "HISTORY",
                    icon = Icons.Default.History,
                    onClick = { /* Navigate to History */ }
                )
                YouStatCard(
                    modifier = Modifier.weight(1f),
                    label = "SETTINGS",
                    icon = Icons.Default.Settings,
                    onClick = onNavigateToSettings
                )
            }
        }

        if (inactiveChannels.isNotEmpty()) {
            item {
                Text(
                    text = "INACTIVE CHANNELS",
                    color = White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkGray),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Channels failing to play are moved here.",
                            color = LightGray,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        inactiveChannels.take(5).forEach { channel ->
                            Text(
                                text = channel.name,
                                color = White,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        
                        if (inactiveChannels.size > 5) {
                            Text(
                                text = "And ${inactiveChannels.size - 5} more...",
                                color = Red,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun YouStatCard(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused },
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) White else DarkGray
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isFocused) Black else Red,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = label,
                color = if (isFocused) Black else LightGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
