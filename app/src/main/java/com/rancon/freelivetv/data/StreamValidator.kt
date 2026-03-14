package com.rancon.freelivetv.data

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.random.Random

object StreamValidator {
    private const val TAG = "StreamValidator"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val userAgents = listOf(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
        "Mozilla/5.0 (Android 11; Mobile; rv:68.0) Gecko/68.0 Firefox/88.0",
        "FreeLiveTV-Android/2.0 (Official/Stable)",
        "ExoPlayerDemo/2.18.1 (Linux;Android 12) ExoPlayerLib/2.18.1",
        "Mozilla/5.0 (SmartHub; SMART-TV; Linux; Tizen 2.4.0) AppleWebkit/538.1 (KHTML, like Gecko) SamsungBrowser/1.1 TV Safari/538.1"
    )

    fun getRandomUserAgent(): String = userAgents[Random.nextInt(userAgents.size)]

    data class ValidationResult(
        val isAvailable: Boolean,
        val latencyMs: Long = -1,
        val bitrate: Double = 0.0,
        val isBdix: Boolean = false,
        val contentType: String? = null
    )

    suspend fun validate(url: String): ValidationResult {
        val startTime = System.currentTimeMillis()
        return try {
            val request = Request.Builder()
                .url(url)
                .addHeader("Range", "bytes=0-102400")
                .header("User-Agent", getRandomUserAgent())
                .build()

            client.newCall(request).execute().use { response ->
                val endTime = System.currentTimeMillis()
                val latency = endTime - startTime
                val isAvailable = response.isSuccessful || response.code == 206
                val contentType = response.header("Content-Type")
                
                var bitrate = 0.0
                if (isAvailable) {
                    val contentLength = response.body?.contentLength() ?: 0L
                    if (contentLength > 0 && latency > 0) {
                        bitrate = (contentLength * 8.0) / latency 
                    }
                }
                
                val isBdix = isLikelyBdix(url)
                
                Log.d(TAG, "Validated $url: status=${response.code}, latency=${latency}ms, bitrate=${bitrate}kbps, BDIX=$isBdix")
                
                ValidationResult(
                    isAvailable = isAvailable,
                    latencyMs = latency,
                    bitrate = bitrate,
                    isBdix = isBdix,
                    contentType = contentType
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Validation failed for $url: ${e.message}")
            ValidationResult(isAvailable = false)
        }
    }

    fun isLikelyBdix(url: String): Boolean {
        val bdixPatterns = listOf(
            ".p-cdn.live", "bdix", "local", "circut", "fptp",
            "103.114.171.", "103.102.253.", "103.239.253." // Common BD ISP ranges
        )
        return bdixPatterns.any { url.contains(it, ignoreCase = true) }
    }
}
