package com.example.cornytask_v2.features.time_goals

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
import com.example.cornytask_v2.MainActivity
import com.example.cornytask_v2.R
import com.example.cornytask_v2.data.TimeGoal
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

    companion object {
        const val ACTION_START = "com.example.cornytask_v2.features.time_goals.ACTION_START"
        const val ACTION_STOP = "com.example.cornytask_v2.features.time_goals.ACTION_STOP"
        const val EXTRA_TIME_GOAL_ID = "extra_time_goal_id"
        private const val NOTIFICATION_ID = 1
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
                    is TimeGoalManager.TimerState.Running -> showNotification(state.goalId, state.remainingMillis)
                    is TimeGoalManager.TimerState.Idle, is TimeGoalManager.TimerState.Finished -> stopService()
                }
            }
        }
    }

    private fun stopService() {
        stopForeground(true)
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val timeGoalId = intent.getStringExtra(EXTRA_TIME_GOAL_ID)
                serviceScope.launch {
                    val timeGoal = timeGoalRepository.getTimeGoalsFlow().first().find { it.id == timeGoalId } ?: return@launch
                    val durationMillis = TimeUnit.MINUTES.toMillis(timeGoal.remainingTimeMinutes)
                    TimeGoalManager.startTimer(timeGoal, durationMillis)
                }
            }
            ACTION_STOP -> TimeGoalManager.stopTimer()
        }
        return START_NOT_STICKY
    }

    private fun showNotification(goalId: String, remainingMillis: Long) {
        serviceScope.launch {
            val timeGoal = timeGoalRepository.getTimeGoalsFlow().first().find { it.id == goalId } ?: return@launch
            val notification = createNotification(timeGoal, remainingMillis)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, "Time Goal Timer", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(timeGoal: TimeGoal, remainingMillis: Long): android.app.Notification {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingMillis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(remainingMillis) % 60
        val timeString = String.format("%02d:%02d", minutes, seconds)

        val notificationIntent = Intent(this, MainActivity::class.java)
        notificationIntent.putExtra("timeGoalId", timeGoal.id)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("${timeGoal.title}: $timeString remaining")
            .setSmallIcon(R.drawable.unicorn_logo)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
