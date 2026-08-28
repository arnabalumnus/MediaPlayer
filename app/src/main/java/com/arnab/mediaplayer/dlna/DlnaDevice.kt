package com.arnab.mediaplayer.dlna

/** A UPnP MediaRenderer discovered on the LAN (e.g. a smart TV's DLNA/"Smart View" service). */
data class DlnaDevice(
    val friendlyName: String,
    val location: String,
    val controlUrl: String
)
