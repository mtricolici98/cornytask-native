package com.nobadhabbits.cornytask.features.widget

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.nobadhabbits.cornytask.R

class TodoNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val todoTitle = intent.getStringExtra("todoTitle") ?: "Todo"
        val todoId = intent.getStringExtra("todoId") ?: ""
        val timeSlot = intent.getStringExtra("timeSlot") ?: "soon"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("todo_channel", "Todo Notifications", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, "todo_channel")
            .setContentTitle("Reminder")
            .setContentText("$todoTitle is due $timeSlot")
            .setSmallIcon(R.drawable.unicorn_logo)
            .build()

        notificationManager.notify(todoId.hashCode(), notification)
    }
}