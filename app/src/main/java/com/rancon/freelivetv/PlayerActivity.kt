package com.rancon.freelivetv

import android.app.PictureInPictureParams
import android.content.Context
import android.content.res.Configuration
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.util.Rational
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.ui.PlayerView
import com.rancon.freelivetv.data.Channel
import com.rancon.freelivetv.data.FtpDataSource
import com.rancon.freelivetv.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@UnstableApi
class PlayerActivity : ComponentActivity() {

    private var exoPlayer: ExoPlayer? = null
    private var currentChannel by mutableStateOf<Channel?>(null)
    private var channelList = listOf<Channel>()
    private var errorMessage by mutableStateOf<String?>(null)
    private var isBuffering by mutableStateOf(true)
    private var isPlaying by mutableStateOf(false)
    private var currentUrlIndex by mutableIntStateOf(0)
    private var isNetworkAvailable by mutableStateOf(true)
    private var isInPipMode by mutableStateOf(false)

    private var brightness by mutableFloatStateOf(-1f)
    private var volumeLevel by mutableFloatStateOf(0f)
    private var showGestureOverlay by mutableStateOf<GestureType?>(null)

    enum class GestureType { BRIGHTNESS, VOLUME }

    private var lastInputTime by mutableLongStateOf(System.currentTimeMillis())
    private val IDLE_THRESHOLD_MS = 3 * 60 * 60 * 1000L

    private lateinit var viewModel: ChannelViewModel
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var channelCollectionJob: Job? = null
    
    private var watchStartTime: Long = 0L
    private var contentId: String = ""
    private var hasResumed: Boolean = false

    private var tracks by mutableStateOf(Tracks.EMPTY)
    private var playbackSpeed by mutableFloatStateOf(1.0f)
    private var showControls = mutableStateOf(true)

    private var audioFocusRequest: AudioFocusRequest? = null
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> exoPlayer?.volume = 1.0f
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> exoPlayer?.volume = 0.2f
            AudioManager.AUDIOFOCUS_LOSS -> exoPlayer?.pause()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        contentId = intent.getStringExtra("channel_id") ?: ""
        val channelName = intent.getStringExtra("channel_name") ?: ""
        val channelUrl = intent.getStringExtra("channel_url") ?: ""
        val backupUrl = intent.getStringExtra("backup_url")

        viewModel = ViewModelProvider(this, ChannelViewModel.Factory)[ChannelViewModel::class.java]

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        volumeLevel = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / 
                      audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        brightness = window.attributes.screenBrightness

        requestAudioFocus()
        setupNetworkMonitor()

        channelCollectionJob = lifecycleScope.launch {
            viewModel.channels.collect { channels ->
                channelList = channels
                if (currentChannel == null) {
                    currentChannel = channels.find { it.id == contentId } ?: Channel(
                        id = contentId,
                        name = channelName,
                        urls = listOfNotNull(channelUrl, backupUrl).filter { it.isNotEmpty() },
                        logo = "",
                        category = "General"
                    )

                    if (currentChannel?.urls?.isNotEmpty() == true) {
                        currentUrlIndex = 0
                        playUrl(currentChannel!!.urls[0])
                    } else {
                        errorMessage = "No stream URL available"
                        isBuffering = false
                    }
                }
            }
        }

