package com.rancon.freelivetv.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class EpgRepository(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val programDao = database.programDao()
    private val channelDao = database.channelDao()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "EpgRepository"
    }

    suspend fun refreshEpg(epgUrl: String = Config.EPG_URL) = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Fetching EPG from $epgUrl")
            val request = Request.Builder().url(epgUrl).build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val xml = response.body?.string() ?: return@withContext
                    val rawPrograms = parseXmlTv(xml)
                    if (rawPrograms.isNotEmpty()) {
                        // Review 41: Smart Normalized Matching
                        val matchedPrograms = rawPrograms.mapNotNull { raw ->
                            findChannelIdForEpg(raw.channelId)?.let { realId ->
                                raw.copy(channelId = realId)
                            }
                        }
                        
                        programDao.cleanupOldPrograms(System.currentTimeMillis() - 86400000)
                        programDao.insertAll(matchedPrograms)
                        Log.i(TAG, "Stored ${matchedPrograms.size} matched EPG programs")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing EPG: ${e.message}")
        }
    }

    /**
     * Review 41: Fuzzy logic to match EPG channel IDs with local DB channel names/ids.
     * Prevents empty "Now Playing" titles due to naming discrepancies.
     */
    private suspend fun findChannelIdForEpg(epgId: String): String? {
        val allLocalChannels = database.channelDao().getAllChannelsSync() // New DAO method needed
        
        // 1. Direct ID match
        allLocalChannels.find { it.id == epgId || it.epgId == epgId }?.let { return it.id }
        
        // 2. Normalized Name match (Case-insensitive, trim whitespace)
        val normalizedEpgId = epgId.lowercase().replace(" ", "")
        allLocalChannels.find { 
            it.name.lowercase().replace(" ", "") == normalizedEpgId 
        }?.let { return it.id }
        
        return null
    }

    private fun parseXmlTv(xml: String): List<Program> {
        val programs = mutableListOf<Program>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            // Review 44: Enhanced date format to handle varied timezone offsets
            val dateFormat = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US)
            var eventType = parser.eventType
            var currentProgram: ProgramBuilder? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val name = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (name == "programme") {
                            val startStr = parser.getAttributeValue(null, "start")
                            val stopStr = parser.getAttributeValue(null, "stop")
                            currentProgram = ProgramBuilder(
                                channelId = parser.getAttributeValue(null, "channel") ?: "",
                                startTime = dateFormat.parse(startStr)?.time ?: 0L,
                                endTime = dateFormat.parse(stopStr)?.time ?: 0L
                            )
                        } else if (currentProgram != null) {
                            when (name) {
                                "title" -> currentProgram.title = parser.nextText()
                                "desc" -> currentProgram.description = parser.nextText()
                                "category" -> currentProgram.category = parser.nextText()
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (name == "programme" && currentProgram != null) {
                            programs.add(currentProgram.toProgram())
                            currentProgram = null
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "XMLTV Parsing error: ${e.message}")
        }
        return programs
    }

    private class ProgramBuilder(
        val channelId: String,
        val startTime: Long,
        val endTime: Long,
        var title: String = "No Title",
        var description: String = "",
        var category: String = "General"
    ) {
        fun toProgram() = Program(
            channelId = channelId,
            title = title,
            description = description,
            startTime = startTime,
            endTime = endTime,
            category = category
        )
    }
}
