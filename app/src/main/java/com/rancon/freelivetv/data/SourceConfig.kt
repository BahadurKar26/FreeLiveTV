package com.rancon.freelivetv.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sources")
data class SourceConfig(
    @PrimaryKey val id: String,
    val name: String,
    val url: String,
    val type: String, // "M3U", "JSON", etc.
    val category: String,
    val lastFetched: Long = 0,
    val isActive: Boolean = true,
    val priority: Int = 1
)
