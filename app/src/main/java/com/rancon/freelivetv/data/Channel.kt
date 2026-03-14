package com.rancon.freelivetv.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

/**
 * Optimized Channel Entity (Elite Cycle 6)
 * Added isVisible flag for broken link management (Review 304).
 */
@Parcelize
@Entity(
    tableName = "channels",
    indices = [
        Index(value = ["category"]),
        Index(value = ["healthStatus"])
    ]
)
data class Channel(
    @PrimaryKey val id: String,
    val name: String,
    val urls: List<String>,
    val logo: String = "",
    val category: String = "General",
    val region: String = "Global",
    val language: String = "English",
    val priority: Int = 5,
    val epgId: String? = null,
    var healthStatus: String = "ACTIVE",
    var failureCount: Int = 0,
    var lastPlayedTime: Long = 0,
    var currentUrlIndex: Int = 0,
    var isFavorite: Boolean = false,
    var lastFailureTime: Long = 0,
    var latencyMs: Long = -1,
    var bitrate: Double = 0.0,
    var isBdix: Boolean = false,
    var isNew: Boolean = false,
    var isVisible: Boolean = true, // Review 304: Broken link safety
    val addedAt: Long = System.currentTimeMillis()
) : Parcelable {

    fun getCurrentUrl(): String = if (urls.isNotEmpty()) urls[currentUrlIndex] else ""

    fun hasNextUrl(): Boolean = currentUrlIndex < urls.size - 1

    fun nextUrl(): Boolean {
        return if (currentUrlIndex < urls.size - 1) {
            currentUrlIndex++
            true
        } else {
            false
        }
    }

    fun resetUrlIndex() {
        currentUrlIndex = 0
    }

    fun daysRemaining(): Int {
        if (lastFailureTime == 0L) return 7
        val daysSinceFailure = (System.currentTimeMillis() - lastFailureTime) / (24 * 60 * 60 * 1000)
        return (7 - daysSinceFailure).toInt().coerceAtMost(0)
    }

    fun markAsFailed() {
        failureCount++
        lastFailureTime = System.currentTimeMillis()
        if (failureCount >= 3) {
            healthStatus = "INACTIVE"
        }
        // Review 304: Permanently hide after 15 failures
        if (failureCount >= 15) {
            isVisible = false
        }
    }

    fun markAsSuccess() {
        failureCount = 0
        healthStatus = "ACTIVE"
        lastFailureTime = 0
        isVisible = true
    }

    fun toggleFavorite(): Boolean {
        isFavorite = !isFavorite
        return isFavorite
    }

    fun getQuality(): String {
        return when {
            name.contains("1080p", ignoreCase = true) -> "1080p"
            name.contains("720p", ignoreCase = true) -> "720p"
            name.contains("576p", ignoreCase = true) -> "576p"
            name.contains("480p", ignoreCase = true) -> "480p"
            name.contains("360p", ignoreCase = true) -> "360p"
            else -> "Auto"
        }
    }

    fun getDisplayName(): String {
        return name.replace(Regex("\\s*\\(\\d+p\\)\\s*"), "").trim()
    }

    companion object {
        const val HEALTH_ACTIVE = "ACTIVE"
        const val HEALTH_INACTIVE = "INACTIVE"
        const val MAX_FAILURES = 3
        const val CLEANUP_DAYS = 7
    }
}
