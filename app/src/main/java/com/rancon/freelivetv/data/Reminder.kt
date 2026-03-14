package com.rancon.freelivetv.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey val programId: Long,
    val channelId: String,
    val programTitle: String,
    val startTime: Long,
    val isNotified: Boolean = false
) : Parcelable
