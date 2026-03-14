package com.rancon.freelivetv.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rancon.freelivetv.ChannelViewModel
import com.rancon.freelivetv.PlayerActivity
import com.rancon.freelivetv.R
import com.rancon.freelivetv.ui.theme.*

@Composable
fun PlayerScreen(
    channelId: String,
    channelName: String,
    viewModel: ChannelViewModel,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val channels by viewModel.channels.collectAsState()

    LaunchedEffect(Unit) {
        val intent = Intent(context, PlayerActivity::class.java).apply {
            putExtra("channel_id", channelId)
            putExtra("channel_name", channelName)

            channels.find { it.id == channelId }?.let { channel ->
                if (channel.urls.isNotEmpty()) {
                    putExtra("channel_url", channel.urls[0])
                }
                if (channel.urls.size > 1) {
                    putExtra("backup_url", channel.urls[1])
                }
            }
        }
        context.startActivity(intent)
        onBackPressed()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Black),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Red)
        Text(
            text = stringResource(R.string.loading_channels),
            color = White,
            fontSize = 16.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)
        )
    }
}
