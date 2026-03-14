package com.rancon.freelivetv.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "interactions",
    indices = [Index(value = ["timestamp"]), Index(value = ["actionType"])]
)
data class Interaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contentId: String,
    val contentType: String,
    val actionType: String,
    val duration: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val searchQuery: String? = null,
    val category: String? = null,
    val language: String? = null
)
