package com.rancon.freelivetv

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rancon.freelivetv.ui.screens.*
import com.rancon.freelivetv.ui.theme.*
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@UnstableApi
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FreeLiveTVTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Black
                ) {
                    NetflixStyleApp()
                }
            }
        }
    }
}

@UnstableApi
@Composable
fun NetflixStyleApp() {
    val viewModel: ChannelViewModel = viewModel(factory = ChannelViewModel.Factory)
    val banglaChannels by viewModel.banglaChannels.collectAsState()
    val globalChannels by viewModel.globalChannels.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val navController = rememberNavController()
    var currentRoute by remember { mutableStateOf("home") }
    
    // Review 28: Double back to exit
    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    val context = androidx.compose.ui.platform.LocalContext.current

    BackHandler(enabled = currentRoute == "home") {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < 2000) {
            (context as? ComponentActivity)?.finish()
        } else {
            lastBackPressTime = currentTime
            Toast.makeText(context, "Press again to exit", Toast.LENGTH_SHORT).show()
        }
    }

    val navItems = listOf(
        R.string.nav_home to "home",
        R.string.nav_live_tv to "live",
        "Guide" to "epg",
        R.string.nav_movies to "movies",
        R.string.nav_series to "series",
        R.string.nav_you to "you"
    )

    if (isLoading && banglaChannels.isEmpty() && globalChannels.isEmpty()) {
        LoadingScreen()
    } else if (error != null) {
        ErrorScreen(
            message = error!!,
            onRetry = { viewModel.loadChannels() }
        )
    } else {
        Column(modifier = Modifier.fillMaxSize().background(Black)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 8.dp)
                    .padding(horizontal = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "RANCON TV",
                    color = Red,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 32.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    navItems.forEach { item ->
                        val label = if (item.first is Int) stringResource(item.first as Int) else item.first as String
                        val route = item.second
                        TvNavButton(
                            label = label,
                            isSelected = currentRoute == route,
                            onClick = {
                                currentRoute = route
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                }

                TvIconButton(
                    icon = Icons.Default.Search,
                    onClick = { navController.navigate("search") }
                )
            }

            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                NavHost(
                    navController = navController,
                    startDestination = "home"
                ) {
                    composable("home") {
                        HomeScreen(
                            viewModel = viewModel,
                            banglaChannels = banglaChannels,
                            globalChannels = globalChannels,
                            onChannelClick = { id, name ->
                                navController.navigate("player/${URLEncoder.encode(id, "UTF-8")}/${URLEncoder.encode(name, "UTF-8")}")
                            },
                            onMovieClick = { id, title ->
                                navController.navigate("movie/${URLEncoder.encode(id, "UTF-8")}/${URLEncoder.encode(title, "UTF-8")}")
                            },
                            onSeriesClick = { id, title ->
                                navController.navigate("series/${URLEncoder.encode(id, "UTF-8")}/${URLEncoder.encode(title, "UTF-8")}")
                            }
                        )
                    }

                    composable("live") {
                        LiveTVScreen(
                            viewModel = viewModel,
                            onChannelClick = { id, name ->
                                navController.navigate("player/${URLEncoder.encode(id, "UTF-8")}/${URLEncoder.encode(name, "UTF-8")}")
                            }
                        )
                    }

                    composable("epg") {
                        EpgScreen(
                            viewModel = viewModel,
                            onChannelClick = { id, name ->
                                navController.navigate("player/${URLEncoder.encode(id, "UTF-8")}/${URLEncoder.encode(name, "UTF-8")}")
                            },
                            onBackPressed = { navController.popBackStack() }
                        )
                    }

                    composable("movies") {
                        MoviesScreen(
                            viewModel = viewModel,
                            onMovieClick = { id, title ->
                                navController.navigate("movie/${URLEncoder.encode(id, "UTF-8")}/${URLEncoder.encode(title, "UTF-8")}")
                            }
                        )
                    }

                    composable("series") {
                        SeriesScreen(
                            viewModel = viewModel,
                            onSeriesClick = { id, title ->
                                navController.navigate("series/${URLEncoder.encode(id, "UTF-8")}/${URLEncoder.encode(title, "UTF-8")}")
                            }
                        )
                    }

                    composable("you") {
                        YouHubScreen(
                            viewModel = viewModel,
                            inactiveChannels = emptyList(), // Inactive channels handled in Hub
                            onNavigateToFavorites = { navController.navigate("favorites") },
                            onNavigateToSearch = { navController.navigate("search") },
                            onNavigateToSettings = { navController.navigate("settings") }
                        )
                    }

                    composable(
                        route = "player/{channelId}/{channelName}",
                        arguments = listOf(
                            navArgument("channelId") { type = NavType.StringType },
                            navArgument("channelName") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val channelId = backStackEntry.arguments?.getString("channelId") ?: ""
                        val channelName = backStackEntry.arguments?.getString("channelName") ?: ""
                        PlayerScreen(
                            channelId = java.net.URLDecoder.decode(channelId, "UTF-8"),
                            channelName = java.net.URLDecoder.decode(channelName, "UTF-8"),
                            viewModel = viewModel,
                            onBackPressed = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = "movie/{movieId}/{movieTitle}",
                        arguments = listOf(
                            navArgument("movieId") { type = NavType.StringType },
                            navArgument("movieTitle") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        MovieDetailsScreen(
                            movieId = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("movieId") ?: "", "UTF-8"),
                            movieTitle = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("movieTitle") ?: "", "UTF-8"),
                            viewModel = viewModel,
                            onBackPressed = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = "series/{seriesId}/{seriesTitle}",
                        arguments = listOf(
                            navArgument("seriesId") { type = NavType.StringType },
                            navArgument("seriesTitle") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        SeriesDetailsScreen(
                            seriesId = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("seriesId") ?: "", "UTF-8"),
                            seriesTitle = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("seriesTitle") ?: "", "UTF-8"),
                            viewModel = viewModel,
                            onBackPressed = { navController.popBackStack() }
                        )
                    }

                    composable("search") {
                        SearchScreen(
                            viewModel = viewModel,
                            onItemClick = { type, id, name ->
                                val eid = URLEncoder.encode(id, "UTF-8")
                                val ename = URLEncoder.encode(name, "UTF-8")
                                when (type) {
                                    "live" -> navController.navigate("player/$eid/$ename")
                                    "movie" -> navController.navigate("movie/$eid/$ename")
                                    "series" -> navController.navigate("series/$eid/$ename")
                                }
                            }
                        )
                    }

                    composable("favorites") {
                        FavoritesScreen(
                            viewModel = viewModel,
                            onItemClick = { type, id, name ->
                                val eid = URLEncoder.encode(id, "UTF-8")
                                val ename = URLEncoder.encode(name, "UTF-8")
                                when (type) {
                                    "live" -> navController.navigate("player/$eid/$ename")
                                    "movie" -> navController.navigate("movie/$eid/$ename")
                                    "series" -> navController.navigate("series/$eid/$ename")
                                }
                            }
                        )
                    }

                    composable("settings") {
                        SettingsScreen(
                            viewModel = viewModel,
                            onRefreshClick = { viewModel.loadChannels() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TvNavButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isFocused) 1.1f else 1.0f, label = "scale")
    
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
    ) {
        Text(
            text = label,
            color = if (isSelected || isFocused) White else Gray,
            fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Normal,
            fontSize = 16.sp
        )
    }
}

@Composable
fun TvIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isFocused) 1.2f else 1.0f, label = "scale")

    IconButton(
        onClick = onClick,
        modifier = Modifier
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (isFocused) White else Gray
        )
    }
}
