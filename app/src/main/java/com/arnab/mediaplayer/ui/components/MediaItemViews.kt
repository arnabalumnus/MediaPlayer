package com.arnab.mediaplayer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.arnab.mediaplayer.data.VideoThumbnailCache
import com.arnab.mediaplayer.data.model.AudioItem
import com.arnab.mediaplayer.data.model.VideoItem
import com.arnab.mediaplayer.util.ViewMode
import com.arnab.mediaplayer.util.formatDuration
import java.io.File

private val CardShape = RoundedCornerShape(20.dp)
private val ThumbnailShape = RoundedCornerShape(14.dp)

@Composable
fun ViewModeToggleAction(viewMode: ViewMode, onToggle: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    IconButton(
        onClick = onToggle,
        interactionSource = interactionSource,
        modifier = Modifier.tvFocusHalo(interactionSource)
    ) {
        Icon(
            imageVector = if (viewMode == ViewMode.LIST) Icons.Filled.GridView else Icons.AutoMirrored.Filled.ViewList,
            contentDescription = "Toggle view"
        )
    }
}

/** Soft translucent card shared by every list row / grid cell — the "glass" tile look. */
@Composable
private fun MediaCard(onClick: () -> Unit, content: @Composable () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        onClick = onClick,
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusHalo(interactionSource, CardShape)
    ) {
        content()
    }
}

/**
 * Thumbnail that shows [icon] as a placeholder/fallback, hiding it once [model] loads
 * successfully. A `null` model just shows the icon — used while a video's cached thumbnail
 * hasn't been generated yet, so we don't kick off two decodes (ours and Coil's) at once.
 */
@Composable
private fun Thumbnail(model: Any?, tint: Color, icon: ImageVector, modifier: Modifier) {
    var showFallback by remember(model) { mutableStateOf(true) }
    Box(
        modifier = modifier.background(tint.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onState = { state -> showFallback = state !is AsyncImagePainter.State.Success }
            )
        }
        if (showFallback) {
            Icon(icon, contentDescription = null, tint = tint)
        }
    }
}

/**
 * Video thumbnail backed by [VideoThumbnailCache] — the first time a given video is seen this
 * decodes a frame and persists it to the app's cache dir; every scroll pass after that (and
 * every future app run) just loads that small cached JPEG instead of re-decoding the video.
 */
@Composable
private fun VideoThumbnail(item: VideoItem, modifier: Modifier) {
    val context = LocalContext.current
    var cachedFile by remember(item.id) { mutableStateOf<File?>(null) }
    LaunchedEffect(item.id) {
        cachedFile = VideoThumbnailCache.getOrCreate(context, item.id, item.uri)
    }
    Thumbnail(
        model = cachedFile,
        tint = MaterialTheme.colorScheme.tertiary,
        icon = Icons.Filled.Movie,
        modifier = modifier
    )
}

@Composable
fun AudioListRow(item: AudioItem, onClick: () -> Unit) {
    MediaCard(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Thumbnail(
                model = item.albumArtUri,
                tint = MaterialTheme.colorScheme.primary,
                icon = Icons.Filled.MusicNote,
                modifier = Modifier.size(52.dp).clip(ThumbnailShape)
            )
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f)
            ) {
                Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${item.artist} • ${item.album}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                formatDuration(item.durationMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AudioGridCell(item: AudioItem, onClick: () -> Unit) {
    MediaCard(onClick = onClick) {
        Column(modifier = Modifier.padding(10.dp)) {
            Thumbnail(
                model = item.albumArtUri,
                tint = MaterialTheme.colorScheme.primary,
                icon = Icons.Filled.MusicNote,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(ThumbnailShape)
            )
            Text(
                item.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                item.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun VideoListRow(item: VideoItem, onClick: () -> Unit) {
    MediaCard(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VideoThumbnail(
                item = item,
                modifier = Modifier.size(width = 100.dp, height = 60.dp).clip(ThumbnailShape)
            )
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f)
            ) {
                Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${item.widthPx}x${item.heightPx}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                formatDuration(item.durationMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun VideoGridCell(item: VideoItem, onClick: () -> Unit) {
    MediaCard(onClick = onClick) {
        Column(modifier = Modifier.padding(10.dp)) {
            VideoThumbnail(
                item = item,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(ThumbnailShape)
            )
            Text(
                item.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                formatDuration(item.durationMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
