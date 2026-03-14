package com.rancon.freelivetv.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_reports")
data class SyncReport(
    @PrimaryKey val sourceId: String,
    val sourceName: String,
    val lastSyncTime: Long = System.currentTimeMillis(),
    val status: String, // "SUCCESS", "PARTIAL", "FAILED"
    val itemsFound: Int,
    val errorMessage: String? = null
)
