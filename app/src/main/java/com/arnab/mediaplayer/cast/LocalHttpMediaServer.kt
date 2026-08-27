package com.arnab.mediaplayer.cast

import android.content.Context
import android.net.Uri
import fi.iki.elonen.NanoHTTPD
import java.io.IOException

/**
 * Serves the currently playing local video's bytes over plain HTTP on the LAN, since a
 * Cast receiver (running on the TV) can't resolve our app's `content://` URIs directly —
 * it needs a URL it can fetch itself.
 */
class LocalHttpMediaServer(
    private val context: Context,
    port: Int = 8090
) : NanoHTTPD(port) {

    companion object {
        const val MEDIA_PATH = "/media"
    }

    @Volatile
    var mediaUri: Uri? = null

    @Volatile
    var mimeType: String = "video/mp4"

    override fun serve(session: IHTTPSession): Response {
        val uri = mediaUri
        if (session.uri != MEDIA_PATH || uri == null) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")
        }

        return try {
            val length = context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length }
                ?: return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")

            val rangeHeader = session.headers["range"]
            if (rangeHeader != null && length > 0) {
                serveRange(uri, rangeHeader, length)
            } else {
                val stream = context.contentResolver.openInputStream(uri)
                    ?: return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")
                newFixedLengthResponse(Response.Status.OK, mimeType, stream, length).apply {
                    addHeader("Accept-Ranges", "bytes")
                }
            }
        } catch (e: IOException) {
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error: ${e.message}")
        }
    }

    private fun serveRange(uri: Uri, rangeHeader: String, totalLength: Long): Response {
        val spec = rangeHeader.removePrefix("bytes=")
        val (startText, endText) = spec.split("-", limit = 2).let { it[0] to it.getOrElse(1) { "" } }
        val start = startText.toLongOrNull() ?: 0L
        val end = endText.toLongOrNull()?.coerceAtMost(totalLength - 1) ?: (totalLength - 1)
        val contentLength = (end - start + 1).coerceAtLeast(0)

        val stream = context.contentResolver.openInputStream(uri)
            ?: return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")
        stream.skip(start)

        return newFixedLengthResponse(Response.Status.PARTIAL_CONTENT, mimeType, stream, contentLength).apply {
            addHeader("Content-Range", "bytes $start-$end/$totalLength")
            addHeader("Accept-Ranges", "bytes")
        }
    }
}
