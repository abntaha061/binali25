package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DownloadProgress
import com.example.data.DownloadRepository
import com.example.data.SubtitleItem
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FormatItem(
    val formatId: String,
    val resolution: String,
    val ext: String,
    val fileSizeApprox: Long,
    val note: String
)

data class VideoDetails(
    val url: String,
    val title: String,
    val duration: Int,
    val thumbnailUrl: String,
    val formats: List<FormatItem>,
    val subtitles: List<SubtitleItem> = emptyList()
)

sealed interface VideoInfoState {
    object Idle : VideoInfoState
    object Loading : VideoInfoState
    data class Success(val videoDetails: VideoDetails) : VideoInfoState
    data class Error(val message: String) : VideoInfoState
}

class VideoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DownloadRepository(application)

    val urlState = MutableStateFlow("")

    private val _videoInfoState = MutableStateFlow<VideoInfoState>(VideoInfoState.Idle)
    val videoInfoState: StateFlow<VideoInfoState> = _videoInfoState.asStateFlow()

    val downloadProgressState: StateFlow<DownloadProgress> = repository.downloadProgressState

    val selectedSubLanguages = MutableStateFlow<Set<String>>(emptySet())
    val embedSubtitles = MutableStateFlow(false)

    private var downloadJob: Job? = null

    fun updateUrl(newUrl: String) {
        urlState.value = newUrl
    }

    fun toggleSubtitleLanguage(langCode: String) {
        val current = selectedSubLanguages.value.toMutableSet()
        if (current.contains(langCode)) {
            current.remove(langCode)
        } else {
            current.add(langCode)
        }
        selectedSubLanguages.value = current
    }

    fun setEmbedSubtitles(embed: Boolean) {
        embedSubtitles.value = embed
    }

    fun fetchVideoInfo(url: String = urlState.value) {
        if (url.isBlank()) return

        _videoInfoState.value = VideoInfoState.Loading
        selectedSubLanguages.value = emptySet()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val info: VideoInfo = YoutubeDL.getInstance().getInfo(url)

                val filteredFormats = info.formats
                    ?.filter { format ->
                        val vcodec = format.vcodec
                        vcodec != null && vcodec != "none"
                    }
                    ?.map { format ->
                        val noteStr = format.formatNote ?: ""
                        val resStr = when {
                            format.height > 0 -> "${format.height}p"
                            noteStr.isNotBlank() -> noteStr
                            !format.format.isNullOrEmpty() -> format.format ?: "SD"
                            else -> "SD"
                        }
                        val size = if (format.fileSize > 0) {
                            format.fileSize
                        } else {
                            format.fileSizeApproximate
                        }
                        FormatItem(
                            formatId = format.formatId ?: "",
                            resolution = resStr,
                            ext = format.ext ?: "mp4",
                            fileSizeApprox = size,
                            note = noteStr
                        )
                    }
                    ?.distinctBy { it.resolution }
                    ?.sortedByDescending { item ->
                        item.resolution.filter { it.isDigit() }.toIntOrNull() ?: 0
                    } ?: emptyList()

                val availableSubtitles = repository.getAvailableSubtitles(url)

                val details = VideoDetails(
                    url = url,
                    title = info.title ?: "بدون عنوان",
                    duration = info.duration,
                    thumbnailUrl = info.thumbnail ?: "",
                    formats = filteredFormats,
                    subtitles = availableSubtitles
                )

                _videoInfoState.value = VideoInfoState.Success(details)
            } catch (e: Exception) {
                _videoInfoState.value = VideoInfoState.Error(
                    e.localizedMessage ?: "فشل في جلب معلومات الفيديو"
                )
            }
        }
    }

    fun startDownload(formatId: String) {
        val currentState = _videoInfoState.value
        if (currentState !is VideoInfoState.Success) return

        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            repository.startDownload(
                url = currentState.videoDetails.url,
                formatId = formatId,
                selectedSubLangs = selectedSubLanguages.value.toList(),
                embedSubs = embedSubtitles.value
            )
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        repository.cancelDownload()
    }
}
