package com.arnab.mediaplayer.ui.audio

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
import com.arnab.mediaplayer.data.model.AudioItem
import com.arnab.mediaplayer.ui.components.AudioGridCell
import com.arnab.mediaplayer.ui.components.AudioListRow
import com.arnab.mediaplayer.util.ViewMode

@Composable
fun AudioTabScreen(
    audioItems: List<AudioItem>,
    viewMode: ViewMode,
    isLoading: Boolean,
    onLoad: () -> Unit,
    onItemClick: (AudioItem, Int) -> Unit
) {
    LaunchedEffect(Unit) { onLoad() }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading && audioItems.isEmpty() -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            audioItems.isEmpty() -> {
                Text(
                    "No MP3 files found in the Music folder",
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
                    itemsIndexed(audioItems) { index, item ->
                        AudioListRow(item = item, onClick = { onItemClick(item, index) })
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
                    itemsIndexed(audioItems) { index, item ->
                        AudioGridCell(item = item, onClick = { onItemClick(item, index) })
                    }
                }
            }
        }
    }
}
