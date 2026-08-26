package com.arnab.mediaplayer.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object PermissionUtils {

    /** Permissions required to read audio/video from shared storage, by API level. */
    fun requiredMediaPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    /** Notification permission is only meaningful on API 33+. */
    fun notificationPermissionOrNull(): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else null

    fun allPermissions(): Array<String> =
        requiredMediaPermissions() + listOfNotNull(notificationPermissionOrNull())

    fun hasAllPermissions(context: Context): Boolean =
        allPermissions().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
}
