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

class SeriesRepository(private val context: Context, private val scope: CoroutineScope) {

    private val database = AppDatabase.getDatabase(context)
    private val seriesDao = database.seriesDao()

    val allSeries: Flow<List<Series>> = seriesDao.getAllSeries()
    val continueWatchingEpisodes: Flow<List<Episode>> = seriesDao.getContinueWatchingEpisodes()

    private val httpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "SeriesRepository"
        private const val SERIES_FILE = "series.json"
        private const val MAX_ENRICH_COUNT = 50
    }

    init {
        scope.launch {
            if (seriesDao.getAllSeries().first().isEmpty()) {
                seedFromAssets()
            }
        }
    }

    private suspend fun seedFromAssets() {
        withContext(Dispatchers.IO) {
            try {
                val json = context.assets.open(SERIES_FILE).bufferedReader().use { it.readText() }
                val (series, episodes) = parseSeriesJson(json)
                if (series.isNotEmpty()) {
                    seriesDao.insertAll(series)
                    seriesDao.insertEpisodes(episodes)
                    Log.i(TAG, "Seeded ${series.size} series from assets")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error seeding series: ${e.message}")
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

    suspend fun refreshSeries() {
        withContext(Dispatchers.IO) {
            try {
                val json = retryRequest {
                    val request = okhttp3.Request.Builder()
                        .url(Config.SERIES_URL)
                        .header("User-Agent", "FreeLiveTV-Android/1.0")
                        .build()
                    httpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw Exception("Unexpected code $response")
                        response.body?.string()
                    }
                }

                if (json != null) {
                    val remoteSeriesResult = parseSeriesJson(json)
                    val remoteSeries = remoteSeriesResult.first
                    val remoteEpisodes = remoteSeriesResult.second
                    
                    if (remoteSeries.isNotEmpty()) {
                        // Optimized Enrichment Phase (QC-005 / Phase 9.18)
                        val semaphore = Semaphore(3) 
                        val enrichedSeries = mutableListOf<Series>()
                        
                        // Limit enrichment to first 50 items to save resources
                        val seriesToEnrich = remoteSeries.take(MAX_ENRICH_COUNT)
                        val remainingSeries = remoteSeries.drop(MAX_ENRICH_COUNT)

                        seriesToEnrich.chunked(10).forEach { chunk ->
                            val deferred = chunk.map { series ->
                                async {
                                    semaphore.withPermit {
                                        if (series.posterUrl.isBlank() || series.description.isBlank() || series.cast.isEmpty()) {
                                            val metadata = TmdbHelper.fetchMetadata(series.title, "tv")
                                            if (metadata != null) {
                                                series.copy(
                                                    posterUrl = metadata.posterPath ?: series.posterUrl,
                                                    backdropUrl = metadata.backdropPath ?: series.backdropUrl,
                                                    description = if (series.description.isBlank()) metadata.overview else series.description,
                                                    rating = if (series.rating == 0.0) metadata.rating else series.rating,
                                                    cast = if (series.cast.isEmpty()) metadata.cast else series.cast
                                                )
                                            } else series
                                        } else series
                                    }
                                }
                            }
                            enrichedSeries.addAll(deferred.awaitAll())
                            yield()
                        }
                        
                        upsertSeries(enrichedSeries + remainingSeries)
                        upsertEpisodes(remoteEpisodes)
                        Log.i(TAG, "Synced ${remoteSeries.size} series (enriched $MAX_ENRICH_COUNT)")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync series after retries: ${e.message}")
            }
            Unit
        }
    }

    private suspend fun upsertSeries(newSeries: List<Series>) = withContext(Dispatchers.IO) {
        val existingSeries = seriesDao.getAllSeries().first().associateBy { it.id }
        val toUpdate = newSeries.map { s ->
            val existing = existingSeries[s.id]
            if (existing != null) {
                s.copy(isFavorite = existing.isFavorite, lastPlayedTime = existing.lastPlayedTime)
            } else s
        }
        
        toUpdate.chunked(50).forEach { chunk -> 
            seriesDao.insertAll(chunk)
            yield()
        }
        Unit
    }

    private suspend fun upsertEpisodes(newEpisodes: List<Episode>) = withContext(Dispatchers.IO) {
        val existingEpisodes = seriesDao.getContinueWatchingEpisodes().first().associateBy { it.id }
        val episodesToInsert = newEpisodes.map { ep ->
            val existing = existingEpisodes[ep.id]
            if (existing != null) {
                ep.copy(
                    lastPlayedPosition = existing.lastPlayedPosition,
                    lastPlayedTime = existing.lastPlayedTime,
                    isWatched = existing.isWatched
                )
            } else ep
        }
        
        episodesToInsert.chunked(50).forEach { chunk ->
            seriesDao.insertEpisodes(chunk)
            yield()
        }
        Unit
    }

    suspend fun updateEpisodeProgress(episodeId: String, position: Long) {
        seriesDao.updateEpisodeProgress(episodeId, position, System.currentTimeMillis())
    }

    private fun parseSeriesJson(jsonString: String): Pair<List<Series>, List<Episode>> {
        val seriesList = mutableListOf<Series>()
        val episodeList = mutableListOf<Episode>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val seriesId = obj.optString("id", "s_$i")
                
                val genres = mutableListOf<String>()
                val genreArray = obj.optJSONArray("genre") ?: obj.optJSONArray("genres")
                if (genreArray != null) {
                    for (j in 0 until genreArray.length()) {
                        genres.add(genreArray.getString(j))
                    }
                }

                val seasonsArray = obj.optJSONArray("seasons")
                if (seasonsArray != null) {
                    for (j in 0 until seasonsArray.length()) {
                        val seasonObj = seasonsArray.getJSONObject(j)
                        val seasonNumber = seasonObj.optInt("number", seasonObj.optInt("seasonNumber", j + 1))
                        val episodesArray = seasonObj.optJSONArray("episodes")
                        if (episodesArray != null) {
                            for (k in 0 until episodesArray.length()) {
                                val epObj = episodesArray.getJSONObject(k)
                                episodeList.add(Episode(
                                    id = epObj.optString("id", "ep_${seriesId}_${seasonNumber}_${k}"),
                                    seriesId = seriesId,
                                    title = epObj.optString("title", "Episode ${k + 1}"),
                                    seasonNumber = seasonNumber,
                                    episodeNumber = epObj.optInt("episodeNumber", epObj.optInt("number", k + 1)),
                                    streamUrl = epObj.optString("streamUrl", epObj.optString("url", "")),
                                    duration = epObj.optInt("duration", 0),
                                    description = epObj.optString("description", ""),
                                    thumbnailUrl = epObj.optString("thumbnailUrl", epObj.optString("poster", ""))
                                ))
                            }
                        }
                    }
                }

                val castList = mutableListOf<String>()
                val castArray = obj.optJSONArray("cast")
                if (castArray != null) {
                    for (j in 0 until castArray.length()) {
                        castList.add(castArray.getString(j))
                    }
                }

                seriesList.add(Series(
                    id = seriesId,
                    title = obj.optString("title", "Unknown Series"),
                    posterUrl = obj.optString("posterUrl", obj.optString("poster", "")),
                    backdropUrl = obj.optString("backdropUrl", obj.optString("backdrop", "")),
                    genre = genres,
                    year = obj.optInt("year", 0),
                    // Phase 9.15: Fix JSON parsing crash risk
                    endYear = if (obj.has("endYear")) obj.optInt("endYear", -1).let { if (it == -1) null else it } else null,
                    rating = obj.optDouble("rating", 0.0),
                    description = obj.optString("description", ""),
                    language = obj.optString("language", "English"),
                    country = obj.optString("country", "Global"),
                    cast = castList
                ))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing series JSON: ${e.message}")
        }
        return Pair(seriesList, episodeList)
    }

    fun getEpisodesForSeries(seriesId: String): Flow<List<Episode>> = seriesDao.getEpisodesForSeries(seriesId)
    
    suspend fun getEpisodeById(id: String): Episode? = seriesDao.getEpisodeById(id)
}
