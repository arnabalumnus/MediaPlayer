package com.arnab.mediaplayer.ui.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arnab.mediaplayer.data.model.AudioItem
import com.arnab.mediaplayer.data.model.VideoItem
import com.arnab.mediaplayer.ui.audio.AudioTabScreen
import com.arnab.mediaplayer.ui.components.ViewModeToggleAction
import com.arnab.mediaplayer.ui.video.VideoTabScreen
import com.arnab.mediaplayer.viewmodel.AudioViewModel
import com.arnab.mediaplayer.viewmodel.VideoViewModel
import kotlinx.coroutines.launch

private val tabTitles = listOf("Audio", "Video")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    audioViewModel: AudioViewModel,
    videoViewModel: VideoViewModel,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onAudioItemClick: (List<AudioItem>, Int) -> Unit,
    onVideoItemClick: (VideoItem) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { tabTitles.size })
    val scope = rememberCoroutineScope()

    val audioItems by audioViewModel.audioItems.collectAsStateWithLifecycle()
    val audioViewMode by audioViewModel.viewMode.collectAsStateWithLifecycle()
    val audioLoading by audioViewModel.isLoading.collectAsStateWithLifecycle()

    val videoItems by videoViewModel.videoItems.collectAsStateWithLifecycle()
    val videoViewMode by videoViewModel.viewMode.collectAsStateWithLifecycle()
    val videoLoading by videoViewModel.isLoading.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Media Player") },
                actions = {
                    if (pagerState.currentPage == 0) {
                        ViewModeToggleAction(audioViewMode, onToggle = audioViewModel::toggleViewMode)
                    } else {
                        ViewModeToggleAction(videoViewMode, onToggle = videoViewModel::toggleViewMode)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (!hasPermission) {
                PermissionRationale(onRequestPermission)
                return@Column
            }

            TabRow(selectedTabIndex = pagerState.currentPage) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(title) }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> AudioTabScreen(
                        audioItems = audioItems,
                        viewMode = audioViewMode,
                        isLoading = audioLoading,
                        onLoad = audioViewModel::load,
                        onItemClick = { _, index -> onAudioItemClick(audioItems, index) }
                    )
                    else -> VideoTabScreen(
                        videoItems = videoItems,
                        viewMode = videoViewMode,
                        isLoading = videoLoading,
                        onLoad = videoViewModel::load,
                        onItemClick = { item, _ -> onVideoItemClick(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionRationale(onRequestPermission: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Storage permission is needed to scan the Music and Movies folders.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onRequestPermission, modifier = Modifier.padding(top = 16.dp)) {
                Text("Grant permission")
            }
        }
    }
}
