package com.arnab.mediaplayer.cast

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaSeekOptions
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.MediaMetadata as CastMediaMetadata

/**
 * Bridges our local ExoPlayer-driven UI to a Google Cast session so the currently playing
 * video can be handed off to a Chromecast-built-in Android TV on the same Wi-Fi network.
 * The device picker itself is handled by [androidx.mediarouter.app.MediaRouteButton]; this
 * class reacts once a session connects and forwards play/pause/seek to the remote device.
 */
class CastController(context: Context) {

    private val appContext = context.applicationContext
    private var castContext: CastContext? = null
    private var currentSession: CastSession? = null
    private var pendingLoad: (() -> Unit)? = null

    val isAvailable = mutableStateOf(false)
    val isCasting = mutableStateOf(false)
    val castDeviceName = mutableStateOf<String?>(null)

    private val sessionListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarted(session: CastSession, sessionId: String) = onConnected(session)
        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) = onConnected(session)
        override fun onSessionEnded(session: CastSession, error: Int) = onDisconnected()
        override fun onSessionSuspended(session: CastSession, reason: Int) = onDisconnected()
        override fun onSessionStartFailed(session: CastSession, error: Int) = onDisconnected()
        override fun onSessionStarting(session: CastSession) = Unit
        override fun onSessionEnding(session: CastSession) = Unit
        override fun onSessionResuming(session: CastSession, sessionId: String) = Unit
        override fun onSessionResumeFailed(session: CastSession, error: Int) = Unit
    }

    init {
        try {
            castContext = CastContext.getSharedInstance(appContext).also {
                it.sessionManager.addSessionManagerListener(sessionListener, CastSession::class.java)
            }
            isAvailable.value = true
        } catch (e: Exception) {
            // No/outdated Google Play services, or no Cast-capable network — casting stays unavailable.
            Log.w("CastController", "Cast unavailable: ${e.message}")
            isAvailable.value = false
        }
    }

    private fun onConnected(session: CastSession) {
        currentSession = session
        isCasting.value = true
        castDeviceName.value = session.castDevice?.friendlyName
        pendingLoad?.invoke()
        pendingLoad = null
    }

    private fun onDisconnected() {
        currentSession = null
        isCasting.value = false
        castDeviceName.value = null
    }

    /**
     * Queues (or immediately sends, if already connected) a load request for [url]. Position is
     * read lazily via [currentPositionMs] at the moment the session is actually ready, so casting
     * resumes from wherever local playback currently is.
     */
    fun prepareToCast(url: String, title: String, mimeType: String, currentPositionMs: () -> Long) {
        val load: () -> Unit = {
            val metadata = CastMediaMetadata(CastMediaMetadata.MEDIA_TYPE_MOVIE).apply {
                putString(CastMediaMetadata.KEY_TITLE, title)
                putInt(CastMediaMetadata.KEY_WIDTH, 848)
                putInt(CastMediaMetadata.KEY_HEIGHT, 478)
            }
            val mediaInfo = MediaInfo.Builder(url)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType(mimeType)
                .setMetadata(metadata)
                .build()
            val request = MediaLoadRequestData.Builder()
                .setMediaInfo(mediaInfo)
                .setAutoplay(true)
                .setCurrentTime(currentPositionMs())
                .build()
            currentSession?.remoteMediaClient?.load(request)
            Unit
        }
        if (currentSession != null) load() else pendingLoad = load
    }

    fun playRemote() {
        currentSession?.remoteMediaClient?.play()
    }

    fun pauseRemote() {
        currentSession?.remoteMediaClient?.pause()
    }

    fun seekRemote(positionMs: Long) {
        currentSession?.remoteMediaClient?.seek(MediaSeekOptions.Builder().setPosition(positionMs).build())
    }

    fun isRemotePlaying(): Boolean =
        currentSession?.remoteMediaClient?.mediaStatus?.playerState == MediaStatus.PLAYER_STATE_PLAYING

    fun remotePositionMs(): Long = currentSession?.remoteMediaClient?.approximateStreamPosition ?: 0L

    fun remoteDurationMs(): Long = currentSession?.remoteMediaClient?.mediaInfo?.streamDuration ?: 0L

    fun stopCasting() {
        castContext?.sessionManager?.endCurrentSession(true)
    }

    fun release() {
        castContext?.sessionManager?.removeSessionManagerListener(sessionListener, CastSession::class.java)
    }
}
