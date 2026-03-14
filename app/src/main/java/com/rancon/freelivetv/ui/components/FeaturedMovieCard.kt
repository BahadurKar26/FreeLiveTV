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
import com.rancon.freelivetv.data.Movie

@Composable
fun FeaturedMovieCard(
    movie: Movie,
    onPlayClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(16.dp)
            .onFocusChanged { isFocused = it.isFocused },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2C2C2C)
        ),
        border = if (isFocused) BorderStroke(2.dp, Color.White) else null
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Poster
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(movie.posterUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = movie.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Gradient overlay for text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            startY = 0.6f
                        )
                    )
            )

            // Content
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = movie.title,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${movie.year} • ${movie.genre.joinToString()} • ★${movie.rating}/10",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    var playFocused by remember { mutableStateOf(false) }
                    Button(
                        onClick = onPlayClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (playFocused) Color.White else Color(0xFFE50914)
                        ),
                        modifier = Modifier
                            .onFocusChanged { playFocused = it.isFocused }
                    ) {
                        Text(
                            "PLAY NOW",
                            color = if (playFocused) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    var favFocused by remember { mutableStateOf(false) }
                    OutlinedButton(
                        onClick = onFavoriteClick,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (favFocused) Color.White else Color.Gray
                        ),
                        modifier = Modifier
                            .onFocusChanged { favFocused = it.isFocused }
                    ) {
                        Text("+ FAVORITES")
                    }
                }
            }
        }
    }
}