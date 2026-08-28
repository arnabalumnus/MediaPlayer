package com.arnab.mediaplayer.ui.video

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arnab.mediaplayer.data.model.VideoItem
import com.arnab.mediaplayer.ui.components.VideoGridCell
import com.arnab.mediaplayer.ui.components.VideoListRow
import com.arnab.mediaplayer.util.ViewMode

@Composable
fun VideoTabScreen(
    videoItems: List<VideoItem>,
    viewMode: ViewMode,
    isLoading: Boolean,
    onLoad: () -> Unit,
    onItemClick: (VideoItem, Int) -> Unit
) {
    LaunchedEffect(Unit) { onLoad() }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading && videoItems.isEmpty() -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            videoItems.isEmpty() -> {
                Text(
                    "No videos found in the Movies folder",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            viewMode == ViewMode.LIST -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(videoItems) { index, item ->
                        VideoListRow(item = item, onClick = { onItemClick(item, index) })
                    }
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(videoItems) { index, item ->
                        VideoGridCell(item = item, onClick = { onItemClick(item, index) })
                    }
                }
            }
        }
    }
}
