package com.arnab.mediaplayer.ui.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arnab.mediaplayer.data.model.AudioItem
import com.arnab.mediaplayer.data.model.VideoItem
import com.arnab.mediaplayer.ui.audio.AudioTabScreen
import com.arnab.mediaplayer.ui.components.ViewModeToggleAction
import com.arnab.mediaplayer.ui.components.glassPanel
import com.arnab.mediaplayer.ui.components.glassSource
import com.arnab.mediaplayer.ui.theme.PillShape
import com.arnab.mediaplayer.ui.video.VideoTabScreen
import com.arnab.mediaplayer.viewmodel.AudioViewModel
import com.arnab.mediaplayer.viewmodel.VideoViewModel
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch

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
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val hazeState = remember { HazeState() }

    val audioItems by audioViewModel.audioItems.collectAsStateWithLifecycle()
    val audioViewMode by audioViewModel.viewMode.collectAsStateWithLifecycle()
    val audioLoading by audioViewModel.isLoading.collectAsStateWithLifecycle()

    val videoItems by videoViewModel.videoItems.collectAsStateWithLifecycle()
    val videoViewMode by videoViewModel.viewMode.collectAsStateWithLifecycle()
    val videoLoading by videoViewModel.isLoading.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    title = { Text("Media Player") },
                    actions = {
                        if (pagerState.currentPage == 0) {
                            ViewModeToggleAction(audioViewMode, onToggle = audioViewModel::toggleViewMode)
                        } else {
                            ViewModeToggleAction(videoViewMode, onToggle = videoViewModel::toggleViewMode)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    modifier = Modifier.statusBarsPadding()
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(top = padding.calculateTopPadding())) {
                if (!hasPermission) {
                    PermissionRationale(onRequestPermission)
                    return@Column
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .glassSource(hazeState)
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

        if (hasPermission) {
            FloatingTabBar(
                hazeState = hazeState,
                selectedIndex = pagerState.currentPage,
                onSelect = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun FloatingTabBar(
    hazeState: HazeState,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(horizontal = 48.dp)
            .glassPanel(hazeState = hazeState, shape = PillShape, tint = MaterialTheme.colorScheme.primary)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TabIcon(
                icon = Icons.Filled.MusicNote,
                contentDescription = "Audio",
                selected = selectedIndex == 0,
                onClick = { onSelect(0) }
            )
            TabIcon(
                icon = Icons.Filled.Movie,
                contentDescription = "Video",
                selected = selectedIndex == 1,
                onClick = { onSelect(1) }
            )
        }
    }
}

@Composable
private fun TabIcon(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val indicatorColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else Color.Transparent,
        label = "tabIndicator"
    )
    val iconTint by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
        label = "tabIconTint"
    )
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(indicatorColor),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconTint
            )
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
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onRequestPermission, modifier = Modifier.padding(top = 16.dp)) {
                Text("Grant permission")
            }
        }
    }
}
