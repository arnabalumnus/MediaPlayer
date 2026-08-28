package com.arnab.mediaplayer.ui.video

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arnab.mediaplayer.ui.components.glassPanel
import com.arnab.mediaplayer.ui.components.tvFocusHalo
import com.arnab.mediaplayer.util.formatDuration
import dev.chrisbanes.haze.HazeState

private val PanelShape = RoundedCornerShape(24.dp)

@Composable
fun VideoPlayerControls(
    hazeState: HazeState,
    playPauseFocusRequester: FocusRequester,
    title: String,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    isFullscreen: Boolean,
    keepScreenOn: Boolean,
    resizeModeLabel: String,
    isCasting: Boolean,
    castDeviceName: String?,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekBackward: () -> Unit,
    onSeekForward: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleFullscreen: () -> Unit,
    onToggleKeepScreenOn: () -> Unit,
    onCycleResizeMode: () -> Unit,
    onEnterPip: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(12.dp)
                .glassPanel(hazeState, PanelShape, tint = Color.Black)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val backInteraction = remember { MutableInteractionSource() }
            IconButton(
                onClick = onBack,
                interactionSource = backInteraction,
                modifier = Modifier.tvFocusHalo(backInteraction)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = title,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            )
            val resizeInteraction = remember { MutableInteractionSource() }
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onCycleResizeMode, interactionSource = resizeInteraction, indication = null)
                    .tvFocusHalo(resizeInteraction)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Filled.AspectRatio, contentDescription = "Cycle screen fit: $resizeModeLabel", tint = Color.White)
                Text(resizeModeLabel, color = Color.White, style = MaterialTheme.typography.labelMedium)
            }
            CastButton(modifier = Modifier.size(40.dp))
            val pipInteraction = remember { MutableInteractionSource() }
            IconButton(
                onClick = onEnterPip,
                interactionSource = pipInteraction,
                modifier = Modifier.tvFocusHalo(pipInteraction)
            ) {
                Icon(Icons.Filled.PictureInPictureAlt, contentDescription = "Picture-in-picture", tint = Color.White)
            }
            val screenOnInteraction = remember { MutableInteractionSource() }
            IconButton(
                onClick = onToggleKeepScreenOn,
                interactionSource = screenOnInteraction,
                modifier = Modifier.tvFocusHalo(screenOnInteraction)
            ) {
                Icon(
                    imageVector = if (keepScreenOn) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    contentDescription = "Keep screen on",
                    tint = Color.White
                )
            }
            val fullscreenInteraction = remember { MutableInteractionSource() }
            IconButton(
                onClick = onToggleFullscreen,
                interactionSource = fullscreenInteraction,
                modifier = Modifier.tvFocusHalo(fullscreenInteraction)
            ) {
                Icon(
                    imageVector = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                    contentDescription = "Fullscreen",
                    tint = Color.White
                )
            }
        }

        if (isCasting) {
            Text(
                text = "Casting to ${castDeviceName ?: "TV"}",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 76.dp)
                    .glassPanel(hazeState, RoundedCornerShape(16.dp), tint = Color.Black)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        // Center transport controls
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val rewindInteraction = remember { MutableInteractionSource() }
            IconButton(
                onClick = onSeekBackward,
                interactionSource = rewindInteraction,
                modifier = Modifier.size(56.dp).tvFocusHalo(rewindInteraction)
            ) {
                Icon(Icons.Filled.Replay10, contentDescription = "Rewind 10 seconds", tint = Color.White, modifier = Modifier.size(36.dp))
            }
            val playPauseInteraction = remember { MutableInteractionSource() }
            IconButton(
                onClick = onPlayPause,
                interactionSource = playPauseInteraction,
                modifier = Modifier
                    .size(76.dp)
                    .glassPanel(hazeState, CircleShape, tint = MaterialTheme.colorScheme.primary)
                    .tvFocusHalo(playPauseInteraction)
                    .focusRequester(playPauseFocusRequester)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }
            val forwardInteraction = remember { MutableInteractionSource() }
            IconButton(
                onClick = onSeekForward,
                interactionSource = forwardInteraction,
                modifier = Modifier.size(56.dp).tvFocusHalo(forwardInteraction)
            ) {
                Icon(Icons.Filled.Forward10, contentDescription = "Forward 10 seconds", tint = Color.White, modifier = Modifier.size(36.dp))
            }
        }

        // Bottom seek bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(12.dp)
                .glassPanel(hazeState, PanelShape, tint = Color.Black)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            var isDragging by remember { mutableStateOf(false) }
            var dragPosition by remember { mutableFloatStateOf(0f) }
            val sliderPosition = if (isDragging) dragPosition else positionMs.toFloat()

            Slider(
                value = sliderPosition,
                onValueChange = {
                    isDragging = true
                    dragPosition = it
                },
                onValueChangeFinished = {
                    onSeek(dragPosition.toLong())
                    isDragging = false
                },
                valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatDuration(sliderPosition.toLong()), color = Color.White)
                Text(formatDuration(durationMs), color = Color.White)
            }
        }
    }
}

@Composable
fun CenterBanner(hazeState: HazeState, text: String) {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = text,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 88.dp)
                .glassPanel(hazeState, RoundedCornerShape(16.dp), tint = Color.Black)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

@Composable
fun GestureIndicator(hazeState: HazeState, type: GestureType, label: String) {
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .align(if (type == GestureType.BRIGHTNESS) Alignment.CenterStart else Alignment.CenterEnd)
                .padding(32.dp)
                .glassPanel(hazeState, RoundedCornerShape(16.dp), tint = Color.Black)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (type == GestureType.BRIGHTNESS) Icons.Filled.Brightness6 else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = null,
                tint = Color.White
            )
            Text(label, color = Color.White)
        }
    }
}
