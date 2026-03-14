package com.rancon.freelivetv.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "series")
data class Series(
    @PrimaryKey val id: String,
    val title: String,
    val posterUrl: String,
    val backdropUrl: String = "",
    val genre: List<String>,
    val year: Int,
    val endYear: Int? = null,
    val rating: Double,
    val description: String,
    val cast: List<String> = emptyList(),
    val seasons: List<Season> = emptyList(),
    var isFavorite: Boolean = false,
    val language: String = "English",
    val country: String = "USA",
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

    fun getYearRange(): String {
        return if (endYear != null) {
            "$year - $endYear"
        } else {
            "$year - Present"
        }
    }
    
    fun getTotalSeasons(): Int = seasons.size
    fun getTotalEpisodes(): Int = seasons.sumOf { it.episodes.size }
}

@Parcelize
data class Season(
    val number: Int,
    val episodes: List<Episode>,
    val posterUrl: String? = null,
    val description: String? = null,
    val year: Int? = null
) : Parcelable

@Parcelize
@Entity(tableName = "episodes")
data class Episode(
    @PrimaryKey val id: String,
    val seriesId: String,
    val title: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val streamUrl: String,
    val thumbnailUrl: String = "",
    val duration: Int,
    val description: String = "",
    var lastPlayedPosition: Long = 0,
    var isWatched: Boolean = false,
    val airDate: Long? = null,
    val year: Int? = null,
    val rating: Double? = null,
    var lastPlayedTime: Long = 0
) : Parcelable {

    fun getEpisodeDisplay(): String {
        return "S${seasonNumber}E${episodeNumber}"
    }

    fun markAsWatched() {
        isWatched = true
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
