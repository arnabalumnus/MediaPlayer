package com.arnab.mediaplayer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arnab.mediaplayer.ui.audio.AudioPlayerActivity
import com.arnab.mediaplayer.ui.dashboard.DashboardScreen
import com.arnab.mediaplayer.ui.theme.MediaPlayerTheme
import com.arnab.mediaplayer.ui.video.VideoPlayerActivity
import com.arnab.mediaplayer.util.PermissionUtils
import com.arnab.mediaplayer.viewmodel.AudioViewModel
import com.arnab.mediaplayer.viewmodel.VideoViewModel

class MainActivity : ComponentActivity() {

    private val audioViewModel: AudioViewModel by viewModels {
        AudioViewModel.Factory((application as MediaPlayerApp).mediaRepository)
    }
    private val videoViewModel: VideoViewModel by viewModels {
        VideoViewModel.Factory((application as MediaPlayerApp).mediaRepository)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refreshPermissionState() }

    private var hasPermissionState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hasPermissionState = mutableStateOf(PermissionUtils.hasAllPermissions(this))

        setContent {
            var hasPermission by hasPermissionState
            MediaPlayerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    DashboardScreen(
                        audioViewModel = audioViewModel,
                        videoViewModel = videoViewModel,
                        hasPermission = hasPermission,
                        onRequestPermission = { permissionLauncher.launch(PermissionUtils.allPermissions()) },
                        onAudioItemClick = { queue, startIndex ->
                            startActivity(
                                Intent(this, AudioPlayerActivity::class.java).apply {
                                    putParcelableArrayListExtra(AudioPlayerActivity.EXTRA_QUEUE, ArrayList(queue))
                                    putExtra(AudioPlayerActivity.EXTRA_START_INDEX, startIndex)
                                }
                            )
                        },
                        onVideoItemClick = { video ->
                            startActivity(
                                Intent(this, VideoPlayerActivity::class.java).apply {
                                    putExtra(VideoPlayerActivity.EXTRA_VIDEO, video)
                                }
                            )
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionState()
    }

    private fun refreshPermissionState() {
        hasPermissionState.value = PermissionUtils.hasAllPermissions(this)
    }
}
