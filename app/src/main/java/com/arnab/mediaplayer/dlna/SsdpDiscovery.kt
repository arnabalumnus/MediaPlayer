package com.arnab.mediaplayer.dlna

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

private const val TAG = "SsdpDiscovery"
private const val SSDP_ADDRESS = "239.255.255.250"
private const val SSDP_PORT = 1900
private const val DISCOVERY_WINDOW_MS = 6000
private const val RECEIVE_POLL_MS = 500

object SsdpDiscovery {

    /**
     * Broadcasts SSDP M-SEARCH requests and returns any UPnP devices exposing an AVTransport
     * service (i.e. can be controlled as a renderer), regardless of how their root device is
     * typed - some TVs (Samsung's DMR included) expose the renderer as an embedded sub-device,
     * or label the root device in vendor-specific ways, so requiring the root `deviceType` to
     * literally contain "MediaRenderer" is too strict. SSDP over UDP is inherently lossy, so
     * the search is re-sent a few times across the discovery window rather than just once.
     */
    suspend fun discoverMediaRenderers(context: Context): List<DlnaDevice> = withContext(Dispatchers.IO) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val multicastLock = wifiManager?.createMulticastLock("dlna-discovery")?.apply {
            setReferenceCounted(true)
            acquire()
        }

        val locationUrls = mutableSetOf<String>()
        try {
            val socket = DatagramSocket().apply { soTimeout = RECEIVE_POLL_MS }
            val group = InetAddress.getByName(SSDP_ADDRESS)

            fun sendSearch() {
                for (searchTarget in listOf("ssdp:all", "urn:schemas-upnp-org:device:MediaRenderer:1")) {
                    val message = buildString {
                        append("M-SEARCH * HTTP/1.1\r\n")
                        append("HOST: $SSDP_ADDRESS:$SSDP_PORT\r\n")
                        append("MAN: \"ssdp:discover\"\r\n")
                        append("MX: 3\r\n")
                        append("ST: $searchTarget\r\n")
                        append("\r\n")
                    }.toByteArray()
                    socket.send(DatagramPacket(message, message.size, group, SSDP_PORT))
                }
            }

            val buffer = ByteArray(4096)
            val deadline = System.currentTimeMillis() + DISCOVERY_WINDOW_MS
            var nextResendAt = 0L
            while (System.currentTimeMillis() < deadline) {
                if (System.currentTimeMillis() >= nextResendAt) {
                    sendSearch()
                    nextResendAt = System.currentTimeMillis() + 2000
                }
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val response = String(packet.data, 0, packet.length, Charsets.UTF_8)
                    val location = parseLocationHeader(response)
                    Log.i(TAG, "SSDP response from ${packet.address?.hostAddress}: location=$location")
                    location?.let { locationUrls += it }
                } catch (e: IOException) {
                    // per-poll read timeout - loop again until the overall deadline
                }
            }
            socket.close()
        } catch (e: IOException) {
            Log.w(TAG, "SSDP discovery failed: ${e.message}")
        } finally {
            multicastLock?.let { if (it.isHeld) it.release() }
        }

        Log.i(TAG, "SSDP found ${locationUrls.size} candidate location(s): $locationUrls")
        locationUrls.mapNotNull { fetchRendererDescription(it) }
    }

    private fun parseLocationHeader(response: String): String? {
        val line = response.lineSequence().firstOrNull { it.startsWith("location:", ignoreCase = true) }
            ?: return null
        return line.substringAfter(":").trim().takeIf { it.isNotEmpty() }
    }

    /** Fetches a device's UPnP description XML and, if it exposes AVTransport, extracts what we need. */
    private fun fetchRendererDescription(locationUrl: String): DlnaDevice? {
        return try {
            val connection = (URL(locationUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 3000
                readTimeout = 3000
                requestMethod = "GET"
            }
            val xml = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val parser = Xml.newPullParser().apply {
                setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                setInput(xml.reader())
            }

            var friendlyName: String? = null
            var deviceType: String? = null
            var urlBase: String? = null
            var avTransportControlUrl: String? = null

            var currentTag = ""
            var inService = false
            var serviceType = ""
            var controlUrl = ""

            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                        if (currentTag == "service") {
                            inService = true
                            serviceType = ""
                            controlUrl = ""
                        }
                    }
                    XmlPullParser.TEXT -> {
                        val text = parser.text?.trim().orEmpty()
                        if (text.isNotEmpty()) {
                            when (currentTag) {
                                "friendlyName" -> if (friendlyName == null) friendlyName = text
                                "deviceType" -> if (deviceType == null) deviceType = text
                                "URLBase" -> if (urlBase == null) urlBase = text
                                "serviceType" -> if (inService) serviceType = text
                                "controlURL" -> if (inService) controlUrl = text
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "service") {
                            if (avTransportControlUrl == null && serviceType.contains("AVTransport")) {
                                avTransportControlUrl = controlUrl
                            }
                            inService = false
                        }
                        currentTag = ""
                    }
                }
                event = parser.next()
            }

            val transportPath = avTransportControlUrl
            Log.i(
                TAG,
                "Description at $locationUrl: friendlyName=$friendlyName deviceType=$deviceType " +
                    "avTransportControlUrl=$transportPath"
            )
            if (transportPath.isNullOrBlank()) return null

            val base = urlBase?.takeIf { it.isNotBlank() } ?: run {
                val u = URL(locationUrl)
                "${u.protocol}://${u.host}:${if (u.port > 0) u.port else u.defaultPort}"
            }

            DlnaDevice(
                friendlyName = friendlyName ?: "DLNA device",
                location = locationUrl,
                controlUrl = resolveUrl(base, transportPath)
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read device description at $locationUrl: ${e.message}")
            null
        }
    }

    private fun resolveUrl(base: String, path: String): String =
        if (path.startsWith("http://") || path.startsWith("https://")) {
            path
        } else {
            val cleanBase = base.trimEnd('/')
            val cleanPath = if (path.startsWith("/")) path else "/$path"
            "$cleanBase$cleanPath"
        }
}
