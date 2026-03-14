package com.rancon.freelivetv.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

class ChannelRepository(private val context: Context, private val scope: CoroutineScope) {

    private val database = AppDatabase.getDatabase(context)
    private val channelDao = database.channelDao()

    val allChannels: Flow<List<Channel>> = channelDao.getAllChannels()
    
    // Review 501: Refactored dynamic category flows to avoid redundant boilerplate
    fun getChannelsByCategoryFlow(category: String): Flow<List<Channel>> = allChannels.map { list ->
        list.filter { 
            (if (category == "All") true else it.category.contains(category, ignoreCase = true)) 
            && it.healthStatus == "ACTIVE" 
            && it.isVisible
        }
    }

    // Standard flows for UI rows
    val banglaChannels = getChannelsByCategoryFlow("Bangla")
    val globalChannels = getChannelsByCategoryFlow("Global")
    val newsChannels = getChannelsByCategoryFlow("News")
    val sportsChannels = getChannelsByCategoryFlow("Sports")
    val movieChannels = getChannelsByCategoryFlow("Movies")
    val entertainmentChannels = getChannelsByCategoryFlow("Entertainment")
    val kidsChannels = getChannelsByCategoryFlow("Kids")

    val inactiveChannels: Flow<List<Channel>> = allChannels.map { list ->
        list.filter { it.healthStatus == "INACTIVE" }
    }

    val favorites: Flow<List<Channel>> = channelDao.getFavoriteChannels()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "ChannelRepository"
        private const val CHANNEL_FILE = "curated_channels.json"
        
        private val USER_AGENTS = listOf(
            "FreeLiveTV-Elite/2.2",
            "Mozilla/5.0 (Linux; Android 10; SM-G973F) AppleWebKit/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
        )
        fun getRandomUserAgent() = USER_AGENTS.random()
    }

    init {
        scope.launch {
            if (channelDao.getAllChannelsSync().isEmpty()) {
                seedFromAssets()
            }
        }
    }

    private suspend fun seedFromAssets() {
        withContext(Dispatchers.IO) {
            try {
                val json = context.assets.open(CHANNEL_FILE).bufferedReader().use { it.readText() }
                val channels = parseJson(json)
                if (channels.isNotEmpty()) {
                    upsertChannels(channels)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error seeding from assets: ${e.message}")
            }
        }
    }

    private suspend fun <T> retryRequest(
        times: Int = 3,
        initialDelay: Long = 1000,
        maxDelay: Long = 5000,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        repeat(times - 1) {
            try {
                return block()
            } catch (e: Exception) {
                Log.w(TAG, "Request failed, retrying in $currentDelay ms...")
            }
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
        return block()
    }

    suspend fun refreshChannels() {
        withContext(Dispatchers.IO) {
            try {
                val json = retryRequest {
                    val request = Request.Builder()
                        .url(Config.CHANNELS_URL)
                        .header("User-Agent", getRandomUserAgent())
                        .build()
                    httpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw Exception("Unexpected code $response")
                        response.body?.string()
                    }
                }

                if (json != null) {
                    val remoteChannels = parseJson(json)
                    if (remoteChannels.isNotEmpty()) {
                        upsertChannels(remoteChannels)
                        Log.i(TAG, "Successfully synced ${remoteChannels.size} channels")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync remote channels after retries: ${e.message}")
            }
        }
    }

    private suspend fun upsertChannels(newChannels: List<Channel>) {
        withContext(Dispatchers.IO) {
            val existingChannels = channelDao.getAllChannelsSync().associateBy { it.id }
            val toUpdate = newChannels.map { channel ->
                val existing = existingChannels[channel.id]
                if (existing != null) {
                    channel.copy(
                        isFavorite = existing.isFavorite,
                        failureCount = existing.failureCount,
                        lastPlayedTime = existing.lastPlayedTime,
                        lastFailureTime = existing.lastFailureTime,
                        healthStatus = existing.healthStatus,
                        currentUrlIndex = existing.currentUrlIndex,
                        isVisible = existing.isVisible, // Review 701: Preserve visibility flag
                        isNew = false
                    )
                } else {
                    channel.copy(isNew = true)
                }
            }
            
            toUpdate.chunked(100).forEach { chunk ->
                channelDao.insertAll(chunk)
                yield()
            }
        }
    }

    private fun parseJson(jsonString: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        try {
            val trimmed = jsonString.trim()
            if (trimmed.startsWith("[")) {
                val jsonArray = JSONArray(trimmed)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val id = obj.optString("id", obj.optString("url", "ch_$i"))
                    val name = obj.optString("name", obj.optString("title", "Unknown"))
                    
                    val urls = mutableListOf<String>()
                    if (obj.has("urls")) {
                        val urlsArray = obj.getJSONArray("urls")
                        for (j in 0 until urlsArray.length()) {
                            urlsArray.optString(j)?.takeIf { it.isNotBlank() }?.let { urls.add(it) }
                        }
                    } else if (obj.has("url")) {
                        urls.add(obj.getString("url"))
                    }
                    
                    if (urls.isEmpty()) continue

                    channels.add(Channel(
                        id = id,
                        name = name,
                        urls = urls.distinct(),
                        logo = obj.optString("logo", ""),
                        category = obj.optString("category", "General"),
                        region = obj.optString("region", "Global"),
                        language = obj.optString("language", "English"),
                        priority = obj.optInt("priority", 5),
                        epgId = obj.optString("epgId", "")
                    ))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "JSON Parse error: ${e.message}")
        }
        return channels
    }

    suspend fun toggleFavorite(channelId: String) {
        val channel = channelDao.getChannelById(channelId)
        channel?.let {
            it.isFavorite = !it.isFavorite
            channelDao.update(it)
        }
    }

    suspend fun reportFailure(channelId: String) {
        val channel = channelDao.getChannelById(channelId)
        channel?.let {
            it.markAsFailed() // Review 701 logic integrated in markAsFailed
            channelDao.update(it)
        }
    }

    suspend fun reportSuccess(channelId: String) {
        val channel = channelDao.getChannelById(channelId)
        channel?.let {
            it.markAsSuccess()
            channelDao.update(it)
        }
    }

    suspend fun recordWatch(channelId: String) {
        val channel = channelDao.getChannelById(channelId)
        channel?.let {
            it.lastPlayedTime = System.currentTimeMillis()
            channelDao.update(it)
        }
    }

    fun searchChannels(query: String): Flow<List<Channel>> {
        return allChannels.map { list ->
            list.filter { it.name.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true) }
        }
    }
}
