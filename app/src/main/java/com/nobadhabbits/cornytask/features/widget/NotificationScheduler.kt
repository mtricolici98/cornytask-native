package com.nobadhabbits.cornytask.features.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.nobadhabbits.cornytask.data.Todo
import java.util.Calendar

class NotificationScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleNotifications(todo: Todo) {
        val dueDate = todo.dueDate ?: return

        val fiveHoursBefore = Calendar.getInstance()
        fiveHoursBefore.time = dueDate
        fiveHoursBefore.add(Calendar.HOUR, -5)

        val oneHourBefore = Calendar.getInstance()
        oneHourBefore.time = dueDate
        oneHourBefore.add(Calendar.HOUR, -1)

        val fiveMinutesBefore = Calendar.getInstance()
        fiveMinutesBefore.time = dueDate
        fiveMinutesBefore.add(Calendar.MINUTE, -5)

        scheduleNotification(todo, fiveHoursBefore.timeInMillis, 1)
        scheduleNotification(todo, oneHourBefore.timeInMillis, 2)
        scheduleNotification(todo, fiveMinutesBefore.timeInMillis, 3)
    }

    fun cancelNotifications(todo: Todo) {
        cancelNotification(todo, 1)
        cancelNotification(todo, 2)
        cancelNotification(todo, 3)
    }

    private fun scheduleNotification(todo: Todo, time: Long, requestCodeSuffix: Int) {
        if (time < System.currentTimeMillis()) return
        var timesLot = "in 5 minutes";
        if (requestCodeSuffix == 1){
            timesLot = "in 5 hours";
        } else if (requestCodeSuffix == 2) {
            timesLot = "in 1 hour";
        }
        val intent = Intent(context, TodoNotificationReceiver::class.java).apply {
            putExtra("todoTitle", todo.title)
            putExtra("todoId", todo.id)
            putExtra("timeSlot", timesLot)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            todo.id.hashCode() + requestCodeSuffix,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, pendingIntent)
    }

    private fun cancelNotification(todo: Todo, requestCodeSuffix: Int) {
        val intent = Intent(context, TodoNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            todo.id.hashCode() + requestCodeSuffix,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let { alarmManager.cancel(it) }
    }
}
