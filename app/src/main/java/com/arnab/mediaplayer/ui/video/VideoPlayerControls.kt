package com.arnab.mediaplayer.ui.video

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arnab.mediaplayer.util.formatDuration

@Composable
fun VideoPlayerControls(
    title: String,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    isFullscreen: Boolean,
    keepScreenOn: Boolean,
    resizeModeLabel: String,
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
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
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
            Row(
                modifier = Modifier
                    .clickable(onClick = onCycleResizeMode)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Filled.AspectRatio, contentDescription = "Cycle screen fit: $resizeModeLabel", tint = Color.White)
                Text(resizeModeLabel, color = Color.White, style = MaterialTheme.typography.labelMedium)
            }
            IconButton(onClick = onEnterPip) {
                Icon(Icons.Filled.PictureInPictureAlt, contentDescription = "Picture-in-picture", tint = Color.White)
            }
            IconButton(onClick = onToggleKeepScreenOn) {
                Icon(
                    imageVector = if (keepScreenOn) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    contentDescription = "Keep screen on",
                    tint = Color.White
                )
            }
            IconButton(onClick = onToggleFullscreen) {
                Icon(
                    imageVector = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                    contentDescription = "Fullscreen",
                    tint = Color.White
                )
            }
        }

        // Center transport controls
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onSeekBackward, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Filled.Replay10, contentDescription = "Rewind 10 seconds", tint = Color.White, modifier = Modifier.size(36.dp))
            }
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(72.dp)
                    .background(Color.Black.copy(alpha = 0.45f), shape = RoundedCornerShape(50))
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }
            IconButton(onClick = onSeekForward, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Filled.Forward10, contentDescription = "Forward 10 seconds", tint = Color.White, modifier = Modifier.size(36.dp))
            }
        }

        // Bottom seek bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.45f))
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
fun CenterBanner(text: String) {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = text,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 72.dp)
                .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

@Composable
fun GestureIndicator(type: GestureType, label: String) {
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .align(if (type == GestureType.BRIGHTNESS) Alignment.CenterStart else Alignment.CenterEnd)
                .padding(32.dp)
                .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(12.dp))
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
