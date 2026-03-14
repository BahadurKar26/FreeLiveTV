package com.rancon.freelivetv.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.rancon.freelivetv.ui.theme.*
import androidx.media3.common.util.UnstableApi

@UnstableApi
@Composable
fun MovieDetailsScreen(
    movieId: String,
    movieTitle: String,
    viewModel: ChannelViewModel,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val movies by viewModel.movies.collectAsState()
    val movie = remember(movies) { movies.find { it.id == movieId } }

    if (movie == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Red)
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
    ) {
        // Navigation & Favorite
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackPressed) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = White)
                }

                val isFavorite = viewModel.isFavorite(movieId)
                IconButton(onClick = { viewModel.toggleFavorite(movieId, "movie") }) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = stringResource(R.string.title_favorites),
                        tint = if (isFavorite) Yellow else LightGray
                    )
                }
            }
        }

        // Poster
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, White.copy(alpha = 0.1f))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(movie.posterUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = movie.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Info
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = movie.title, color = White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Yellow, modifier = Modifier.size(16.dp))
                    Text(text = " ${movie.rating}/10", color = Yellow, fontSize = 16.sp)
                }
                Text(text = "${movie.year} • ${movie.genre.joinToString(" • ")} • ${movie.getFormattedDuration()}", color = LightGray, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                Text(text = "Country: ${movie.country} • Language: ${movie.language}", color = LightGray, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }

        // Actions
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                var playFocused by remember { mutableStateOf(false) }
                Button(
                    onClick = { 
                        val intent = Intent(context, PlayerActivity::class.java).apply {
                            putExtra("channel_id", movie.id)
                            putExtra("channel_name", movie.title)
                            putExtra("channel_url", movie.streamUrl)
                        }
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (playFocused) White else Red),
                    modifier = Modifier.weight(1f).onFocusChanged { playFocused = it.isFocused }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = if (playFocused) Black else White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(R.string.label_play), color = if (playFocused) Black else White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Synopsis
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkGray)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = stringResource(R.string.label_synopsis), color = Red, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    Text(text = movie.description, color = White, fontSize = 14.sp, lineHeight = 20.sp)
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}
