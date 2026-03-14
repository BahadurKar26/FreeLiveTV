package com.rancon.freelivetv.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object TmdbHelper {
    private const val TAG = "TmdbHelper"
    private const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    data class TmdbMetadata(
        val posterPath: String?,
        val backdropPath: String?,
        val rating: Double,
        val overview: String,
        val genres: List<String>,
        val cast: List<String> = emptyList(),
        val director: String = ""
    )

    // Phase 9.19: User-Agent rotation to avoid CDN blocking
    private val USER_AGENTS = listOf(
        "FreeLiveTV-Android/1.2",
        "Mozilla/5.0 (Linux; Android 10; SM-G973F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.106 Mobile Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
    )

    suspend fun fetchMetadata(title: String, type: String): TmdbMetadata? = withContext(Dispatchers.IO) {
        try {
            val endpoint = if (type == "movie") "movie" else "tv"
            val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
            val searchUrl = "${Config.TMDB_BASE_URL}/search/$endpoint?api_key=${Config.TMDB_API_KEY}&query=$encodedTitle"
            
            val searchRequest = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", USER_AGENTS.random())
                .build()

            client.newCall(searchRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext null
                    val json = JSONObject(body)
                    val results = json.getJSONArray("results")
                    if (results.length() > 0) {
                        val first = results.getJSONObject(0)
                        val id = first.getInt("id")
                        
                        // Fetch details with credits (Phase 1.4)
                        return@withContext fetchFullDetails(id, type) ?: TmdbMetadata(
                            posterPath = getImageUrl(first.optString("poster_path")),
                            backdropPath = getImageUrl(first.optString("backdrop_path")),
                            rating = first.optDouble("vote_average", 0.0),
                            overview = first.optString("overview", ""),
                            genres = emptyList()
                        )
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "TMDB fetch error for $title: ${e.message}")
            null
        }
    }

    private suspend fun fetchFullDetails(id: Int, type: String): TmdbMetadata? = withContext(Dispatchers.IO) {
        try {
            val endpoint = if (type == "movie") "movie" else "tv"
            val url = "${Config.TMDB_BASE_URL}/$endpoint/$id?api_key=${Config.TMDB_API_KEY}&append_to_response=credits"
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENTS.random())
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext null
                    val json = JSONObject(body)
                    
                    val credits = json.optJSONObject("credits")
                    val castList = mutableListOf<String>()
                    val castArray = credits?.optJSONArray("cast")
                    if (castArray != null) {
                        for (i in 0 until minOf(castArray.length(), 5)) {
                            castList.add(castArray.getJSONObject(i).optString("name"))
                        }
                    }

                    var directorName = ""
                    if (type == "movie") {
                        val crewArray = credits?.optJSONArray("crew")
                        if (crewArray != null) {
                            for (i in 0 until crewArray.length()) {
                                val person = crewArray.getJSONObject(i)
                                if (person.optString("job") == "Director") {
                                    directorName = person.optString("name")
                                    break
                                }
                            }
                        }
                    } else {
                        val createdBy = json.optJSONArray("created_by")
                        if (createdBy != null && createdBy.length() > 0) {
                            directorName = createdBy.getJSONObject(0).optString("name")
                        }
                    }

                    val genres = mutableListOf<String>()
                    val genresArray = json.optJSONArray("genres")
                    if (genresArray != null) {
                        for (i in 0 until genresArray.length()) {
                            genres.add(genresArray.getJSONObject(i).optString("name"))
                        }
                    }

                    return@withContext TmdbMetadata(
                        posterPath = getImageUrl(json.optString("poster_path")),
                        backdropPath = getImageUrl(json.optString("backdrop_path")),
                        rating = json.optDouble("vote_average", 0.0),
                        overview = json.optString("overview", ""),
                        genres = genres,
                        cast = castList,
                        director = directorName
                    )
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "TMDB details fetch error for $id: ${e.message}")
            null
        }
    }

    private fun getImageUrl(path: String?): String? {
        return if (path != null && path != "null" && path.isNotBlank()) {
            IMAGE_BASE_URL + path
        } else null
    }
}
