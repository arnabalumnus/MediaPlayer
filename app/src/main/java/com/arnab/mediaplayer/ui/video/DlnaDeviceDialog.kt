package com.arnab.mediaplayer.ui.video

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arnab.mediaplayer.dlna.DlnaController
import com.arnab.mediaplayer.dlna.DlnaDevice

@Composable
fun DlnaDeviceDialog(controller: DlnaController, onDismiss: () -> Unit) {
    val isDiscovering by controller.isDiscovering
    val devices by controller.discoveredDevices
    val isCasting by controller.isCasting
    val castDeviceName by controller.castDeviceName

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cast to device") },
        text = {
            Column {
                if (isCasting) {
                    Text(
                        "Currently casting to ${castDeviceName ?: "device"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                when {
                    isDiscovering && devices.isEmpty() -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.size(12.dp))
                        Text("Searching your Wi-Fi network…")
                    }
                    devices.isEmpty() -> Text(
                        "No DLNA-compatible devices found. Make sure your TV is on, connected " +
                            "to the same Wi-Fi network, and that DLNA/network sharing is enabled on it."
                    )
                    else -> Column {
                        devices.forEach { device ->
                            DeviceRow(device) {
                                controller.connect(device)
                                onDismiss()
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isCasting) {
                TextButton(onClick = {
                    controller.stopCasting()
                    onDismiss()
                }) { Text("Disconnect") }
            } else {
                TextButton(onClick = { controller.discover() }) { Text("Rescan") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun DeviceRow(device: DlnaDevice, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Filled.Tv, contentDescription = null)
        Text(device.friendlyName)
    }
}
