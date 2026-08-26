package com.arnab.mediaplayer.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Connects Compose UI (running in an Activity) to the [AudioPlaybackService]'s session. */
object PlaybackController {

    suspend fun connect(context: Context): MediaController {
        val sessionToken = SessionToken(
            context.applicationContext,
            ComponentName(context.applicationContext, AudioPlaybackService::class.java)
        )
        val future = MediaController.Builder(context.applicationContext, sessionToken).buildAsync()
        return suspendCancellableCoroutine { continuation ->
            future.addListener(
                {
                    if (continuation.isActive) {
                        continuation.resume(future.get())
                    }
                },
                MoreExecutors.directExecutor()
            )
            continuation.invokeOnCancellation { future.cancel(false) }
        }
    }

    fun release(controller: MediaController?) {
        controller?.release()
    }
}
