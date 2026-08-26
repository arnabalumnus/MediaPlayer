package com.arnab.mediaplayer.ui.audio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.IntentCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.arnab.mediaplayer.data.model.AudioItem
import com.arnab.mediaplayer.playback.PlaybackController
import com.arnab.mediaplayer.ui.theme.MediaPlayerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class AudioPlayerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_QUEUE = "extra_queue"
        const val EXTRA_START_INDEX = "extra_start_index"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val queue: List<AudioItem> =
            IntentCompat.getParcelableArrayListExtra(intent, EXTRA_QUEUE, AudioItem::class.java)
                ?: emptyList()
        val startIndex = intent.getIntExtra(EXTRA_START_INDEX, 0)

        setContent {
            MediaPlayerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AudioPlayerScreen(
                        queue = queue,
                        startIndex = startIndex,
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

data class AudioPlayerUiState(
    val currentItem: AudioItem? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false
)

@Composable
fun AudioPlayerScreen(
    queue: List<AudioItem>,
    startIndex: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var controller by remember { mutableStateOf<MediaController?>(null) }
    var uiState by remember { mutableStateOf(AudioPlayerUiState(currentItem = queue.getOrNull(startIndex))) }

    DisposableEffect(Unit) {
        onDispose { PlaybackController.release(controller) }
    }

    LaunchedEffect(Unit) {
        val mediaController = PlaybackController.connect(context)
        controller = mediaController

        if (mediaController.mediaItemCount != queue.size) {
            val mediaItems = queue.map { item ->
                MediaItem.Builder()
                    .setMediaId(item.id.toString())
                    .setUri(item.uri)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(item.title)
                            .setArtist(item.artist)
                            .setAlbumTitle(item.album)
                            .setArtworkUri(item.albumArtUri)
                            .build()
                    )
                    .build()
            }
            mediaController.setMediaItems(mediaItems, startIndex.coerceIn(0, mediaItems.lastIndex), 0L)
            mediaController.prepare()
            mediaController.play()
        }

        fun syncState() {
            val index = mediaController.currentMediaItemIndex
            uiState = uiState.copy(
                currentItem = queue.getOrNull(index) ?: uiState.currentItem,
                isPlaying = mediaController.isPlaying,
                durationMs = mediaController.duration.coerceAtLeast(0L),
                hasNext = mediaController.hasNextMediaItem(),
                hasPrevious = mediaController.hasPreviousMediaItem()
            )
        }
        syncState()

        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                uiState = uiState.copy(isPlaying = isPlaying)
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                syncState()
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                syncState()
            }
        }
        mediaController.addListener(listener)

        while (isActive) {
            uiState = uiState.copy(positionMs = mediaController.currentPosition.coerceAtLeast(0L))
            delay(500)
        }
    }

    AudioPlayerContent(
        uiState = uiState,
        onBack = onBack,
        onPlayPause = {
            controller?.let { if (it.isPlaying) it.pause() else it.play() }
        },
        onNext = { controller?.seekToNextMediaItem() },
        onPrevious = { controller?.seekToPreviousMediaItem() },
        onSeek = { positionMs -> controller?.seekTo(positionMs) }
    )
}
