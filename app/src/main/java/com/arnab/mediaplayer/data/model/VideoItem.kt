package com.arnab.mediaplayer.data.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class VideoItem(
    val id: Long,
    val uri: Uri,
    val title: String,
    val durationMs: Long,
    val widthPx: Int,
    val heightPx: Int
) : Parcelable
