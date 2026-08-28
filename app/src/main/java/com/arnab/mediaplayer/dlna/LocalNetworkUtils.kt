package com.arnab.mediaplayer.dlna

import java.net.Inet4Address
import java.net.NetworkInterface

object LocalNetworkUtils {

    /** The device's LAN IPv4 address (e.g. over Wi-Fi), so a DLNA renderer on the same network can reach us. */
    fun getLocalIpAddress(): String? = try {
        NetworkInterface.getNetworkInterfaces().asSequence()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.asSequence() }
            .filterIsInstance<Inet4Address>()
            .firstOrNull()
            ?.hostAddress
    } catch (e: Exception) {
        null
    }
}
