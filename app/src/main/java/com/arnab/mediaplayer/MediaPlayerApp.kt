package com.arnab.mediaplayer

import android.app.Application
import com.arnab.mediaplayer.data.MediaRepository

class MediaPlayerApp : Application() {

    lateinit var mediaRepository: MediaRepository
        private set

    override fun onCreate() {
        super.onCreate()
        mediaRepository = MediaRepository(this)
    }
}
