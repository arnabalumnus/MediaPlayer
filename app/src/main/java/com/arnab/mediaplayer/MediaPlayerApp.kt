package com.arnab.mediaplayer

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.arnab.mediaplayer.data.MediaRepository

class MediaPlayerApp : Application(), ImageLoaderFactory {

    lateinit var mediaRepository: MediaRepository
        private set

    override fun onCreate() {
        super.onCreate()
        mediaRepository = MediaRepository(this)
    }

    /** Registers a video-frame decoder so Coil can render thumbnails straight from video URIs. */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .build()
}
