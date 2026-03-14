package com.rancon.freelivetv.data

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class ChannelSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = coroutineScope {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val channelRepo = ChannelRepository(applicationContext, scope)
        val moviesRepo = MoviesRepository(applicationContext, scope)
        val seriesRepo = SeriesRepository(applicationContext, scope)
        val discoveryRepo = DiscoveryRepository(applicationContext)
        val userDataManager = UserDataManager(applicationContext)
        
        try {
            Log.i("ChannelSyncWorker", "Starting background sync and discovery (Crawler Strategy)...")
            
            // 1. Refresh curated content from primary GitHub source
            val deferredChannels = async { channelRepo.refreshChannels() }
            val deferredMovies = async { moviesRepo.refreshMovies() }
            val deferredSeries = async { seriesRepo.refreshSeries() }
            
            deferredChannels.await()
            deferredMovies.await()
            deferredSeries.await()

            // 2. Perform On-Device Discovery (AI-Assisted Discovery equivalent)
            // This crawls external sources defined in sources.json
            discoveryRepo.performDiscovery()

            // 3. Sync EPG Data (Now Playing / Next Program)
            discoveryRepo.refreshEpg()

            // 4. Self-Healing & Latency/Bitrate Ranking
            // Monitors active streams and replaces dead ones with best available mirrors
            discoveryRepo.selfHeal()
            
            // Update last refresh time for Settings screen
            userDataManager.setLastRefreshTime(System.currentTimeMillis())
            
            Log.i("ChannelSyncWorker", "Sync and Discovery completed successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e("ChannelSyncWorker", "Error during sync: ${e.message}")
            Result.retry()
        }
    }
}
