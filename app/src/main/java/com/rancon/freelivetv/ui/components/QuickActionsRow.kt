package com.rancon.freelivetv.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun QuickActionsRow(
    onLastWatchedClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onContinueWatchingClick: () -> Unit,
    onGuideClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickActionButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.History,
            label = "LAST WATCHED",
            onClick = onLastWatchedClick
        )

        QuickActionButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Star,
            label = "FAVORITES",
            onClick = onFavoritesClick
        )

        QuickActionButton(
            modifier = Modifier.weight(1f),
            icon = Icons.AutoMirrored.Filled.PlaylistPlay,
            label = "CONTINUE",
            onClick = onContinueWatchingClick
        )

        QuickActionButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Tv,
            label = "TV GUIDE",
            onClick = onGuideClick
        )
    }
}

@Composable
fun QuickActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused },
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) Color.White else Color(0xFF2C2C2C)
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isFocused) Color.Black else Color(0xFFE50914),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = if (isFocused) Color.Black else Color.Gray
            )
        }
    }
}
