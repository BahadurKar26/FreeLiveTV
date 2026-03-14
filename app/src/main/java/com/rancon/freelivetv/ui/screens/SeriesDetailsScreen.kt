package com.rancon.freelivetv.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.rancon.freelivetv.ChannelViewModel
import com.rancon.freelivetv.PlayerActivity
import com.rancon.freelivetv.R
import com.rancon.freelivetv.data.Episode
import com.rancon.freelivetv.data.Series
import com.rancon.freelivetv.ui.theme.*
import androidx.media3.common.util.UnstableApi

@UnstableApi
@Composable
fun SeriesDetailsScreen(
    seriesId: String,
    seriesTitle: String,
    viewModel: ChannelViewModel,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val series = viewModel.getSeriesById(seriesId)
    val continueWatching by viewModel.continueWatching.collectAsState()
    
    // Review 303: Dynamic Resume logic
    val lastEpisode = continueWatching.find { it.id.startsWith("ep_${seriesId}") }
    var expandedSeason by remember { mutableIntStateOf(series?.seasons?.firstOrNull()?.number ?: 1) }

    if (series == null) {
        Box(modifier = Modifier.fillMaxSize().background(Black), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Red)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Black),
        contentPadding = PaddingValues(bottom = 48.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackPressed) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = White)
                }

                val isFavorite = viewModel.isFavorite(seriesId)
                IconButton(onClick = { viewModel.toggleFavorite(seriesId, "series") }) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Yellow else LightGray
                    )
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Card(
                    modifier = Modifier.width(240.dp).height(360.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, White.copy(alpha = 0.1f))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(series.posterUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = series.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Column(modifier = Modifier.padding(start = 32.dp).weight(1f)) {
                    Text(text = series.title, color = White, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                    
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 16.dp)) {
                        Surface(color = Gray.copy(alpha = 0.3f), shape = RoundedCornerShape(4.dp)) {
                            Text(text = " ${series.rating}/10 ", color = Yellow, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = series.getYearRange(), color = LightGray, fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = series.genre.take(2).joinToString(" • "), color = LightGray, fontSize = 18.sp)
                    }

                    Text(
                        text = series.description,
                        color = White.copy(alpha = 0.8f),
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        modifier = Modifier.padding(top = 24.dp),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    var playFocused by remember { mutableStateOf(false) }
                    Button(
                        onClick = { 
                            val targetEpisode = if (lastEpisode != null) {
                                // Find actual episode object for resume
                                series.seasons.flatMap { it.episodes }.find { it.id == lastEpisode.id }
                            } else {
                                series.seasons.firstOrNull()?.episodes?.firstOrNull()
                            }
                            
                            targetEpisode?.let { ep ->
                                val intent = Intent(context, PlayerActivity::class.java).apply {
                                    putExtra("channel_id", ep.id)
                                    putExtra("channel_name", "${series.title} - ${ep.getEpisodeDisplay()}")
                                    putExtra("channel_url", ep.streamUrl)
                                }
                                context.startActivity(intent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (playFocused) White else Red),
                        modifier = Modifier.onFocusChanged { playFocused = it.isFocused }.width(240.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = if (playFocused) Black else White)
                        Text(
                            text = if (lastEpisode != null) " RESUME WATCHING" else " WATCH S1:E1", 
                            color = if (playFocused) Black else White, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Text(text = "Seasons", color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(32.dp))
        }

        // Review 301: Horizontal Season Tabs for fast TV navigation
        item {
            Row(
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                series.seasons.forEach { season ->
                    var isFocused by remember { mutableStateOf(false) }
                    val selected = expandedSeason == season.number
                    Surface(
                        onClick = { expandedSeason = season.number },
                        modifier = Modifier.onFocusChanged { isFocused = it.isFocused },
                        color = if (selected || isFocused) Red else Gray.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(
                            text = "Season ${season.number}",
                            color = if (selected || isFocused) White else LightGray,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        val currentSeason = series.seasons.find { it.number == expandedSeason }
        if (currentSeason != null) {
            items(currentSeason.episodes) { episode ->
                EpisodeItemRow(series.title, episode, isWatched = lastEpisode?.id == episode.id)
            }
        }
    }
}

@UnstableApi
@Composable
fun EpisodeItemRow(seriesTitle: String, episode: Episode, isWatched: Boolean) {
    val context = LocalContext.current
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = {
            val intent = Intent(context, PlayerActivity::class.java).apply {
                putExtra("channel_id", episode.id)
                putExtra("channel_name", "$seriesTitle - ${episode.getEpisodeDisplay()}")
                putExtra("channel_url", episode.streamUrl)
            }
            context.startActivity(intent)
        },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 2.dp).onFocusChanged { isFocused = it.isFocused },
        color = if (isFocused) White.copy(alpha = 0.1f) else Color.Transparent,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = episode.episodeNumber.toString(),
                color = if (isWatched) Red else (if (isFocused) White else Gray),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(40.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = episode.title,
                    color = if (isFocused) White else LightGray,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${episode.duration} min",
                    color = Gray,
                    fontSize = 12.sp
                )
            }
            
            if (isFocused) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Red)
            } else if (isWatched) {
                Text("RESUME", color = Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
