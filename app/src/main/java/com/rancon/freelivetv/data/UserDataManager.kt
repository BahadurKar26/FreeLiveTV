package com.rancon.freelivetv.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class UserDataManager(private val context: Context) {

    private val gson = Gson()
    private val prefsFile = File(context.filesDir, "user_data.json")
    private val scope = CoroutineScope(Dispatchers.IO)

    private data class UserData(
        val failureCounts: MutableMap<String, Int> = ConcurrentHashMap<String, Int>(),
        val lastFailureTimes: MutableMap<String, Long> = ConcurrentHashMap<String, Long>(),
        val lastPlayedTimes: MutableMap<String, Long> = ConcurrentHashMap<String, Long>(),
        val watchCounts: MutableMap<String, Int> = ConcurrentHashMap<String, Int>(),
        val watchTime: MutableMap<String, Long> = ConcurrentHashMap<String, Long>(),
        val watchProgress: MutableMap<String, Long> = ConcurrentHashMap<String, Long>(), // id -> position in ms
        var lastRefreshTime: Long = 0L
    )

    @Volatile
    private var data = UserData()

    private val _allProgress = MutableStateFlow<Map<String, Long>>(emptyMap())
    val allProgress: StateFlow<Map<String, Long>> = _allProgress.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        try {
            if (prefsFile.exists()) {
                val json = prefsFile.readText()
                val type = object : TypeToken<UserData>() {}.type
                val loadedData: UserData? = gson.fromJson(json, type)
                if (loadedData != null) {
                    data = loadedData
                    _allProgress.value = data.watchProgress.toMap()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveData() {
        _allProgress.value = data.watchProgress.toMap()
        scope.launch {
            try {
                synchronized(this@UserDataManager) {
                    val json = gson.toJson(data)
                    prefsFile.writeText(json)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getFailureCount(channelId: String): Int = data.failureCounts[channelId] ?: 0

    fun incrementFailureCount(channelId: String) {
        data.failureCounts[channelId] = (data.failureCounts[channelId] ?: 0) + 1
        saveData()
    }

    fun resetFailureCount(channelId: String) {
        data.failureCounts.remove(channelId)
        saveData()
    }

    fun getLastFailureTime(channelId: String): Long = data.lastFailureTimes[channelId] ?: 0

    fun setLastFailureTime(channelId: String, time: Long) {
        data.lastFailureTimes[channelId] = time
        saveData()
    }

    fun getLastPlayedTime(id: String): Long = data.lastPlayedTimes[id] ?: 0

    fun setLastPlayedTime(id: String, time: Long) {
        data.lastPlayedTimes[id] = time
        saveData()
    }

    fun recordWatch(id: String, duration: Long) {
        data.watchCounts[id] = (data.watchCounts[id] ?: 0) + 1
        data.watchTime[id] = (data.watchTime[id] ?: 0) + duration
        saveData()
    }

    fun saveProgress(id: String, position: Long) {
        if (position > 0) {
            data.watchProgress[id] = position
            saveData()
        }
    }

    fun getProgress(id: String): Long = data.watchProgress[id] ?: 0L

    fun getMostWatched(): List<String> {
        return data.watchCounts.entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(20)
    }

    fun resetAll() {
        synchronized(this) {
            data = UserData()
            _allProgress.value = emptyMap()
            saveData()
        }
    }

    fun getLastRefreshTime(): Long = data.lastRefreshTime

    fun setLastRefreshTime(time: Long) {
        data.lastRefreshTime = time
        saveData()
    }
}
