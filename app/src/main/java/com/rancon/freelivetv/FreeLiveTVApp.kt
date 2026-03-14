package com.rancon.freelivetv

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.StatFs
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.work.*
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.rancon.freelivetv.data.ChannelSyncWorker
import com.rancon.freelivetv.data.EpgRepository
import com.rancon.freelivetv.data.ReminderWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "freelivetv_prefs")

@UnstableApi
class FreeLiveTVApp : Application(), ImageLoaderFactory {

    // Review 97: Structured Application Scope
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        lateinit var instance: FreeLiveTVApp
            private set
        
        private var simpleCache: SimpleCache? = null
        
        fun getCache(context: Context): SimpleCache {
            if (simpleCache == null) {
                val cacheDir = File(context.cacheDir, "media_cache")
                val freeSpace = getFreeInternalSpace(context)
                val cacheSize = if (freeSpace > 2000) 200L * 1024 * 1024 else 100L * 1024 * 1024
                val evictor = LeastRecentlyUsedCacheEvictor(cacheSize)
                val databaseProvider = StandaloneDatabaseProvider(context)
                simpleCache = SimpleCache(cacheDir, evictor, databaseProvider)
            }
            return simpleCache!!
        }

        private fun getFreeInternalSpace(context: Context): Long {
            val stat = StatFs(context.filesDir.path)
            return (stat.availableBlocksLong * stat.blockSizeLong) / (1024 * 1024)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        scheduleSyncWorker()
        ReminderWorker.schedule(this)
        
        // Review 97: Integrated with applicationScope
        applicationScope.launch(Dispatchers.IO) {
            EpgRepository(this@FreeLiveTVApp).refreshEpg()
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_RUNNING_CRITICAL) {
            // Hard clear caches on low-RAM devices (Review 95)
            newImageLoader().memoryCache?.clear()
            System.gc()
        }
    }

    private fun scheduleSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<ChannelSyncWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ChannelSyncWorker",
            ExistingPeriodicWorkPolicy.REPLACE,
            syncRequest
        )
    }

    override fun newImageLoader(): ImageLoader {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val isLowRamDevice = activityManager.isLowRamDevice
        
        val diskCacheLimit = if (isLowRamDevice) 50L * 1024 * 1024 else 100L * 1024 * 1024
        val memoryCachePercent = if (isLowRamDevice) 0.10 else 0.20

        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(memoryCachePercent)
                    .strongReferencesEnabled(!isLowRamDevice) // Review 95: Optimize for low ram
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(diskCacheLimit)
                    .build()
            }
            .respectCacheHeaders(false)
            .crossfade(true)
            .build()
    }
}