        setContent {
            FreeLiveTVTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Black)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = { showGestureOverlay = null },
                                onDragCancel = { showGestureOverlay = null },
                                onVerticalDrag = { change, dragAmount ->
                                    val isLeft = change.position.x < size.width / 2
                                    if (isLeft) adjustBrightness(dragAmount / size.height)
                                    else adjustVolume(dragAmount / size.height)
                                }
                            )
                        }
                ) {
                    if (errorMessage == null && exoPlayer != null && isNetworkAvailable) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    player = exoPlayer
                                    useController = false
                                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    showGestureOverlay?.let { type ->
                        GestureOverlay(type = type, value = if (type == GestureType.BRIGHTNESS) brightness else volumeLevel)
                    }

                    if (!isInPipMode) {
                        PlayerControls(
                            channelName = currentChannel?.name ?: channelName,
                            isPlaying = isPlaying,
                            onPlayPauseToggle = { togglePlayPause() },
                            onBackClick = { finish() },
                            isBuffering = isBuffering,
                            errorMessage = errorMessage,
                            onRetryClick = { retryPlayback() },
                            availableUrls = currentChannel?.urls ?: emptyList(),
                            selectedUrlIndex = currentUrlIndex,
                            onUrlSelected = { index ->
                                currentUrlIndex = index
                                playUrl(currentChannel!!.urls[index])
                            },
                            tracks = tracks,
                            playbackSpeed = playbackSpeed,
                            onSpeedSelected = { speed ->
                                playbackSpeed = speed
                                exoPlayer?.setPlaybackSpeed(speed)
                            },
                            showControls = showControls.value,
                            onShowControlsChange = { showControls.value = it }
                        )
                    }

                    if (isBuffering && errorMessage == null && isNetworkAvailable) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Red)
                    }
                }
            }
        }
    }

    private fun playUrl(url: String) {
        exoPlayer?.release()
        try {
            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .setAllowCrossProtocolRedirects(true)

            val cacheDataSourceFactory = CacheDataSource.Factory()
                .setCache(FreeLiveTVApp.getCache(this))
                .setUpstreamDataSourceFactory(httpDataSourceFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

            // Review 1251: Enhanced Extractor Factory for BDIX .ts and .mkv files
            val extractorsFactory = DefaultExtractorsFactory()
                .setConstantBitrateSeekingEnabled(true)

            val mediaSource: MediaSource = when {
                url.startsWith("ftp://", ignoreCase = true) -> {
                    val ftpDataSourceFactory = DataSource.Factory { FtpDataSource() }
                    DefaultMediaSourceFactory(this).setDataSourceFactory(ftpDataSourceFactory).createMediaSource(MediaItem.fromUri(url))
                }
                // Review 1251: Handling varied BDIX file formats over HTTP
                url.contains(".ts", ignoreCase = true) || url.contains(".mkv", ignoreCase = true) || url.contains(".mp4", ignoreCase = true) -> {
                    ProgressiveMediaSource.Factory(cacheDataSourceFactory, extractorsFactory)
                        .createMediaSource(MediaItem.fromUri(url))
                }
                else -> HlsMediaSource.Factory(cacheDataSourceFactory).createMediaSource(MediaItem.fromUri(url))
            }

            exoPlayer = ExoPlayer.Builder(this).build().apply {
                setMediaSource(mediaSource)
                prepare()
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onTracksChanged(tracks: Tracks) { this@PlayerActivity.tracks = tracks }
                    override fun onIsPlayingChanged(playing: Boolean) {
                        this@PlayerActivity.isPlaying = playing
                        if (playing) {
                            watchStartTime = System.currentTimeMillis()
                            this@PlayerActivity.errorMessage = null
                            currentChannel?.let { viewModel.reportChannelSuccess(it.id) }
                            if (!hasResumed) {
                                val savedProgress = viewModel.getProgress(contentId)
                                if (savedProgress > 0) seekTo(savedProgress)
                                hasResumed = true
                            }
                        } else {
                            savePlaybackProgress()
                            recordWatchDuration()
                        }
                    }
                    override fun onPlaybackStateChanged(playbackState: Int) { runOnUiThread { this@PlayerActivity.isBuffering = playbackState == Player.STATE_BUFFERING } }
                    override fun onPlayerError(error: PlaybackException) {
                        val nextUrlIndex = currentUrlIndex + 1
                        if (currentChannel != null && nextUrlIndex < currentChannel!!.urls.size) {
                            currentUrlIndex = nextUrlIndex
                            runOnUiThread { playUrl(currentChannel!!.urls[currentUrlIndex]) }
                        } else switchToNextChannel()
                    }
                })
            }
        } catch (e: Exception) {
            errorMessage = "Failed to initialize player: ${e.message}"
            isBuffering = false
        }
    }

    private fun requestAudioFocus() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MOVIE).build())
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioManager.requestAudioFocus(audioFocusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
    }

    private fun adjustBrightness(delta: Float) {
        showGestureOverlay = GestureType.BRIGHTNESS
        brightness = (brightness - delta).coerceIn(0.01f, 1f)
        val lp = window.attributes
        lp.screenBrightness = brightness
        window.attributes = lp
    }

    private fun adjustVolume(delta: Float) {
        showGestureOverlay = GestureType.VOLUME
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        volumeLevel = (volumeLevel - delta).coerceIn(0f, 1f)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (volumeLevel * maxVolume).toInt(), 0)
    }

    private fun togglePlayPause() { if (isPlaying) exoPlayer?.pause() else exoPlayer?.play() }

    private fun retryPlayback() {
        errorMessage = null
        isBuffering = true
        currentUrlIndex = 0
        currentChannel?.urls?.getOrNull(0)?.let { playUrl(it) }
    }

    private fun switchToNextChannel() {
        if (channelList.isEmpty()) return
        val currentIndex = channelList.indexOfFirst { it.id == contentId }
        val nextChannel = if (currentIndex != -1 && currentIndex < channelList.size - 1) channelList[currentIndex + 1] else channelList.getOrNull(0)
        if (nextChannel != null && nextChannel.id != contentId) {
            contentId = nextChannel.id
            currentChannel = nextChannel
            currentUrlIndex = 0
            hasResumed = false
            playUrl(nextChannel.urls.getOrNull(0) ?: "")
        }
    }

    private fun savePlaybackProgress() {
        exoPlayer?.let { if (it.playbackState != Player.STATE_IDLE && it.playbackState != Player.STATE_ENDED) viewModel.saveProgress(contentId, it.currentPosition) }
    }

    private fun recordWatchDuration() {
        if (watchStartTime > 0) {
            val duration = System.currentTimeMillis() - watchStartTime
            if (duration > 5000) viewModel.recordWatch(contentId, duration)
            watchStartTime = 0L
        }
    }

    private fun setupNetworkMonitor() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isNetworkAvailable = true
                if (errorMessage != null) runOnUiThread { retryPlayback() }
            }
            override fun onLost(network: Network) {
                isNetworkAvailable = false
                runOnUiThread { exoPlayer?.pause() }
            }
        }
        connectivityManager.registerNetworkCallback(request, networkCallback!!)
    }

    override fun onPause() {
        super.onPause()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode) return
        savePlaybackProgress()
        exoPlayer?.pause()
        recordWatchDuration()
    }

    override fun onStop() {
        savePlaybackProgress()
        recordWatchDuration()
        exoPlayer?.release()
        exoPlayer = null
        super.onStop()
    }

    override fun onDestroy() {
        savePlaybackProgress()
        recordWatchDuration()
        networkCallback?.let {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            try { cm.unregisterNetworkCallback(it) } catch (e: Exception) {}
        }
        audioFocusRequest?.let {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) am.abandonAudioFocusRequest(it)
        }
        channelCollectionJob?.cancel()
        exoPlayer?.release()
        exoPlayer = null
        super.onDestroy()
    }
    
    @Composable
    fun GestureOverlay(type: GestureType, value: Float) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Card(colors = CardDefaults.cardColors(containerColor = Black.copy(alpha = 0.6f)), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = if (type == GestureType.BRIGHTNESS) Icons.Default.Brightness6 else Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = White, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(progress = { value }, color = Red, trackColor = White.copy(alpha = 0.3f), modifier = Modifier.width(100.dp))
                }
            }
        }
    }

    @Composable
    fun PlayerControls(channelName: String, isPlaying: Boolean, onPlayPauseToggle: () -> Unit, onBackClick: () -> Unit, isBuffering: Boolean, errorMessage: String?, onRetryClick: () -> Unit, availableUrls: List<String>, selectedUrlIndex: Int, onUrlSelected: (Int) -> Unit, tracks: Tracks, playbackSpeed: Float, onSpeedSelected: (Float) -> Unit, showControls: Boolean, onShowControlsChange: (Boolean) -> Unit) {
        var showQualityMenu by remember { mutableStateOf(false) }
        var showTrackMenu by remember { mutableStateOf(false) }
        var showSpeedMenu by remember { mutableStateOf(false) }
        LaunchedEffect(showControls, isPlaying, showQualityMenu, showTrackMenu, showSpeedMenu) { if (showControls && isPlaying && !showQualityMenu && !showTrackMenu && !showSpeedMenu) { delay(5000); onShowControlsChange(false) } }
        Box(modifier = Modifier.fillMaxSize().clickable { if (!showQualityMenu && !showTrackMenu && !showSpeedMenu) onShowControlsChange(!showControls) }) {
            if (showControls || !isPlaying || errorMessage != null) {
                Row(modifier = Modifier.fillMaxWidth().background(Black.copy(alpha = 0.5f)).padding(16.dp).align(Alignment.TopCenter), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = White) }
                    Text(text = channelName, color = White, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 16.dp))
                }
                if (errorMessage == null) {
                    IconButton(onClick = onPlayPauseToggle, modifier = Modifier.align(Alignment.Center).size(80.dp).background(Black.copy(alpha = 0.3f), RoundedCornerShape(40.dp))) {
                        Icon(imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play/Pause", tint = White, modifier = Modifier.size(48.dp))
                    }
                }
                if (errorMessage != null) {
                    Column(modifier = Modifier.align(Alignment.Center).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = errorMessage, color = White, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onRetryClick, colors = ButtonDefaults.buttonColors(containerColor = Red)) { Text(stringResource(R.string.retry)) }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().background(Black.copy(alpha = 0.5f)).padding(16.dp).align(Alignment.BottomCenter), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    if (tracks.groups.isNotEmpty()) {
                        Box {
                            IconButton(onClick = { showTrackMenu = true }) { Icon(Icons.Default.ClosedCaption, contentDescription = "Tracks", tint = White) }
                            DropdownMenu(expanded = showTrackMenu, onDismissRequest = { showTrackMenu = false }, modifier = Modifier.background(DarkGray)) {
                                tracks.groups.forEach { group -> if (group.type == C.TRACK_TYPE_AUDIO || group.type == C.TRACK_TYPE_TEXT) { for (i in 0 until group.length) { val isSelected = group.isTrackSelected(i); DropdownMenuItem(text = { Text(text = group.getTrackFormat(i).language ?: "Track ${i+1}", color = if (isSelected) Red else White) }, onClick = { exoPlayer?.trackSelectionParameters = exoPlayer?.trackSelectionParameters?.buildUpon()?.setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, i))?.build() ?: exoPlayer?.trackSelectionParameters!!; showTrackMenu = false }) } } }
                            }
                        }
                    }
                    Box {
                        IconButton(onClick = { showSpeedMenu = true }) { Icon(Icons.Default.Speed, contentDescription = "Speed", tint = White) }
                        DropdownMenu(expanded = showSpeedMenu, onDismissRequest = { showSpeedMenu = false }, modifier = Modifier.background(DarkGray)) {
                            listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed -> DropdownMenuItem(text = { Text("${speed}x", color = if (playbackSpeed == speed) Red else White) }, onClick = { onSpeedSelected(speed); showSpeedMenu = false }) }
                        }
                    }
                    if (availableUrls.size > 1) {
                        Box {
                            IconButton(onClick = { showQualityMenu = true }) { Icon(Icons.Default.Settings, contentDescription = "Quality", tint = White) }
                            DropdownMenu(expanded = showQualityMenu, onDismissRequest = { showQualityMenu = false }, modifier = Modifier.background(DarkGray)) {
                                availableUrls.forEachIndexed { index, _ -> DropdownMenuItem(text = { Text(text = if (index == 0) "Primary" else "Mirror $index", color = if (selectedUrlIndex == index) Red else White) }, onClick = { onUrlSelected(index); showQualityMenu = false }) }
                            }
                        }
                    }
                    IconButton(onClick = { enterPipMode() }) { Icon(Icons.Default.PictureInPicture, contentDescription = "PiP", tint = White) }
                }
            }
        }
    }

    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build()
            enterPictureInPictureMode(params)
        }
    }

    override fun onUserLeaveHint() { if (isPlaying && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) enterPipMode() }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
    }
}
