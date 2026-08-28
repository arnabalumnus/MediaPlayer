package com.arnab.mediaplayer.dlna

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val TAG = "AvTransportClient"
private const val AV_TRANSPORT_SERVICE_TYPE = "urn:schemas-upnp-org:service:AVTransport:1"

data class DlnaPositionInfo(val positionMs: Long, val durationMs: Long)

/** Sends UPnP AVTransport SOAP actions (the "remote control" calls) to a [DlnaDevice]'s controlUrl. */
object AvTransportClient {

    suspend fun setAvTransportUri(controlUrl: String, mediaUrl: String, title: String, mimeType: String) {
        val didl = buildDidlLite(mediaUrl, title, mimeType)
        sendAction(
            controlUrl, "SetAVTransportURI",
            "<InstanceID>0</InstanceID>" +
                "<CurrentURI>${xmlEscape(mediaUrl)}</CurrentURI>" +
                "<CurrentURIMetaData>${xmlEscape(didl)}</CurrentURIMetaData>"
        )
    }

    suspend fun play(controlUrl: String) {
        sendAction(controlUrl, "Play", "<InstanceID>0</InstanceID><Speed>1</Speed>")
    }

    suspend fun pause(controlUrl: String) {
        sendAction(controlUrl, "Pause", "<InstanceID>0</InstanceID>")
    }

    suspend fun stop(controlUrl: String) {
        sendAction(controlUrl, "Stop", "<InstanceID>0</InstanceID>")
    }

    suspend fun seek(controlUrl: String, positionMs: Long) {
        sendAction(
            controlUrl, "Seek",
            "<InstanceID>0</InstanceID><Unit>REL_TIME</Unit><Target>${formatTime(positionMs)}</Target>"
        )
    }

    suspend fun getPositionInfo(controlUrl: String): DlnaPositionInfo? {
        val response = sendAction(controlUrl, "GetPositionInfo", "<InstanceID>0</InstanceID>") ?: return null
        val relTime = extractTag(response, "RelTime") ?: return null
        val duration = extractTag(response, "TrackDuration") ?: return null
        return DlnaPositionInfo(parseTime(relTime), parseTime(duration))
    }

    suspend fun getTransportState(controlUrl: String): String? {
        val response = sendAction(controlUrl, "GetTransportInfo", "<InstanceID>0</InstanceID>") ?: return null
        return extractTag(response, "CurrentTransportState")
    }

    private suspend fun sendAction(controlUrl: String, action: String, paramsXml: String): String? =
        withContext(Dispatchers.IO) {
            val body = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">" +
                "<s:Body><u:$action xmlns:u=\"$AV_TRANSPORT_SERVICE_TYPE\">$paramsXml</u:$action></s:Body>" +
                "</s:Envelope>"

            try {
                val connection = (URL(controlUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 3000
                    readTimeout = 3000
                    setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"")
                    setRequestProperty("SOAPACTION", "\"$AV_TRANSPORT_SERVICE_TYPE#$action\"")
                }
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(body) }

                val responseCode = connection.responseCode
                val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() }
                connection.disconnect()

                if (responseCode !in 200..299) {
                    Log.w(TAG, "$action failed: HTTP $responseCode: $text")
                }
                text
            } catch (e: Exception) {
                Log.w(TAG, "$action request failed: ${e.message}")
                null
            }
        }

    private fun buildDidlLite(mediaUrl: String, title: String, mimeType: String): String =
        "<DIDL-Lite xmlns=\"urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/\" " +
            "xmlns:dc=\"http://purl.org/dc/elements/1.1/\" " +
            "xmlns:upnp=\"urn:schemas-upnp-org:metadata-1-0/upnp/\">" +
            "<item id=\"0\" parentID=\"-1\" restricted=\"1\">" +
            "<dc:title>${xmlEscape(title)}</dc:title>" +
            "<upnp:class>object.item.videoItem</upnp:class>" +
            "<res protocolInfo=\"http-get:*:$mimeType:*\">${xmlEscape(mediaUrl)}</res>" +
            "</item></DIDL-Lite>"

    private fun xmlEscape(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private fun extractTag(xml: String, tag: String): String? =
        Regex("<$tag>(.*?)</$tag>", RegexOption.DOT_MATCHES_ALL).find(xml)?.groupValues?.get(1)?.trim()

    fun formatTime(ms: Long): String {
        val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms.coerceAtLeast(0))
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
    }

    fun parseTime(time: String): Long {
        val parts = time.substringBefore('.').split(":").map { it.toLongOrNull() ?: 0L }
        return when (parts.size) {
            3 -> ((parts[0] * 3600) + (parts[1] * 60) + parts[2]) * 1000
            2 -> ((parts[0] * 60) + parts[1]) * 1000
            else -> 0L
        }
    }
}
