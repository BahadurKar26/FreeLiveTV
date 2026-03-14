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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rancon.freelivetv.data.FeaturedContent

@Composable
fun FeaturedContentCard(
    featured: FeaturedContent,
    onPlayClick: () -> Unit,
    onDetailsClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(horizontal = 16.dp)
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
            // Placeholder for poster/image
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1A1A1A))
            )

            // Content overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = featured.title,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${featured.year} • ${featured.genre} • Rating: ${featured.rating}",
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
                            text = "PLAY",
                            color = if (playFocused) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    var detailsFocused by remember { mutableStateOf(false) }
                    OutlinedButton(
                        onClick = onDetailsClick,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (detailsFocused) Color.White else Color.Gray
                        ),
                        modifier = Modifier
                            .onFocusChanged { detailsFocused = it.isFocused }
                    ) {
                        Text(text = "DETAILS")
                    }
                }
            }
        }
    }
}
