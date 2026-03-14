package com.rancon.freelivetv.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.random.Random
import kotlin.system.measureTimeMillis

/**
 * Phase 10.5: Local network scanner to discover BDIX FTP servers.
 * Updated in Round 12 with Randomized Start Offset (Review 203).
 */
class LocalNetworkScanner(private val context: Context) {

    private val TAG = "LocalNetworkScanner"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val portsToScan = listOf(21, 80, 8080, 2121)
    private val semaphore = Semaphore(5)

    fun startScan(onServerFound: (String, Long) -> Unit) {
        if (!NetworkUtils.isWifiConnected(context)) return

        scope.launch {
            // Review 203: Random jitter to prevent router DDoS flags on app launch
            delay(Random.nextLong(500, 5000))
            
            val subnet = getSubnet() ?: return@launch
            Log.i(TAG, "Starting randomized latency scan on subnet: $subnet.0/24")

            val scanJobs = (1..254).map { i ->
                launch {
                    semaphore.withPermit {
                        val host = "$subnet.$i"
                        portsToScan.forEach { port ->
                            val latency = checkPortAndLatency(host, port)
                            if (latency >= 0) {
                                onServerFound("ftp://$host:$port", latency)
                            }
                        }
                    }
                }
            }
            scanJobs.joinAll()
        }
    }

    private fun getSubnet(): String? {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val linkProperties: LinkProperties = connectivityManager.getLinkProperties(connectivityManager.activeNetwork) ?: return null
            for (address in linkProperties.linkAddresses) {
                val ip = address.address.hostAddress
                if (ip != null && ip.contains(".")) return ip.substringBeforeLast(".")
            }
        } catch (e: Exception) { }
        return null
    }

    private suspend fun checkPortAndLatency(host: String, port: Int): Long = withContext(Dispatchers.IO) {
        try {
            val time = measureTimeMillis {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), 250) // Tightened 250ms timeout
                }
            }
            return@withContext time
        } catch (e: Exception) {
            return@withContext -1L
        }
    }

    fun stopScan() {
        scope.cancel()
    }
}
