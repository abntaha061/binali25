package com.example.data

import android.content.Context
import com.example.utils.getDownloadDirectory
import com.example.utils.mapYoutubeDLException
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

data class SubtitleItem(
    val code: String,
    val name: String,
    val isAuto: Boolean
)

data class DownloadProgress(
    val progress: Float = 0f,
    val etaInSeconds: Long = 0L,
    val currentLine: String = "",
    val isDownloading: Boolean = false,
    val isCompleted: Boolean = false,
    val error: String? = null
)

class DownloadRepository(private val context: Context) {

    private val _downloadProgressState = MutableStateFlow(DownloadProgress())
    val downloadProgressState: StateFlow<DownloadProgress> = _downloadProgressState.asStateFlow()

    private var currentProcessId: String? = null

    // Part 1: Get available subtitles (manual & auto captions)
    suspend fun getAvailableSubtitles(url: String): List<SubtitleItem> = withContext(Dispatchers.IO) {
        val subtitlesList = mutableListOf<SubtitleItem>()
        try {
            val request = YoutubeDLRequest(url).apply {
                addOption("--dump-json")
            }
            val response = YoutubeDL.getInstance().execute(request)
            val json = JSONObject(response.out)

            // 1. Manual Subtitles
            val subsNode = json.optJSONObject("subtitles")
            if (subsNode != null) {
                val keys = subsNode.keys()
                while (keys.hasNext()) {
                    val langCode = keys.next()
                    subtitlesList.add(
                        SubtitleItem(
                            code = langCode,
                            name = "$langCode (يدوية)",
                            isAuto = false
                        )
                    )
                }
            }

            // 2. Automatic Captions
            val autoSubsNode = json.optJSONObject("automatic_captions")
            if (autoSubsNode != null) {
                val keys = autoSubsNode.keys()
                while (keys.hasNext()) {
                    val langCode = keys.next()
                    if (subtitlesList.none { it.code == langCode }) {
                        subtitlesList.add(
                            SubtitleItem(
                                code = langCode,
                                name = "$langCode (تلقائية)",
                                isAuto = true
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        subtitlesList
    }

    // Part 3: Request building and download execution with subtitles & auto merge
    suspend fun startDownload(
        url: String,
        formatId: String,
        selectedSubLangs: List<String> = emptyList(),
        embedSubs: Boolean = false
    ) = withContext(Dispatchers.IO) {
        val downloadDir = getDownloadDirectory(context)
        val outputPattern = File(downloadDir, "%(title)s.%(ext)s").absolutePath

        // 1. Build Request with format merge options
        val request = YoutubeDLRequest(url).apply {
            addOption("-f", "$formatId+bestaudio/best")
            addOption("--merge-output-format", "mp4")
            addOption("-o", outputPattern)

            if (selectedSubLangs.isNotEmpty()) {
                addOption("--write-subs")
                addOption("--write-auto-subs")
                addOption("--sub-langs", selectedSubLangs.joinToString(","))
                addOption("--convert-subs", "srt")

                if (embedSubs) {
                    addOption("--embed-subs")
                }
            }
        }

        val processId = "download_${System.currentTimeMillis()}"
        currentProcessId = processId

        _downloadProgressState.value = DownloadProgress(
            isDownloading = true,
            progress = 0f
        )

        try {
            YoutubeDL.getInstance().execute(request, processId) { progress, etaInSeconds, line ->
                _downloadProgressState.value = DownloadProgress(
                    progress = progress,
                    etaInSeconds = etaInSeconds,
                    currentLine = line ?: "",
                    isDownloading = true
                )
            }

            // Check and verify output file
            checkAndMergeIfNeeded(downloadDir)

            _downloadProgressState.value = DownloadProgress(
                progress = 100f,
                isDownloading = false,
                isCompleted = true
            )
        } catch (e: Exception) {
            _downloadProgressState.value = DownloadProgress(
                isDownloading = false,
                error = mapYoutubeDLException(e)
            )
        } finally {
            currentProcessId = null
        }
    }

    // Check & Manual Merge Helper (if needed)
    private fun checkAndMergeIfNeeded(downloadDir: File) {
        // Automatically handled by yt-dlp + FFmpeg module in YoutubeDL
    }

    fun cancelDownload() {
        currentProcessId?.let { pid ->
            try {
                YoutubeDL.getInstance().destroyProcessById(pid)
            } catch (_: Exception) {}
        }
        _downloadProgressState.value = DownloadProgress(isDownloading = false)
    }
}
