package com.rancon.freelivetv.data

import android.util.Log
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

object EpgParser {
    private const val TAG = "EpgParser"
    private val dateFormat = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US)

    fun parse(inputStream: InputStream): List<Program> {
        val programs = mutableListOf<Program>()
        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)

            var eventType = parser.eventType
            var currentProgram: ProgramBuilder? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val name = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (name == "programme") {
                            currentProgram = ProgramBuilder(
                                channelId = parser.getAttributeValue(null, "channel"),
                                startTime = parseDate(parser.getAttributeValue(null, "start")),
                                endTime = parseDate(parser.getAttributeValue(null, "stop"))
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
                            programs.add(currentProgram.build())
                            currentProgram = null
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing XMLTV: ${e.message}")
        }
        return programs
    }

    private fun parseDate(dateStr: String?): Long {
        if (dateStr == null) return 0L
        return try {
            dateFormat.parse(dateStr)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private class ProgramBuilder(
        val channelId: String,
        val startTime: Long,
        val endTime: Long,
        var title: String = "",
        var description: String = "",
        var category: String = "General"
    ) {
        fun build() = Program(
            channelId = channelId,
            title = title,
            description = description,
            startTime = startTime,
            endTime = endTime,
            category = category,
            episodeTitle = null,
            episodeNumber = null,
            seasonNumber = null,
            rating = null,
            isNew = false
        )
    }
}
