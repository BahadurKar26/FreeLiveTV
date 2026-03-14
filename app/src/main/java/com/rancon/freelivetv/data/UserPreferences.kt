package com.rancon.freelivetv.data

import com.google.gson.annotations.SerializedName

data class UserPreferences(
    @SerializedName("watch_history")
    val watchHistory: MutableMap<String, WatchStats> = mutableMapOf(),
    @SerializedName("channel_health")
    val channelHealth: MutableMap<String, Int> = mutableMapOf(),
    @SerializedName("last_refresh_time")
    var lastRefreshTime: Long = 0L
)

data class WatchStats(
    @SerializedName("play_count")
    var playCount: Int = 0,
    @SerializedName("total_watch_time_ms")
    var totalWatchTimeMs: Long = 0
)