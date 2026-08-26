package com.arnab.mediaplayer.ui.video

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.os.Bundle
import android.util.Rational
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.arnab.mediaplayer.data.model.VideoItem
import com.arnab.mediaplayer.ui.theme.MediaPlayerTheme

class VideoPlayerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_VIDEO = "extra_video"
        private const val ACTION_MEDIA_CONTROL = "com.arnab.mediaplayer.PIP_MEDIA_CONTROL"
        private const val EXTRA_CONTROL_TYPE = "extra_control_type"
        private const val CONTROL_TYPE_PLAY = 1
        private const val CONTROL_TYPE_PAUSE = 2
    }

    private lateinit var player: ExoPlayer
    private lateinit var audioManager: AudioManager
    private var video: VideoItem? = null

    private val isInPipState = mutableStateOf(false)

    private val pipReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != ACTION_MEDIA_CONTROL) return
            when (intent.getIntExtra(EXTRA_CONTROL_TYPE, -1)) {
                CONTROL_TYPE_PLAY -> player.play()
                CONTROL_TYPE_PAUSE -> player.pause()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        video = IntentCompat.getParcelableExtra(intent, EXTRA_VIDEO, VideoItem::class.java)

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                /* handleAudioFocus= */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        video?.let {
            player.setMediaItem(MediaItem.fromUri(it.uri))
            player.prepare()
            player.playWhenReady = true
        }

        // Keep the system PiP action (play/pause) in sync while the small PiP window is open.
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isInPipState.value) {
                    setPictureInPictureParams(buildPipParams(isPlaying))
                }
            }
        })

        ContextCompat.registerReceiver(
            this,
            pipReceiver,
            IntentFilter(ACTION_MEDIA_CONTROL),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        setContent {
            val isInPip by isInPipState
            MediaPlayerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = androidx.compose.ui.graphics.Color.Black) {
                    VideoPlayerScreen(
                        title = video?.title ?: "",
                        player = player,
                        audioManager = audioManager,
                        window = window,
                        isInPictureInPicture = isInPip,
                        onBack = { finish() },
                        onToggleFullscreen = ::applyFullscreen,
                        onToggleKeepScreenOn = ::applyKeepScreenOn,
                        onEnterPip = ::enterPip
                    )
                }
                DisposableEffect(Unit) {
                    onDispose { player.release() }
                }
            }
        }
    }

    private fun applyFullscreen(enabled: Boolean) {
        requestedOrientation = if (enabled) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        if (enabled) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun applyKeepScreenOn(enabled: Boolean) {
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun enterPip() {
        enterPictureInPictureMode(buildPipParams(player.isPlaying))
    }

    /** Auto-enter PiP when the user leaves (Home/recents) while a video is actively playing. */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (player.isPlaying) {
            enterPip()
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipState.value = isInPictureInPictureMode
    }

    private fun buildPipParams(isPlaying: Boolean): PictureInPictureParams {
        val aspect = video?.takeIf { it.widthPx > 0 && it.heightPx > 0 }
            ?.let { Rational(it.widthPx, it.heightPx) }
            ?: Rational(16, 9)
        val clampedAspect = clampToSupportedAspectRatio(aspect)

        val controlType = if (isPlaying) CONTROL_TYPE_PAUSE else CONTROL_TYPE_PLAY
        val actionTitle = if (isPlaying) "Pause" else "Play"
        val icon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            controlType,
            Intent(ACTION_MEDIA_CONTROL).setPackage(packageName).putExtra(EXTRA_CONTROL_TYPE, controlType),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val remoteAction = RemoteAction(
            Icon.createWithResource(this, icon),
            actionTitle,
            actionTitle,
            pendingIntent
        )

        return PictureInPictureParams.Builder()
            .setAspectRatio(clampedAspect)
            .setActions(listOf(remoteAction))
            .build()
    }

    /** PiP aspect ratios must fall within Android's supported range of 1:2.39 to 2.39:1. */
    private fun clampToSupportedAspectRatio(rational: Rational): Rational {
        val value = rational.toFloat()
        return when {
            value > 2.39f -> Rational(239, 100)
            value < 1f / 2.39f -> Rational(100, 239)
            else -> rational
        }
    }

    override fun onStop() {
        super.onStop()
        player.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(pipReceiver)
    }
}
