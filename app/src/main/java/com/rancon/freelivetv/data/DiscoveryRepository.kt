package com.rancon.freelivetv.data

import android.content.Context
import android.os.StatFs
import android.util.Log
import androidx.room.withTransaction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.*
import okhttp3.dnsoverhttps.DnsOverHttps
import org.json.JSONArray
import org.json.JSONObject
import java.net.InetAddress
import java.util.concurrent.TimeUnit

/**
 * Optimized Discovery Repository (Elite Cycle 7 - DNS & Storage Safety)
 */
class DiscoveryRepository(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val channelDao = database.channelDao()
    private val movieDao = database.movieDao()
    private val seriesDao = database.seriesDao()
    private val sourceDao = database.sourceDao()
    
    // Review 403: DnsOverHttps to prevent BDIX-level DNS hijacking
    private val appDns = DnsOverHttps.Builder()
        .client(OkHttpClient())
        .url(HttpUrl.get("https://dns.google/dns-query"))
        .bootstrapDnsHosts(listOf(InetAddress.getByName("8.8.8.8"), InetAddress.getByName("8.8.4.4")))
        .build()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .dns(appDns)
        .connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES))
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Accept-Encoding", "gzip")
                .header("User-Agent", "FreeLiveTV-Elite/2.2")
                .build()
            chain.proceed(request)
        }
        .build()

    companion object {
        private const val TAG = "DiscoveryRepository"
        private const val MAX_ENRICH_COUNT = 50
        private val DISCOVERY_SEMAPHORE = Semaphore(3)
    }

    private fun getAvailableStorageMB(): Long {
        val stat = StatFs(context.filesDir.path)
        return (stat.availableBlocksLong * stat.blockSizeLong) / (1024 * 1024)
    }

    suspend fun performDiscovery() = withContext(Dispatchers.IO) {
        // Review 410: Hardware check before sync
        if (getAvailableStorageMB() < 200) {
            Log.e(TAG, "Sync aborted: Critical storage level")
            return@withContext
        }

        initSources()
        val sources = sourceDao.getAllSources().first().filter { it.isActive }
        
        val discoveryJobs = sources.map { source ->
            async {
                DISCOVERY_SEMAPHORE.withPermit {
                    try {
                        withTimeout(45000) {
                            val request = Request.Builder().url(source.url).build()
                            httpClient.newCall(request).execute().use { response ->
                                if (response.isSuccessful) {
                                    val body = response.body?.string() ?: return@use
                                    processSourceBody(body, source)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Discovery failed for ${source.name}: ${e.message}")
                    }
                }
            }
        }
        discoveryJobs.awaitAll()
        
        discoverArchiveSeries()
        discoverArchiveMovies()
    }

    private suspend fun processSourceBody(body: String, source: SourceConfig) {
        when (source.category) {
            "Movies" -> {
                val discoveredMovies = parseJsonMovies(body)
                if (discoveredMovies.isNotEmpty()) upsertMovies(discoveredMovies)
            }
            "Series" -> {
                val discoveredSeries = parseJsonSeries(body)
                if (discoveredSeries.first.isNotEmpty()) {
                    upsertSeries(discoveredSeries.first)
                    seriesDao.insertEpisodes(discoveredSeries.second)
                }
            }
            else -> {
                val discoveredChannels = when (source.type) {
                    "M3U" -> M3UParser.parse(body)
                    "JSON" -> parseJsonChannels(body)
                    else -> emptyList()
                }
                if (discoveredChannels.isNotEmpty()) {
                    mergeAndRankChannels(discoveredChannels, source.category)
                }
            }
        }
    }

    suspend fun initSources() = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(Config.SOURCES_URL).build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val json = response.body?.string()
                    if (json != null) {
                        val type = object : TypeToken<List<SourceConfig>>() {}.type
                        val sources: List<SourceConfig> = Gson().fromJson(json, type)
                        sourceDao.insertSources(sources)
                    }
                }
            }
        } catch (e: Exception) { }
    }

    private suspend fun discoverArchiveSeries() {
        try {
            val request = Request.Builder().url(Config.ARCHIVE_SERIES_URL).build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return
                    val docs = JSONObject(body).getJSONObject("response").getJSONArray("docs")
                    val seriesList = mutableListOf<Series>()
                    for (i in 0 until docs.length()) {
                        val doc = docs.getJSONObject(i)
                        seriesList.add(Series(
                            id = "archive_${doc.optString("identifier")}",
                            title = doc.optString("title"),
                            posterUrl = "https://archive.org/services/img/${doc.optString("identifier")}",
                            description = doc.optString("description"),
                            year = doc.optInt("year", 0),
                            genre = listOf("Public Domain")
                        ))
                    }
                    upsertSeries(seriesList)
                }
            }
        } catch (e: Exception) { }
    }

    private suspend fun discoverArchiveMovies() {
        try {
            val request = Request.Builder().url(Config.ARCHIVE_MOVIES_URL).build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return
                    val docs = JSONObject(body).getJSONObject("response").getJSONArray("docs")
                    val moviesList = mutableListOf<Movie>()
                    for (i in 0 until docs.length()) {
                        val doc = docs.getJSONObject(i)
                        val id = doc.optString("identifier")
                        moviesList.add(Movie(
                            id = "archive_$id",
                            title = doc.optString("title"),
                            streamUrl = "https://archive.org/download/$id/$id.mp4",
                            posterUrl = "https://archive.org/services/img/$id",
                            description = doc.optString("description"),
                            year = doc.optInt("year", 0),
                            genre = listOf("Public Domain"),
                            duration = 0,
                            rating = 0.0
                        ))
                    }
                    upsertMovies(moviesList)
                }
            }
        } catch (e: Exception) { }
    }

    private fun parseJsonMovies(jsonString: String): List<Movie> {
        val movies = mutableListOf<Movie>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                movies.add(Movie(
                    id = obj.optString("id", "m_${i}_${obj.optString("title").hashCode()}"),
                    title = obj.optString("title", "Unknown"),
                    streamUrl = obj.optString("url"),
                    posterUrl = obj.optString("poster", ""),
                    backdropUrl = obj.optString("backdrop", ""),
                    description = obj.optString("description", ""),
                    year = obj.optInt("year", 0),
                    genre = listOf(obj.optString("category", "General")),
                    duration = 0,
                    rating = 0.0
                ))
            }
        } catch (e: Exception) { }
        return movies
    }

    private fun parseJsonSeries(jsonString: String): Pair<List<Series>, List<Episode>> {
        val seriesList = mutableListOf<Series>()
        val episodeList = mutableListOf<Episode>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val sid = obj.optString("id", "s_$i")
                seriesList.add(Series(
                    id = sid,
                    title = obj.optString("title"),
                    posterUrl = obj.optString("poster", ""),
                    genre = listOf("General"),
                    year = obj.optInt("year"),
                    rating = 0.0,
                    description = ""
                ))
            }
        } catch (e: Exception) { }
        return seriesList to episodeList
    }

    private suspend fun upsertMovies(newMovies: List<Movie>) = withContext(Dispatchers.IO) {
        val existing = movieDao.getAllMovies().first().associateBy { it.id }
        val toUpdate = newMovies.map { movie ->
            val ext = existing[movie.id]
            if (ext != null) movie.copy(isFavorite = ext.isFavorite, lastPlayedPosition = ext.lastPlayedPosition, lastPlayedTime = ext.lastPlayedTime)
            else movie
        }
        
        val enriched = mutableListOf<Movie>()
        val toEnrich = toUpdate.take(MAX_ENRICH_COUNT)
        val remaining = toUpdate.drop(MAX_ENRICH_COUNT)
        
        toEnrich.chunked(10).forEach { chunk ->
            val deferred = chunk.map { movie ->
                async {
                    if (movie.posterUrl.isBlank() || movie.description.isBlank()) {
                        val metadata = TmdbHelper.fetchMetadata(movie.title, "movie")
                        if (metadata != null) {
                            movie.copy(
                                posterUrl = metadata.posterPath ?: movie.posterUrl,
                                backdropUrl = metadata.backdropPath ?: movie.backdropUrl,
                                description = metadata.overview,
                                rating = metadata.rating,
                                cast = metadata.cast,
                                director = metadata.director
                            )
                        } else movie
                    } else movie
                }
            }
            enriched.addAll(deferred.awaitAll())
            yield()
        }

        (enriched + remaining).chunked(50).forEach { chunk ->
            movieDao.insertAll(chunk)
            yield()
        }
    }

    private suspend fun upsertSeries(newSeries: List<Series>) = withContext(Dispatchers.IO) {
        val existing = seriesDao.getAllSeries().first().associateBy { it.id }
        val toUpdate = newSeries.map { s ->
            val ext = existing[s.id]
            if (ext != null) s.copy(isFavorite = ext.isFavorite, lastPlayedTime = ext.lastPlayedTime)
            else s
        }

        val enriched = mutableListOf<Series>()
        val toEnrich = toUpdate.take(MAX_ENRICH_COUNT)
        val remaining = toUpdate.drop(MAX_ENRICH_COUNT)
        
        toEnrich.chunked(10).forEach { chunk ->
            val deferred = chunk.map { s ->
                async {
                    if (s.posterUrl.isBlank()) {
                        val metadata = TmdbHelper.fetchMetadata(s.title, "tv")
                        if (metadata != null) {
                            s.copy(
                                posterUrl = metadata.posterPath ?: s.posterUrl,
                                backdropUrl = metadata.backdropPath ?: s.backdropUrl,
                                description = metadata.overview,
                                rating = metadata.rating,
                                cast = metadata.cast
                            )
                        } else s
                    } else s
                }
            }
            enriched.addAll(deferred.awaitAll())
            yield()
        }

        (enriched + remaining).chunked(50).forEach { chunk ->
            seriesDao.insertAll(chunk)
            yield()
        }
    }

    private fun parseJsonChannels(jsonString: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val url = obj.optString("url")
                if (url.isNotBlank()) {
                    channels.add(Channel(
                        id = obj.optString("id", "ch_${url.hashCode()}"),
                        name = obj.optString("name", "Unknown"),
                        urls = listOf(url),
                        logo = obj.optString("logo", ""),
                        category = obj.optString("category", "General"),
                        language = obj.optString("language", "English"),
                        region = obj.optString("region", "Global"),
                        epgId = obj.optString("epgId", "")
                    ))
                }
            }
        } catch (e: Exception) { }
        return channels
    }

    private suspend fun mergeAndRankChannels(newChannels: List<Channel>, defaultCategory: String) {
        newChannels.chunked(100).forEach { chunk ->
            val ids = chunk.map { it.id }
            val existingInDb = channelDao.getAllChannelsSync().filter { it.id in ids }.associateBy { it.id }
            val toUpdate = chunk.map { channel ->
                val existing = existingInDb[channel.id]
                if (existing != null) {
                    existing.copy(
                        urls = (existing.urls + channel.urls).distinct(),
                        category = if (existing.category == "General") defaultCategory else existing.category,
                        epgId = if (existing.epgId.isNullOrBlank()) channel.epgId else existing.epgId
                    )
                } else {
                    channel.copy(category = if (channel.category == "General") defaultCategory else channel.category)
                }
            }
            database.withTransaction { channelDao.insertAll(toUpdate) }
            yield()
        }
    }
}
