package com.rancon.freelivetv.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.util.Calendar

class RecommendationRepository(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val channelDao = database.channelDao()
    private val movieDao = database.movieDao()
    private val interactionDao = database.interactionDao()

    /**
     * Returns a list of recommended channels based on the user's top category affinities
     * for the current time of day.
     */
    fun getRecommendedChannels(): Flow<List<Channel>> {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        
        // Define time slots
        val (start, end) = when (currentHour) {
            in 6..11 -> "06" to "11" // Morning
            in 12..17 -> "12" to "17" // Afternoon
            in 18..23 -> "18" to "23" // Evening
            else -> "00" to "05" // Night
        }

        return combine(
            channelDao.getAllChannels(),
            interactionDao.getCategoryAffinitiesForTimeSlot(start, end)
        ) { channels, timeSlotAffinities ->
            val affinities = if (timeSlotAffinities.isEmpty()) {
                // Fallback to global affinities if time-slot data is sparse
                interactionDao.getCategoryAffinities().first()
            } else {
                timeSlotAffinities
            }

            if (affinities.isEmpty()) {
                return@combine channels.filter { it.healthStatus == "ACTIVE" }
                    .sortedByDescending { it.priority }
                    .take(20)
            }

            val topCategories = affinities.take(3).map { it.category }
            
            channels.filter { it.healthStatus == "ACTIVE" }
                .sortedWith(compareByDescending<Channel> { 
                    if (topCategories.contains(it.category)) 1 else 0 
                }.thenByDescending { it.priority })
                .take(20)
        }
    }

    /**
     * Returns recommended movies based on favorite genres/affinities.
     */
    fun getRecommendedMovies(): Flow<List<Movie>> {
        return combine(
            movieDao.getAllMovies(),
            interactionDao.getCategoryAffinities()
        ) { movies, affinities ->
            if (affinities.isEmpty()) {
                return@combine movies.sortedByDescending { it.rating }.take(20)
            }

            val topCategories = affinities.map { it.category.lowercase() }
            
            movies.sortedWith(compareByDescending<Movie> { movie ->
                movie.genre.count { it.lowercase() in topCategories }
            }.thenByDescending { it.rating })
            .take(20)
        }
    }

    /**
     * Returns "Because you watched [Category]" recommendations.
     */
    suspend fun getRecommendationsForTopCategory(): Pair<String, List<Channel>> {
        val topAffinity = interactionDao.getCategoryAffinities().first().firstOrNull()
        return if (topAffinity != null) {
            val channels = channelDao.getAllChannels().first()
                .filter { it.category == topAffinity.category && it.healthStatus == "ACTIVE" }
                .shuffled()
                .take(10)
            topAffinity.category to channels
        } else {
            "" to emptyList()
        }
    }
}
