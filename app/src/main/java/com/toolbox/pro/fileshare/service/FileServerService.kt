package com.toolbox.pro.fileshare.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.toolbox.pro.MainActivity
import com.toolbox.pro.R
import com.toolbox.pro.fileshare.server.FileServer
import com.toolbox.pro.fileshare.server.SharedFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FileServerService : Service() {

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "file_server_channel"
        private const val NOTIFICATION_ID = 1
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_UPDATE_FILES = "ACTION_UPDATE_FILES"
        const val EXTRA_PORT = "EXTRA_PORT"
        const val EXTRA_FILES_JSON = "EXTRA_FILES_JSON"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        @Volatile
        private var currentFiles: List<SharedFile> = emptyList()
    }

    private var fileServer: FileServer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val port = intent.getIntExtra(EXTRA_PORT, 8080)
                @Suppress("DEPRECATION")
                val files = intent.getSerializableExtra(EXTRA_FILES_JSON) as? ArrayList<SharedFile>
                if (files != null) {
                    currentFiles = files
                }
                startServer(port)
            }
            ACTION_UPDATE_FILES -> {
                @Suppress("DEPRECATION")
                val files = intent.getSerializableExtra(EXTRA_FILES_JSON) as? ArrayList<SharedFile>
                if (files != null) {
                    currentFiles = files
                    fileServer?.setFiles(files)
                }
            }
            ACTION_STOP -> {
                stopServer()
            }
        }
        return START_NOT_STICKY
    }

    private fun startServer(port: Int) {
        if (fileServer == null) {
            fileServer = FileServer(this, port)
            fileServer?.setFiles(currentFiles)
            fileServer?.start()
            _isRunning.value = true
            startForeground(NOTIFICATION_ID, createNotification("Server running on port $port"))
        } else {
            fileServer?.setFiles(currentFiles)
        }
    }

    private fun stopServer() {
        fileServer?.stop()
        fileServer = null
        _isRunning.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "File Server",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "File sharing server notification"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("ToolBox Pro")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        stopServer()
        super.onDestroy()
    }
}
