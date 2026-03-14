package com.rancon.freelivetv.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "movies")
data class Movie(
    @PrimaryKey val id: String,
    val title: String,
    val posterUrl: String,
    val backdropUrl: String = "",
    val streamUrl: String,
    val genre: List<String>,
    val year: Int,
    val duration: Int,
    val rating: Double,
    val description: String,
    val cast: List<String> = emptyList(),
    val director: String = "",
    var isFavorite: Boolean = false,
    var isNew: Boolean = false,
    val language: String = "English",
    val country: String = "USA",
    var lastPlayedPosition: Long = 0,
    var lastPlayedTime: Long = 0
) : Parcelable {

    fun getRatingStars(): String {
        return when {
            rating >= 9.0 -> "★★★★★"
            rating >= 8.0 -> "★★★★☆"
            rating >= 7.0 -> "★★★☆☆"
            rating >= 6.0 -> "★★☆☆☆"
            else -> "★☆☆☆☆"
        }
    }

    fun toggleFavorite(): Boolean {
        isFavorite = !isFavorite
        return isFavorite
    }

    fun getFormattedDuration(): String {
        val hours = duration / 60
        val minutes = duration % 60
        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }
    }
}
