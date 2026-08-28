package com.arnab.mediaplayer.dlna

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Bridges our local ExoPlayer-driven UI to a DLNA/UPnP MediaRenderer (a smart TV's built-in
 * DLNA service) so the currently playing video can be handed off to it. Unlike Google Cast,
 * there's no OS-level picker here: [discover] runs an SSDP search and [connect] loads the
 * video onto whichever device the user picks, then play/pause/seek are forwarded to it.
 */
class DlnaController(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val isDiscovering = mutableStateOf(false)
    val discoveredDevices = mutableStateOf<List<DlnaDevice>>(emptyList())

    val isCasting = mutableStateOf(false)
    val castDeviceName = mutableStateOf<String?>(null)

    private val cachedIsPlaying = mutableStateOf(false)
    private val cachedPositionMs = mutableStateOf(0L)
    private val cachedDurationMs = mutableStateOf(0L)

    private var connectedDevice: DlnaDevice? = null
    private var pollJob: Job? = null

    private var pendingUrl: String? = null
    private var pendingTitle: String = ""
    private var pendingMimeType: String = "video/mp4"
    private var pendingPositionMs: () -> Long = { 0L }

    /** Queues the media to load once the user picks a device via [connect]. */
    fun prepareToCast(url: String, title: String, mimeType: String, currentPositionMs: () -> Long) {
        pendingUrl = url
        pendingTitle = title
        pendingMimeType = mimeType
        pendingPositionMs = currentPositionMs
    }

    fun discover() {
        if (isDiscovering.value) return
        scope.launch {
            isDiscovering.value = true
            discoveredDevices.value = SsdpDiscovery.discoverMediaRenderers(appContext)
            isDiscovering.value = false
        }
    }

    fun connect(device: DlnaDevice) {
        val url = pendingUrl ?: return
        scope.launch {
            connectedDevice = device
            AvTransportClient.setAvTransportUri(device.controlUrl, url, pendingTitle, pendingMimeType)
            delay(500)
            val startPosition = pendingPositionMs()
            if (startPosition > 1000) {
                AvTransportClient.seek(device.controlUrl, startPosition)
            }
            AvTransportClient.play(device.controlUrl)
            isCasting.value = true
            castDeviceName.value = device.friendlyName
            startPolling()
        }
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = scope.launch {
            while (isActive && isCasting.value) {
                val device = connectedDevice ?: break
                val position = AvTransportClient.getPositionInfo(device.controlUrl)
                val state = AvTransportClient.getTransportState(device.controlUrl)
                if (position != null) {
                    cachedPositionMs.value = position.positionMs
                    cachedDurationMs.value = position.durationMs
                }
                if (state != null) {
                    cachedIsPlaying.value = state.equals("PLAYING", ignoreCase = true)
                }
                delay(1000)
            }
        }
    }

    fun playRemote() {
        val device = connectedDevice ?: return
        cachedIsPlaying.value = true
        scope.launch { AvTransportClient.play(device.controlUrl) }
    }

    fun pauseRemote() {
        val device = connectedDevice ?: return
        cachedIsPlaying.value = false
        scope.launch { AvTransportClient.pause(device.controlUrl) }
    }

    fun seekRemote(positionMs: Long) {
        val device = connectedDevice ?: return
        cachedPositionMs.value = positionMs
        scope.launch { AvTransportClient.seek(device.controlUrl, positionMs) }
    }

    fun isRemotePlaying(): Boolean = cachedIsPlaying.value
    fun remotePositionMs(): Long = cachedPositionMs.value
    fun remoteDurationMs(): Long = cachedDurationMs.value

    fun stopCasting() {
        val device = connectedDevice
        pollJob?.cancel()
        if (device != null) {
            scope.launch { AvTransportClient.stop(device.controlUrl) }
        }
        connectedDevice = null
        isCasting.value = false
        castDeviceName.value = null
        cachedPositionMs.value = 0L
        cachedDurationMs.value = 0L
    }

    fun release() {
        pollJob?.cancel()
        scope.cancel()
    }
}
