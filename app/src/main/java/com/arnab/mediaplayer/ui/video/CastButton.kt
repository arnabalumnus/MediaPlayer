package com.arnab.mediaplayer.ui.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory

/**
 * Wraps the framework's [MediaRouteButton]: tapping it opens the system's Cast device picker
 * (any Chromecast-built-in TV on the same Wi-Fi shows up there), and it swaps its own icon to
 * reflect connected/disconnected state automatically — no extra wiring needed here.
 */
@Composable
fun CastButton(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.45f), shape = CircleShape)
            .padding(6.dp),
        factory = { context ->
            MediaRouteButton(context).apply {
                CastButtonFactory.setUpMediaRouteButton(context.applicationContext, this)
            }
        }
    )
}
