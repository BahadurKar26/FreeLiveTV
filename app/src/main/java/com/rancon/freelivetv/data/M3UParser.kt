package com.rancon.freelivetv.data

import android.util.Log

object M3UParser {
    private const val TAG = "M3UParser"

    fun parse(m3uContent: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        val lines = m3uContent.lines()
        var currentName = ""
        var currentLogo = ""
        var currentCategory = "General"
        var currentRegion = "Global"
        var currentId = ""

        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.startsWith("#EXTINF:")) {
                currentId = extractAttribute(line, "tvg-id")
                currentLogo = extractAttribute(line, "tvg-logo")
                val groupTitle = extractAttribute(line, "group-title")
                currentCategory = if (groupTitle.isNotBlank()) groupTitle else "General"
                
                currentRegion = if (groupTitle.contains("Bangla", ignoreCase = true) || line.contains("Bangla", ignoreCase = true)) {
                    "Bangla"
                } else {
                    "Global"
                }

                val commaIndex = line.lastIndexOf(',')
                currentName = if (commaIndex != -1) {
                    line.substring(commaIndex + 1).trim()
                } else {
                    "Unknown Channel"
                }

                i++
                while (i < lines.size) {
                    val nextLine = lines[i].trim()
                    if (nextLine.isEmpty() || (nextLine.startsWith("#") && !nextLine.startsWith("#EXTGRP:"))) {
                        if (nextLine.startsWith("#EXTGRP:")) {
                            val extGrp = nextLine.substringAfter(":").trim()
                            if (extGrp.isNotBlank()) currentCategory = extGrp
                        }
                        i++
                        continue
                    }
                    // Review 101: Support ftp:// for BDIX M3U lists
                    if (nextLine.startsWith("http", ignoreCase = true) || nextLine.startsWith("ftp", ignoreCase = true)) {
                        val id = currentId.ifBlank { "ch_${nextLine.hashCode()}" }
                        channels.add(Channel(
                            id = id,
                            name = if (currentName.isBlank()) "Channel ${channels.size}" else currentName,
                            urls = listOf(nextLine),
                            logo = currentLogo,
                            category = CategoryMapper.map(currentCategory), // Review 54: Apply category mapping during parse
                            region = currentRegion,
                            healthStatus = "ACTIVE"
                        ))
                        break
                    } else {
                        break
                    }
                }
            }
            i++
        }
        Log.d(TAG, "Parsed ${channels.size} channels from M3U")
        return channels
    }

    private fun extractAttribute(line: String, attribute: String): String {
        val patterns = listOf(
            "$attribute=\"([^\"]*)\"".toRegex(RegexOption.IGNORE_CASE),
            "$attribute='([^']*)'".toRegex(RegexOption.IGNORE_CASE),
            "$attribute=([^\\s,]+)".toRegex(RegexOption.IGNORE_CASE)
        )
        
        for (pattern in patterns) {
            val match = pattern.find(line)
            if (match != null) return match.groupValues[1].trim()
        }
        return ""
    }
}
