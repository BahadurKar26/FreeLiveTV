package com.rancon.freelivetv.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.*

/**
 * Optimized Program Entity (Review 5)
 * Added indices for fast EPG lookup during TV guide scrolling.
 */
@Parcelize
@Entity(
    tableName = "programs",
    indices = [
        Index(value = ["channelId", "startTime", "endTime"]),
        Index(value = ["startTime"])
    ]
)
data class Program(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelId: String,
    val title: String,
    val description: String,
    val startTime: Long,
    val endTime: Long,
    val category: String = "General",
    val episodeTitle: String? = null,
    val episodeNumber: Int? = null,
    val seasonNumber: Int? = null,
    val rating: String? = null,
    val isNew: Boolean = false
) : Parcelable {

    fun isLive(): Boolean {
        val now = System.currentTimeMillis()
        return now in startTime..endTime
    }

    fun isUpcoming(): Boolean {
        return System.currentTimeMillis() < startTime
    }

    fun hasEnded(): Boolean {
        return System.currentTimeMillis() > endTime
    }

    fun getDurationMinutes(): Int {
        return ((endTime - startTime) / (60 * 1000)).toInt()
    }

    fun getTimeRange(): String {
        val startDate = Date(startTime)
        val endDate = Date(endTime)
        val format = SimpleDateFormat("h:mm a", Locale.getDefault())
        return "${format.format(startDate)} - ${format.format(endDate)}"
    }
}
