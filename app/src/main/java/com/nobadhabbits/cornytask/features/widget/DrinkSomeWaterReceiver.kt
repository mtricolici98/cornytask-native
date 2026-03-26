package com.nobadhabbits.cornytask.features.widget

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.nobadhabbits.cornytask.MainActivity
import com.nobadhabbits.cornytask.R

class DrinkSomeWaterReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val label = intent.getStringExtra(EXTRA_TIME_OF_DAY_LABEL) ?: "Mood check"
        showMoodNotification(context, label)
    }

    private fun showMoodNotification(context: Context, label: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Tap action -> open the app (MainActivity). You can route to AddMoodScreen via Nav later.
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_ADD_MOOD, true)
        }

        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_OPEN_APP,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Notification channel (required on Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders to drink drink some water"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val title = "Stay Hydrated"
        val message = "It's time to drink some water."

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.unicorn_small) // change if you have a proper notif icon
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(NOTIFICATION_ID_BASE + label.hashCode(), notification)
    }

    companion object {
        const val EXTRA_TIME_OF_DAY_LABEL = "timeOfDayLabel"
        const val EXTRA_OPEN_ADD_MOOD = ""

        private const val CHANNEL_ID = "water_reminders"
        private const val CHANNEL_NAME = "Water reminders"

        private const val REQUEST_OPEN_APP = 5001
        private const val NOTIFICATION_ID_BASE = 9000
    }
}