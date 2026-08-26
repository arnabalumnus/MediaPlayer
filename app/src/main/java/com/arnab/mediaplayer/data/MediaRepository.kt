package com.arnab.mediaplayer.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.arnab.mediaplayer.data.model.AudioItem
import com.arnab.mediaplayer.data.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Scans the device's MediaStore for audio files under the "Music" folder and
 * video files under the "Movies" folder, restricted to the requested formats.
 */
class MediaRepository(private val context: Context) {

    private val supportedAudioMimeTypes = setOf("audio/mpeg", "audio/mp3")
    private val supportedVideoMimeTypes = setOf(
        "video/mp4", "video/mpeg", "video/mp2t", "video/3gpp", "video/3gpp2"
    )

    suspend fun scanAudio(): List<AudioItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<AudioItem>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.MIME_TYPE,
            pathColumn()
        )

        val (selection, args) = buildFolderSelection(
            pathColumn = pathColumn(),
            folderName = "Music",
            mimeColumn = MediaStore.Audio.Media.MIME_TYPE,
            mimeTypes = supportedAudioMimeTypes
        )

        context.contentResolver.query(
            collection, projection, selection, args,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val albumId = cursor.getLong(albumIdCol)
                val uri = ContentUris.withAppendedId(collection, id)
                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"), albumId
                )
                items += AudioItem(
                    id = id,
                    uri = uri,
                    title = cursor.getString(titleCol) ?: "Unknown",
                    artist = cursor.getString(artistCol) ?: "Unknown artist",
                    album = cursor.getString(albumCol) ?: "Unknown album",
                    durationMs = cursor.getLong(durationCol),
                    albumArtUri = albumArtUri
                )
            }
        }
        items
    }

    suspend fun scanVideo(): List<VideoItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<VideoItem>()
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.MIME_TYPE,
            pathColumn()
        )

        val (selection, args) = buildFolderSelection(
            pathColumn = pathColumn(),
            folderName = "Movies",
            mimeColumn = MediaStore.Video.Media.MIME_TYPE,
            mimeTypes = supportedVideoMimeTypes
        )

        context.contentResolver.query(
            collection, projection, selection, args,
            "${MediaStore.Video.Media.TITLE} ASC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                items += VideoItem(
                    id = id,
                    uri = uri,
                    title = cursor.getString(titleCol) ?: "Unknown",
                    durationMs = cursor.getLong(durationCol),
                    widthPx = cursor.getInt(widthCol),
                    heightPx = cursor.getInt(heightCol)
                )
            }
        }
        items
    }

    private fun pathColumn(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.MediaColumns.RELATIVE_PATH
        } else {
            MediaStore.MediaColumns.DATA
        }

    /**
     * Restricts results to [folderName] (e.g. "Music"/"Movies", including nested
     * subfolders) and to one of [mimeTypes]. RELATIVE_PATH (API 29+) stores paths like
     * "Music/" while the legacy DATA column stores a full filesystem path, so the
     * folder match differs slightly between the two.
     */
    private fun buildFolderSelection(
        pathColumn: String,
        folderName: String,
        mimeColumn: String,
        mimeTypes: Set<String>
    ): Pair<String, Array<String>> {
        val mimePlaceholders = mimeTypes.joinToString(",") { "?" }
        val pathSelection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "$pathColumn LIKE ?"
        } else {
            "$pathColumn LIKE ?"
        }
        val pathArg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "$folderName/%"
        } else {
            "%/$folderName/%"
        }
        val selection = "$pathSelection AND $mimeColumn IN ($mimePlaceholders)"
        val args = arrayOf(pathArg) + mimeTypes.toTypedArray()
        return selection to args
    }
}
