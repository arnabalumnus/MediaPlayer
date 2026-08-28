package com.arnab.mediaplayer.ui.video

import android.media.AudioManager
import android.view.ViewGroup
import android.view.Window
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.arnab.mediaplayer.cast.CastController
import com.arnab.mediaplayer.ui.components.glassSource
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

/** D-pad directions/select keys - any of these should wake the hidden control overlay on a TV. */
private fun isNavigationKey(key: Key): Boolean = key in setOf(
    Key.DirectionUp, Key.DirectionDown, Key.DirectionLeft, Key.DirectionRight,
    Key.DirectionCenter, Key.Enter, Key.NumPadEnter
)

private val resizeModes = listOf(
    AspectRatioFrameLayout.RESIZE_MODE_FIT to "Fit",
    AspectRatioFrameLayout.RESIZE_MODE_FILL to "Fill",
    AspectRatioFrameLayout.RESIZE_MODE_ZOOM to "Crop"
)

enum class GestureType { BRIGHTNESS, VOLUME }

data class VideoPlaybackUiState(
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L
)

@Composable
fun VideoPlayerScreen(
    title: String,
    player: ExoPlayer,
    audioManager: AudioManager,
    window: Window,
    isInPictureInPicture: Boolean,
    castController: CastController,
    onBack: () -> Unit,
    onToggleFullscreen: (Boolean) -> Unit,
    onToggleKeepScreenOn: (Boolean) -> Unit,
    onEnterPip: () -> Unit
) {
    val context = LocalContext.current
    val hazeState = remember { HazeState() }
    val rootFocusRequester = remember { FocusRequester() }
    val playPauseFocusRequester = remember { FocusRequester() }

    var playbackState by remember { mutableStateOf(VideoPlaybackUiState()) }
    var isFullscreen by remember { mutableStateOf(false) }
    var keepScreenOn by remember { mutableStateOf(false) }
    var resizeModeIndex by remember { mutableIntStateOf(0) }
    var controlsVisible by remember { mutableStateOf(true) }

    var gestureType by remember { mutableStateOf<GestureType?>(null) }
    var gestureFraction by remember { mutableFloatStateOf(0f) }
    var gestureLabel by remember { mutableStateOf("") }

    var aspectBannerText by remember { mutableStateOf<String?>(null) }

    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var volumeFraction by remember {
        mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) / maxVolume.toFloat())
    }
    var brightnessFraction by remember {
        mutableFloatStateOf(
            window.attributes.screenBrightness.let { if (it in 0f..1f) it else 0.5f }
        )
    }

    val isCasting by castController.isCasting
    val castDeviceName by castController.castDeviceName

    // Poll playback position/duration and mirror play/pause + completion state, switching
    // between the local player and the remote Cast session depending on which is active.
    LaunchedEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!castController.isCasting.value) {
                    playbackState = playbackState.copy(isPlaying = isPlaying)
                }
            }
        }
        player.addListener(listener)
        while (isActive) {
            playbackState = if (castController.isCasting.value) {
                playbackState.copy(
                    isPlaying = castController.isRemotePlaying(),
                    positionMs = castController.remotePositionMs(),
                    durationMs = castController.remoteDurationMs()
                )
            } else {
                playbackState.copy(
                    isPlaying = player.isPlaying,
                    positionMs = player.currentPosition.coerceAtLeast(0L),
                    durationMs = player.duration.coerceAtLeast(0L)
                )
            }
            delay(500)
        }
    }

    // Stop the local playback/sound once the video has handed off to the TV.
    LaunchedEffect(isCasting) {
        if (isCasting) {
            player.pause()
        }
    }

    // Auto-hide the control overlay while playing.
    LaunchedEffect(controlsVisible, playbackState.isPlaying) {
        if (controlsVisible && playbackState.isPlaying) {
            delay(3000)
            controlsVisible = false
        }
    }

    // On a touchscreen, tapping the gesture zones below brings the (always-composed) overlay
    // back. A D-pad has no "tap" - once the controls are hidden there's nothing left focused
    // to press a direction from, so a remote user would be stuck. Move focus onto this root
    // Box whenever the controls hide, so it can catch the next D-pad press and reveal them
    // again; move focus onto Play/Pause whenever they're showing so navigation has a start.
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            playPauseFocusRequester.requestFocus()
        } else {
            rootFocusRequester.requestFocus()
        }
    }

    fun showGesture(type: GestureType, fraction: Float) {
        gestureType = type
        gestureFraction = fraction
        gestureLabel = "${(fraction * 100).roundToInt()}%"
    }

    LaunchedEffect(gestureType, gestureFraction) {
        if (gestureType != null) {
            delay(700)
            gestureType = null
        }
    }

    LaunchedEffect(aspectBannerText) {
        if (aspectBannerText != null) {
            delay(1200)
            aspectBannerText = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(rootFocusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                if (!controlsVisible && keyEvent.type == KeyEventType.KeyDown && isNavigationKey(keyEvent.key)) {
                    controlsVisible = true
                    true
                } else {
                    false
                }
            }
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .glassSource(hazeState),
            factory = {
                PlayerView(context).apply {
                    useController = false
                    this.player = player
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.resizeMode = resizeModes[resizeModeIndex].first
            }
        )

        if (!isInPictureInPicture) {
            // Left half: vertical drag adjusts screen brightness; tap toggles the controls.
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.5f)
                    .align(Alignment.CenterStart)
                    .pointerInput(Unit) {
                        var heightPx = size.height.toFloat().coerceAtLeast(1f)
                        detectVerticalDragGestures(
                            onDragStart = { heightPx = size.height.toFloat().coerceAtLeast(1f) },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                brightnessFraction = (brightnessFraction - dragAmount / heightPx).coerceIn(0.01f, 1f)
                                val attrs = window.attributes
                                attrs.screenBrightness = brightnessFraction
                                window.attributes = attrs
                                showGesture(GestureType.BRIGHTNESS, brightnessFraction)
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { controlsVisible = !controlsVisible })
                    }
            )

            // Right half: vertical drag adjusts media volume; tap toggles the controls.
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.5f)
                    .align(Alignment.CenterEnd)
                    .pointerInput(Unit) {
                        var heightPx = size.height.toFloat().coerceAtLeast(1f)
                        detectVerticalDragGestures(
                            onDragStart = { heightPx = size.height.toFloat().coerceAtLeast(1f) },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                volumeFraction = (volumeFraction - dragAmount / heightPx).coerceIn(0f, 1f)
                                audioManager.setStreamVolume(
                                    AudioManager.STREAM_MUSIC,
                                    (volumeFraction * maxVolume).roundToInt(),
                                    0
                                )
                                showGesture(GestureType.VOLUME, volumeFraction)
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { controlsVisible = !controlsVisible })
                    }
            )

            if (controlsVisible) {
                VideoPlayerControls(
                    hazeState = hazeState,
                    playPauseFocusRequester = playPauseFocusRequester,
                    title = title,
                    isPlaying = playbackState.isPlaying,
                    positionMs = playbackState.positionMs,
                    durationMs = playbackState.durationMs,
                    isFullscreen = isFullscreen,
                    keepScreenOn = keepScreenOn,
                    resizeModeLabel = resizeModes[resizeModeIndex].second,
                    isCasting = isCasting,
                    castDeviceName = castDeviceName,
                    onBack = onBack,
                    onPlayPause = {
                        if (isCasting) {
                            if (castController.isRemotePlaying()) castController.pauseRemote() else castController.playRemote()
                        } else {
                            if (player.isPlaying) player.pause() else player.play()
                        }
                    },
                    onSeekBackward = {
                        if (isCasting) {
                            castController.seekRemote((castController.remotePositionMs() - 10_000).coerceAtLeast(0))
                        } else {
                            player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0))
                        }
                    },
                    onSeekForward = {
                        if (isCasting) {
                            val cappedEnd = castController.remoteDurationMs().coerceAtLeast(0)
                            castController.seekRemote((castController.remotePositionMs() + 10_000).coerceAtMost(cappedEnd))
                        } else {
                            player.seekTo((player.currentPosition + 10_000).coerceAtMost(player.duration.coerceAtLeast(0)))
                        }
                    },
                    onSeek = { if (isCasting) castController.seekRemote(it) else player.seekTo(it) },
                    onToggleFullscreen = {
                        isFullscreen = !isFullscreen
                        onToggleFullscreen(isFullscreen)
                    },
                    onToggleKeepScreenOn = {
                        keepScreenOn = !keepScreenOn
                        onToggleKeepScreenOn(keepScreenOn)
                    },
                    onCycleResizeMode = {
                        resizeModeIndex = (resizeModeIndex + 1) % resizeModes.size
                        aspectBannerText = "Screen: ${resizeModes[resizeModeIndex].second}"
                    },
                    onEnterPip = onEnterPip
                )
            }

            gestureType?.let { type ->
                GestureIndicator(hazeState = hazeState, type = type, label = gestureLabel)
            }

            aspectBannerText?.let { text ->
                CenterBanner(hazeState = hazeState, text = text)
            }
        }
    }
}
