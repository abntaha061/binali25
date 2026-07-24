package com.example.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class VideoInfo(
    val title: String,
    val thumbnailUrl: String
)

sealed interface UiState {
    object Idle : UiState
    object Loading : UiState
    data class Success(val videoInfo: VideoInfo) : UiState
    data class Error(val message: String) : UiState
}

class VideoViewModel : ViewModel() {
    val urlState = MutableStateFlow("")

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun updateUrl(newUrl: String) {
        urlState.value = newUrl
    }

    fun fetchVideoInfo() {
        val currentUrl = urlState.value
        if (currentUrl.isBlank()) return

        _uiState.value = UiState.Loading

        // TODO: Implement YoutubeDL.getInstance().getInfo(url) in background thread
    }
}
