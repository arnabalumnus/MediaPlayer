package com.arnab.mediaplayer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arnab.mediaplayer.data.MediaRepository
import com.arnab.mediaplayer.data.model.AudioItem
import com.arnab.mediaplayer.util.ViewMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AudioViewModel(private val repository: MediaRepository) : ViewModel() {

    private val _audioItems = MutableStateFlow<List<AudioItem>>(emptyList())
    val audioItems: StateFlow<List<AudioItem>> = _audioItems.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.LIST)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun toggleViewMode() {
        _viewMode.value = if (_viewMode.value == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _audioItems.value = repository.scanAudio()
            _isLoading.value = false
        }
    }

    class Factory(private val repository: MediaRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AudioViewModel(repository) as T
    }
}
