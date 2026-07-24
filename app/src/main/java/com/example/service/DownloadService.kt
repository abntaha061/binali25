package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.utils.getDownloadDirectory
import com.example.utils.mapYoutubeDLException
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

class DownloadService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    companion object {
        const val CHANNEL_ID = "download_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_DOWNLOAD = "ACTION_START_DOWNLOAD"
        const val ACTION_CANCEL_DOWNLOAD = "ACTION_CANCEL_DOWNLOAD"

        const val EXTRA_URL = "EXTRA_URL"
        const val EXTRA_FORMAT_ID = "EXTRA_FORMAT_ID"
        const val EXTRA_SUB_LANGS = "EXTRA_SUB_LANGS"
        const val EXTRA_EMBED_SUBS = "EXTRA_EMBED_SUBS"

        private var currentProcessId: String? = null

        fun start(
            context: Context,
            url: String,
            formatId: String,
            subLangs: ArrayList<String> = arrayListOf(),
            embedSubs: Boolean = false
        ) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_START_DOWNLOAD
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_FORMAT_ID, formatId)
                putStringArrayListExtra(EXTRA_SUB_LANGS, subLangs)
                putExtra(EXTRA_EMBED_SUBS, embedSubs)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_CANCEL_DOWNLOAD
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_DOWNLOAD -> {
                val url = intent.getStringExtra(EXTRA_URL) ?: return START_NOT_STICKY
                val formatId = intent.getStringExtra(EXTRA_FORMAT_ID) ?: "best"
                val subLangs = intent.getStringArrayListExtra(EXTRA_SUB_LANGS) ?: arrayListOf()
                val embedSubs = intent.getBooleanExtra(EXTRA_EMBED_SUBS, false)

                startForeground(NOTIFICATION_ID, buildNotification("جاري بدء التحميل...", 0, true))
                performDownload(url, formatId, subLangs, embedSubs)
            }
            ACTION_CANCEL_DOWNLOAD -> {
                cancelDownload()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun performDownload(
        url: String,
        formatId: String,
        subLangs: List<String>,
        embedSubs: Boolean
    ) {
        serviceScope.launch {
            val downloadDir = getDownloadDirectory(applicationContext)
            val outputPattern = File(downloadDir, "%(title)s.%(ext)s").absolutePath

            val request = YoutubeDLRequest(url).apply {
                addOption("-f", "$formatId+bestaudio/best")
                addOption("--merge-output-format", "mp4")
                addOption("-o", outputPattern)

                if (subLangs.isNotEmpty()) {
                    addOption("--write-subs")
                    addOption("--write-auto-subs")
                    addOption("--sub-langs", subLangs.joinToString(","))
                    addOption("--convert-subs", "srt")
                    if (embedSubs) {
                        addOption("--embed-subs")
                    }
                }
            }

            val processId = "download_${System.currentTimeMillis()}"
            currentProcessId = processId

            try {
                YoutubeDL.getInstance().execute(request, processId) { progress, etaInSeconds, line ->
                    // تمييز مرحلة الدمج (Merging) بناءً على مخرجات yt-dlp
                    val isMerging = line?.contains("Merging", ignoreCase = true) == true ||
                            line?.contains("ffmpeg", ignoreCase = true) == true

                    val statusMessage = if (isMerging) {
                        "جاري دمج الصوت والفيديو والترجمة..."
                    } else {
                        "جاري التحميل: ${progress.toInt()}% (متبقي $etaInSeconds ثانية)"
                    }

                    updateNotification(statusMessage, progress.toInt(), false)
                }

                showCompletionNotification("تم تحميل ودمج الفيديو بنجاح! 🎉")
            } catch (e: Exception) {
                val errorMessage = mapYoutubeDLException(e)
                showErrorNotification(errorMessage)
            } finally {
                currentProcessId = null
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
            }
        }
    }

    private fun cancelDownload() {
        currentProcessId?.let { pid ->
            try {
                YoutubeDL.getInstance().destroyProcessById(pid)
            } catch (_: Exception) {}
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تحميلات الفيديو",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعارات تقدم تحميل ودمج الفيديوهات"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String, progress: Int, indeterminate: Boolean) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("محيمل يوتيوب")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, indeterminate)
            .setOngoing(true)
            .setContentIntent(getPendingIntent())
            .build()

    private fun updateNotification(contentText: String, progress: Int, indeterminate: Boolean) {
        val notification = buildNotification(contentText, progress, indeterminate)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showCompletionNotification(message: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("إكتمال التحميل")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .setContentIntent(getPendingIntent())
            .build()
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun showErrorNotification(error: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("فشل التحميل")
            .setContentText(error)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .setContentIntent(getPendingIntent())
            .build()
        notificationManager.notify(NOTIFICATION_ID + 2, notification)
    }

    private fun getPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }
}
