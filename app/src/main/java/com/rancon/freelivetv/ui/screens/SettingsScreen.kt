package com.rancon.freelivetv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rancon.freelivetv.ChannelViewModel
import com.rancon.freelivetv.data.LocalNetworkScanner
import com.rancon.freelivetv.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: ChannelViewModel,
    onRefreshClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val networkType by viewModel.networkType.collectAsState()
    var isScanning by remember { mutableStateOf(false) }
    val discoveredServers = remember { mutableStateListOf<String>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .padding(32.dp)
    ) {
        Text(
            text = "SETTINGS",
            color = White,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                SettingsButton(
                    icon = Icons.Default.Refresh,
                    title = "Sync Content",
                    description = "Force refresh channels, movies, and series",
                    onClick = onRefreshClick
                )
            }

            // Phase 10.12: Manual BDIX Scan
            item {
                SettingsButton(
                    icon = Icons.Default.Search,
                    title = if (isScanning) "Scanning Network..." else "Scan for BDIX Servers",
                    description = "Discover ultra-fast local media servers on your ISP",
                    enabled = !isScanning && networkType == "Wi-Fi",
                    onClick = {
                        isScanning = true
                        discoveredServers.clear()
                        val scanner = LocalNetworkScanner(viewModel.getApplication())
                        scanner.startScan { server ->
                            discoveredServers.add(server)
                        }
                        scope.launch {
                            kotlinx.coroutines.delay(10000) // UI limit for scan
                            isScanning = false
                        }
                    }
                )
            }

            if (discoveredServers.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Discovered Servers:", color = Red, style = MaterialTheme.typography.titleMedium)
                        discoveredServers.forEach { server ->
                            Text(text = "• $server", color = LightGray, fontSize = 12.sp)
                        }
                    }
                }
            }

            item {
                HorizontalDivider(color = Gray.copy(alpha = 0.3f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Network: $networkType", color = if (networkType == "Wi-Fi") Red else White)
                Text(text = "App Version: 2.2-QC-Final", color = Gray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun SettingsButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused },
        color = if (isFocused) White.copy(alpha = 0.1f) else Color.Transparent,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        border = if (isFocused) androidx.compose.foundation.BorderStroke(2.dp, Red) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) (if (isFocused) Red else White) else Gray,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    color = if (enabled) White else Gray,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = description,
                    color = Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
