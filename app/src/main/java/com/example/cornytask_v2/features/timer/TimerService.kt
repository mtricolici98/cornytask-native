package com.example.cornytask_v2.features.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.cornytask_v2.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TimerService : Service() {

    private val binder = TimerBinder()
    private val scope = CoroutineScope(Dispatchers.Default)
    private var timerJob: Job? = null

    private val _timeMillis = MutableStateFlow(0L)
    val timeMillis = _timeMillis.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning = _isRunning.asStateFlow()

    private var initialTime: Long = 0L

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "TimerChannel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder = binder

    fun setInitialTime(millis: Long) {
        initialTime = millis
        if (!_isRunning.value) {
            _timeMillis.value = initialTime
        }
    }

    fun toggle() {
        if (_isRunning.value) {
            pause()
        } else {
            start()
        }
    }

    private fun start() {
        if (_timeMillis.value == 0L && initialTime > 0L) {
            _timeMillis.value = initialTime
        }
        if (_timeMillis.value > 0) {
            _isRunning.value = true
            timerJob = scope.launch {
                while (_timeMillis.value > 0 && _isRunning.value) {
                    delay(1000)
                    _timeMillis.value -= 1000
                    updateNotification()
                }
                if (_timeMillis.value <= 0) {
                    _isRunning.value = false
                    stopForeground(true)
                }
            }
            startForeground(NOTIFICATION_ID, createNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        }
    }

    private fun pause() {
        _isRunning.value = false
        timerJob?.cancel()
        stopForeground(false)
        updateNotification()
    }

    fun reset() {
        _isRunning.value = false
        timerJob?.cancel()
        _timeMillis.value = initialTime
        stopForeground(true)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Timer"
            val descriptionText = "Timer notifications"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val timeFormatted = formatTime(_timeMillis.value)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Timer")
            .setContentText("Time remaining: $timeFormatted")
            .setSmallIcon(R.drawable.unicorn_small)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    private fun formatTime(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        val hours = (millis / (1000 * 60 * 60)) % 24
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }


    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }
}
