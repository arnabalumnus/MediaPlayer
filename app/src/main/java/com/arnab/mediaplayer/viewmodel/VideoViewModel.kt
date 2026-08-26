package com.arnab.mediaplayer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arnab.mediaplayer.data.MediaRepository
import com.arnab.mediaplayer.data.model.VideoItem
import com.arnab.mediaplayer.util.ViewMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VideoViewModel(private val repository: MediaRepository) : ViewModel() {

    private val _videoItems = MutableStateFlow<List<VideoItem>>(emptyList())
    val videoItems: StateFlow<List<VideoItem>> = _videoItems.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.GRID)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun toggleViewMode() {
        _viewMode.value = if (_viewMode.value == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _videoItems.value = repository.scanVideo()
            _isLoading.value = false
        }
    }

    class Factory(private val repository: MediaRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            VideoViewModel(repository) as T
    }
}
