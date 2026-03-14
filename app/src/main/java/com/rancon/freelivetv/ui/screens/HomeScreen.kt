package com.rancon.freelivetv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rancon.freelivetv.ChannelViewModel
import com.rancon.freelivetv.R
import com.rancon.freelivetv.data.Channel
import com.rancon.freelivetv.data.FeaturedContent
import com.rancon.freelivetv.ui.components.*
import com.rancon.freelivetv.ui.theme.*

@Composable
fun HomeScreen(
    viewModel: ChannelViewModel,
    banglaChannels: List<Channel>,
    globalChannels: List<Channel>,
    onChannelClick: (String, String) -> Unit,
    onMovieClick: (String, String) -> Unit,
    onSeriesClick: (String, String) -> Unit
) {
    val listState = rememberLazyListState()
    val isLoading by viewModel.isLoading.collectAsState()
    val networkType by viewModel.networkType.collectAsState()
    
    val newsChannels by viewModel.newsChannels.collectAsState()
    val sportsChannels by viewModel.sportsChannels.collectAsState()
    val movieChannels by viewModel.movieChannels.collectAsState()
    val entertainmentChannels by viewModel.entertainmentChannels.collectAsState()
    val kidsChannels by viewModel.kidsChannels.collectAsState()
    
    val allChannels = remember(banglaChannels, globalChannels) {
        (banglaChannels + globalChannels).distinctBy { it.id }
    }
    
    val continueWatching by viewModel.continueWatching.collectAsState()
    val recommendedChannels by viewModel.recommendedChannels.collectAsState()
    val recommendedMovies by viewModel.recommendedMovies.collectAsState()
    
    var affinityRecs by remember { mutableStateOf<Pair<String, List<Channel>>?>(null) }
    
    LaunchedEffect(Unit) {
        affinityRecs = viewModel.getRecommendationsForTopCategory()
    }

    val featuredContent = remember {
        FeaturedContent(
            title = "Featured Channel",
            year = "LIVE",
            genre = "Entertainment",
            rating = 9.0
        )
    }

    if (isLoading && allChannels.isEmpty()) {
        Column(modifier = Modifier.fillMaxSize().background(Black)) {
            ShimmerRow()
            ShimmerRow()
            ShimmerRow()
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(Black),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Phase 3.12: Added specific keys to all items to prevent focus jitter
            item(key = "home_featured") {
                FeaturedContentCard(
                    featured = featuredContent,
                    onPlayClick = { /* Play featured */ },
                    onDetailsClick = { /* Show details */ }
                )
            }

            item(key = "home_network_banner") {
                NetworkStatusBanner(networkType = networkType)
            }

            if (continueWatching.isNotEmpty()) {
                item(key = "home_continue_watching") {
                    SectionHeader(stringResource(R.string.title_continue_watching))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(continueWatching, key = { it.id }) { item ->
                            var isFocused by remember { mutableStateOf(false) }
                            Card(
                                onClick = { 
                                    if (item.type == "movie") onMovieClick(item.id, item.title) 
                                    else onSeriesClick(item.id, item.title)
                                },
                                modifier = Modifier
                                    .width(200.dp)
                                    .height(112.dp)
                                    .onFocusChanged { isFocused = it.isFocused },
                                colors = CardDefaults.cardColors(containerColor = DarkGray),
                                border = if (isFocused) androidx.compose.foundation.BorderStroke(2.dp, Red) else null
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomStart) {
                                    Text(
                                        text = item.title,
                                        color = White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(12.dp),
                                        maxLines = 1
                                    )
                                    LinearProgressIndicator(
                                        progress = { 0.5f },
                                        modifier = Modifier.fillMaxWidth().height(4.dp),
                                        color = Red,
                                        trackColor = Gray.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (newsChannels.isNotEmpty()) {
                item(key = "home_news_row") {
                    SectionHeader("News Channels")
                    ChannelRow(channels = newsChannels, onChannelClick = onChannelClick)
                }
            }

            if (sportsChannels.isNotEmpty()) {
                item(key = "home_sports_row") {
                    SectionHeader("Sports Live")
                    ChannelRow(channels = sportsChannels, onChannelClick = onChannelClick)
                }
            }

            if (movieChannels.isNotEmpty()) {
                item(key = "home_movie_row") {
                    SectionHeader("Movie Channels")
                    ChannelRow(channels = movieChannels, onChannelClick = onChannelClick)
                }
            }

            if (banglaChannels.isNotEmpty()) {
                item(key = "home_bangla_row") {
                    SectionHeader("Bangla Originals")
                    ChannelRow(channels = banglaChannels, onChannelClick = onChannelClick)
                }
            }

            affinityRecs?.let { (category, recs) ->
                if (recs.isNotEmpty()) {
                    item(key = "home_affinity_${category}") {
                        SectionHeader("More from $category")
                        ChannelRow(channels = recs, onChannelClick = onChannelClick)
                    }
                }
            }

            if (entertainmentChannels.isNotEmpty()) {
                item(key = "home_entertainment_row") {
                    SectionHeader("Entertainment")
                    ChannelRow(channels = entertainmentChannels, onChannelClick = onChannelClick)
                }
            }

            if (kidsChannels.isNotEmpty()) {
                item(key = "home_kids_row") {
                    SectionHeader("Kids Zone")
                    ChannelRow(channels = kidsChannels, onChannelClick = onChannelClick)
                }
            }

            if (recommendedMovies.isNotEmpty()) {
                item(key = "home_movies_row") {
                    SectionHeader("Popular Movies")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(recommendedMovies, key = { it.id }) { movie ->
                            MovieCard(movie = movie, onClick = { onMovieClick(movie.id, movie.title) })
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(48.dp)) }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        color = White,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
fun NetworkStatusBanner(networkType: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        color = if (networkType == "Wi-Fi") Red.copy(alpha = 0.1f) else DarkGray,
        shape = RoundedCornerShape(8.dp),
        border = if (networkType == "Wi-Fi") androidx.compose.foundation.BorderStroke(1.dp, Red.copy(alpha = 0.3f)) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (networkType == "Wi-Fi") "⚡ BDIX FAST UNLOCKED" else "Network: $networkType",
                color = if (networkType == "Wi-Fi") Red else White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (networkType == "Wi-Fi") "You are on Wi-Fi. Local FTP content is now prioritized for buffer-free streaming." 
                       else "Connect to Wi-Fi/BDIX to unlock ultra-fast local content.",
                color = LightGray,
                fontSize = 12.sp
            )
        }
    }
}
