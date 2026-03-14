package com.rancon.freelivetv.data

import java.util.*

/**
 * Phase 1.10: Utility to map raw provider categories to clean, branded UI labels.
 * Updated in Round 8 for better edge-case coverage.
 */
object CategoryMapper {
    private val mapping = mapOf(
        "INTL-NEWS" to "News",
        "NEWS-LIVE" to "News",
        "NEWS-INTERNATIONAL" to "News",
        "SPORTS-TV" to "Sports",
        "LIVE-SPORTS" to "Sports",
        "SPORTS-LIVE-1" to "Sports",
        "MOVIES-HD" to "Movies",
        "CINEMA" to "Movies",
        "KIDS-ZONE" to "Kids",
        "CARTOONS" to "Kids",
        "MUSIC-VIDEO" to "Music",
        "HITS" to "Music",
        "DOCUMENTARY-TV" to "Documentary"
    )

    fun map(rawCategory: String): String {
        val upper = rawCategory.uppercase(Locale.ROOT).trim()
        val mapped = mapping[upper] ?: rawCategory.replaceFirstChar { 
            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() 
        }
        return mapped
    }
}
