package com.rancon.freelivetv.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.json.JSONArray
import java.util.concurrent.TimeUnit

class MoviesRepository(private val context: Context, private val scope: CoroutineScope) {

    private val database = AppDatabase.getDatabase(context)
    private val movieDao = database.movieDao()

    val allMovies: Flow<List<Movie>> = movieDao.getAllMovies()
    val favoriteMovies: Flow<List<Movie>> = movieDao.getFavoriteMovies()
    val continueWatching: Flow<List<Movie>> = movieDao.getContinueWatching()

    private val httpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "MoviesRepository"
        private const val MOVIES_FILE = "movies.json"
        private const val MAX_ENRICH_COUNT = 50
    }

    init {
        scope.launch {
            if (movieDao.getAllMovies().first().isEmpty()) {
                seedFromAssets()
            }
        }
    }

    private suspend fun seedFromAssets() {
        withContext(Dispatchers.IO) {
            try {
                val json = context.assets.open(MOVIES_FILE).bufferedReader().use { it.readText() }
                val movies = parseMoviesJson(json)
                if (movies.isNotEmpty()) {
                    movieDao.insertAll(movies)
                    Log.i(TAG, "Seeded ${movies.size} movies from assets")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error seeding movies: ${e.message}")
            }
            Unit
        }
    }

    private suspend fun <T> retryRequest(
        times: Int = 3,
        initialDelay: Long = 1000,
        maxDelay: Long = 5000,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        repeat(times - 1) {
            try {
                return block()
            } catch (e: Exception) {
                Log.w(TAG, "Request failed, retrying in $currentDelay ms... (${e.message})")
            }
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
        return block()
    }

    suspend fun refreshMovies() {
        withContext(Dispatchers.IO) {
            try {
                val json = retryRequest {
                    val request = okhttp3.Request.Builder()
                        .url(Config.MOVIES_URL)
                        .header("User-Agent", "FreeLiveTV-Android/1.0")
                        .build()
                    httpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw Exception("Unexpected code $response")
                        response.body?.string()
                    }
                }

                if (json != null) {
                    val remoteMovies = parseMoviesJson(json)
                    if (remoteMovies.isNotEmpty()) {
                        // Optimized Enrichment Phase (QC-005 / Phase 9.18)
                        val semaphore = Semaphore(3) 
                        val enrichedMovies = mutableListOf<Movie>()
                        
                        // Limit enrichment to first 50 items
                        val moviesToEnrich = remoteMovies.take(MAX_ENRICH_COUNT)
                        val remainingMovies = remoteMovies.drop(MAX_ENRICH_COUNT)

                        moviesToEnrich.chunked(10).forEach { chunk ->
                            val deferred = chunk.map { movie ->
                                async {
                                    semaphore.withPermit {
                                        if (movie.posterUrl.isBlank() || movie.description.isBlank() || movie.cast.isEmpty()) {
                                            val metadata = TmdbHelper.fetchMetadata(movie.title, "movie")
                                            if (metadata != null) {
                                                movie.copy(
                                                    posterUrl = metadata.posterPath ?: movie.posterUrl,
                                                    backdropUrl = metadata.backdropPath ?: movie.backdropUrl,
                                                    description = if (movie.description.isBlank()) metadata.overview else movie.description,
                                                    rating = if (movie.rating == 0.0) metadata.rating else movie.rating,
                                                    cast = if (movie.cast.isEmpty()) metadata.cast else movie.cast,
                                                    director = if (movie.director.isBlank()) metadata.director else movie.director
                                                )
                                            } else movie
                                        } else movie
                                    }
                                }
                            }
                            enrichedMovies.addAll(deferred.awaitAll())
                            yield()
                        }
                        
                        upsertMovies(enrichedMovies + remainingMovies)
                        Log.i(TAG, "Synced ${remoteMovies.size} movies (enriched $MAX_ENRICH_COUNT)")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync movies after retries: ${e.message}")
            }
            Unit
        }
    }

    private suspend fun upsertMovies(newMovies: List<Movie>) {
        withContext(Dispatchers.IO) {
            val existingMovies = movieDao.getAllMovies().first().associateBy { it.id }
            val toUpdate = newMovies.map { movie ->
                val existing = existingMovies[movie.id]
                if (existing != null) {
                    movie.copy(
                        isFavorite = existing.isFavorite,
                        lastPlayedPosition = existing.lastPlayedPosition,
                        lastPlayedTime = existing.lastPlayedTime
                    )
                } else {
                    movie
                }
            }
            
            toUpdate.chunked(50).forEach { chunk ->
                movieDao.insertAll(chunk)
                yield()
            }
            Unit
        }
    }

    private fun parseMoviesJson(jsonString: String): List<Movie> {
        val movieList = mutableListOf<Movie>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                
                val genres = mutableListOf<String>()
                val genreArray = obj.optJSONArray("genre") ?: obj.optJSONArray("genres")
                if (genreArray != null) {
                    for (j in 0 until genreArray.length()) {
                        genres.add(genreArray.getString(j))
                    }
                }

                val castList = mutableListOf<String>()
                val castArray = obj.optJSONArray("cast")
                if (castArray != null) {
                    for (j in 0 until castArray.length()) {
                        castList.add(castArray.getString(j))
                    }
                }

                movieList.add(Movie(
                    id = obj.optString("id", "m_$i"),
                    title = obj.optString("title", "Unknown Movie"),
                    posterUrl = obj.optString("posterUrl", obj.optString("poster", "")),
                    backdropUrl = obj.optString("backdropUrl", obj.optString("backdrop", "")),
                    streamUrl = obj.optString("streamUrl", obj.optString("url", "")),
                    genre = genres,
                    year = obj.optInt("year", 0),
                    duration = obj.optInt("duration", 0),
                    rating = obj.optDouble("rating", 0.0),
                    description = obj.optString("description", ""),
                    language = obj.optString("language", "English"),
                    country = obj.optString("country", "Global"),
                    isNew = obj.optBoolean("isNew", false),
                    cast = castList,
                    director = obj.optString("director", "")
                ))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing movies JSON: ${e.message}")
        }
        return movieList
    }
}
