package com.nobadhabbits.cornytask.features.time_goals

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.nobadhabbits.cornytask.R
import com.nobadhabbits.cornytask.data.TimeGoal
import com.nobadhabbits.cornytask.features.main.MainMenuActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class TimeGoalService : Service() {

    private lateinit var notificationManager: NotificationManager
    private val timeGoalRepository = TimeGoalRepository()
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var isForeground = false

    companion object {
        const val ACTION_START = "com.nobadhabbits.cornytask.features.time_goals.ACTION_START"
        const val ACTION_STOP = "com.nobadhabbits.cornytask.features.time_goals.ACTION_STOP"
        const val EXTRA_TIME_GOAL_ID = "extra_time_goal_id"
        const val EXTRA_DURATION_MINUTES = "extra_duration_minutes"
        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_ID_COMPLETE = 2
        private const val NOTIFICATION_CHANNEL_ID = "time_goal_channel"
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        observeTimerState()
    }

    private fun observeTimerState() {
        serviceScope.launch {
            TimeGoalManager.timerState.collect { state ->
                when (state) {
                    is TimeGoalManager.TimerState.Running -> showOrUpdateNotification(state.goal, state.remainingMillis, state.totalDurationMillis)
                    is TimeGoalManager.TimerState.Idle -> {
                        if (isForeground) {
                            stopService()
                        }
                    }
                    is TimeGoalManager.TimerState.Finished -> {
                        showCompletedNotification(state.goal, state.totalDurationMillis)
                        if (isForeground) {
                            stopService()
                        }
                    }
                }
            }
        }
    }

    private fun stopService() {
        isForeground = false
        stopForeground(true)
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val timeGoalId = intent.getStringExtra(EXTRA_TIME_GOAL_ID)
                val durationMinutes = intent.getLongExtra(EXTRA_DURATION_MINUTES, 0)
                serviceScope.launch {
                    val timeGoal = timeGoalRepository.getTimeGoalsFlow().first().find { it.id == timeGoalId } ?: return@launch
                    if (durationMinutes > 0) {
                        val durationMillis = TimeUnit.MINUTES.toMillis(durationMinutes)
                        TimeGoalManager.startTimer(timeGoal, durationMillis)
                    }
                }
            }
            ACTION_STOP -> TimeGoalManager.stopTimer()
        }
        return START_NOT_STICKY
    }

    private fun showOrUpdateNotification(timeGoal: TimeGoal, remainingMillis: Long, totalDurationMillis: Long) {
        val notification = createNotification(timeGoal, remainingMillis, totalDurationMillis)

        if (!isForeground) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            isForeground = true
        } else {
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }


    private fun showCompletedNotification(timeGoal: TimeGoal, totalDurationMillis: Long) {
        val notification = createCompletedNotification(timeGoal, totalDurationMillis)

        if (!isForeground) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID_COMPLETE, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(NOTIFICATION_ID_COMPLETE, notification)
            }
            isForeground = true
        } else {
            notificationManager.notify(NOTIFICATION_ID_COMPLETE, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, "Time Goal Timer", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(timeGoal: TimeGoal, remainingMillis: Long, totalDurationMillis: Long): android.app.Notification {
        val timeString = if (remainingMillis >= TimeUnit.HOURS.toMillis(1)) {
            val hours = TimeUnit.MILLISECONDS.toHours(remainingMillis)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingMillis) % 60
            String.format("%02d:%02d", hours, minutes)
        } else {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingMillis)
            val seconds = TimeUnit.MILLISECONDS.toSeconds(remainingMillis) % 60
            String.format("%02d:%02d", minutes, seconds)
        }

        val notificationIntent = Intent(this, MainMenuActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("timeGoalId", timeGoal.id)
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("${timeGoal.title}: $timeString remaining")
            .setSmallIcon(R.drawable.unicorn_logo)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setProgress(totalDurationMillis.toInt(), (totalDurationMillis - remainingMillis).toInt(), false)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun createCompletedNotification(timeGoal: TimeGoal, totalDurationMillis: Long): android.app.Notification {
        val timeString = if (totalDurationMillis >= TimeUnit.HOURS.toMillis(1)) {
            val hours = TimeUnit.MILLISECONDS.toHours(totalDurationMillis)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(totalDurationMillis) % 60
            String.format("%d hours and %d minutes", hours, minutes)
        } else {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(totalDurationMillis)
            String.format("%d minutes", minutes)
        }


        val notificationIntent = Intent(this, MainMenuActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("timeGoalId", timeGoal.id)
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("${timeGoal.title}: $timeString completed")
            .setContentText("Well done!")
            .setSmallIcon(R.drawable.unicorn_logo)
            .setContentIntent(pendingIntent)
            .setOngoing(false)
            .setOnlyAlertOnce(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
