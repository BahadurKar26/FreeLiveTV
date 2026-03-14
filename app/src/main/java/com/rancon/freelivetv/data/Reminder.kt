package com.rancon.freelivetv.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Elite Review 1: Added indices for daily cleanup performance
 */
@Parcelize
@Entity(
    tableName = "reminders",
    indices = [Index(value = ["startTime"])]
)
data class Reminder(
    @PrimaryKey val programId: Long,
    val channelId: String,
    val programTitle: String,
    val startTime: Long,
    val isNotified: Boolean = false
) : Parcelable
