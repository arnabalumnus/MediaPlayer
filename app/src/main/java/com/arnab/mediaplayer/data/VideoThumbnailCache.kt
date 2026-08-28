package com.arnab.mediaplayer.data

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Generates a small JPEG thumbnail for a video the first time it's needed and persists it under
 * the app's cache dir. Without this, every scroll pass that brings a video tile on screen makes
 * Coil's VideoFrameDecoder re-extract a full frame via MediaMetadataRetriever from scratch - that
 * decode is the actual source of the scrolling lag, not something Coil's in-memory cache alone
 * can fix once a tile has scrolled out and back in, or the list is reopened.
 */
object VideoThumbnailCache {

    private const val THUMBNAIL_WIDTH = 320
    private const val THUMBNAIL_HEIGHT = 180

    // Prevents two concurrent requests for the same video (e.g. rapid re-composition during a
    // fast fling) from both paying the extraction cost at once.
    private val locksById = ConcurrentHashMap<Long, Mutex>()

    suspend fun getOrCreate(context: Context, videoId: Long, videoUri: Uri): File? =
        withContext(Dispatchers.IO) {
            val file = cacheFile(context, videoId)
            if (file.exists() && file.length() > 0) return@withContext file

            val mutex = locksById.getOrPut(videoId) { Mutex() }
            mutex.withLock {
                if (file.exists() && file.length() > 0) return@withLock file

                val bitmap = extractFrame(context, videoUri) ?: return@withLock null
                try {
                    file.outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 82, out) }
                    file
                } catch (e: Exception) {
                    file.delete()
                    null
                } finally {
                    bitmap.recycle()
                }
            }
        }

    private fun extractFrame(context: Context, videoUri: Uri): Bitmap? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Backed by MediaProvider's own persistent thumbnail cache too - a second layer
            // of caching underneath ours, and the more efficient extraction path when available.
            context.contentResolver.loadThumbnail(videoUri, Size(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT), null)
        } else {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, videoUri)
                retriever.getFrameAtTime()
            } finally {
                retriever.release()
            }
        }
    } catch (e: Exception) {
        null
    }

    private fun cacheFile(context: Context, videoId: Long): File {
        val dir = File(context.cacheDir, "video_thumbnails").apply { mkdirs() }
        return File(dir, "$videoId.jpg")
    }
}
