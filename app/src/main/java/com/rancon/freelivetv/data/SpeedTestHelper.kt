package com.rancon.freelivetv.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object SpeedTestHelper {
    private const val TAG = "SpeedTestHelper"
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // Multiple test nodes for more realistic average (P4-004)
    private val TEST_NODES = listOf(
        "https://raw.githubusercontent.com/bahadurkar/freelivetv-channels/main/test_file_1mb.bin",
        "https://speed.cloudflare.com/__down?bytes=1048576"
    )

    data class SpeedTestResult(
        val mbps: Double,
        val latencyMs: Long,
        val status: String
    )

    suspend fun runTest(): SpeedTestResult = withContext(Dispatchers.IO) {
        var totalMbps = 0.0
        var totalLatency = 0L
        var successCount = 0

        for (url in TEST_NODES) {
            try {
                // Strict timeout to prevent UI hang (P4-002)
                withTimeout(15000) {
                    val startTime = System.currentTimeMillis()
                    val request = Request.Builder().url(url).build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) return@withTimeout
                        
                        val body = response.body ?: return@withTimeout
                        val source = body.source()
                        val buffer = ByteArray(8192)
                        var bytesReadTotal = 0L
                        
                        while (true) {
                            val read = source.read(buffer)
                            if (read == -1) break
                            bytesReadTotal += read
                            // Limit to 1MB to keep test fast
                            if (bytesReadTotal >= 1024 * 1024) break
                        }
                        
                        val endTime = System.currentTimeMillis()
                        val durationMs = (endTime - startTime).coerceAtLeast(1)
                        val durationSec = durationMs / 1000.0
                        val mbps = (bytesReadTotal * 8.0 / 1024 / 1024) / durationSec
                        
                        totalMbps += mbps
                        totalLatency += durationMs
                        successCount++
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Speed test failed for $url: ${e.message}")
            }
        }

        if (successCount > 0) {
            val avgMbps = totalMbps / successCount
            val avgLatency = totalLatency / successCount
            val status = when {
                avgMbps > 10 -> "EXCELLENT"
                avgMbps > 5 -> "GOOD"
                avgMbps > 2 -> "FAIR"
                else -> "POOR"
            }
            SpeedTestResult(avgMbps, avgLatency, status)
        } else {
            SpeedTestResult(0.0, -1, "FAILED")
        }
    }
}
