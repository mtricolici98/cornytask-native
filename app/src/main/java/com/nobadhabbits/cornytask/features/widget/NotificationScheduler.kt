package com.nobadhabbits.cornytask.features.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.nobadhabbits.cornytask.data.Todo
import java.util.Calendar

class NotificationScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /* =========================
       TODO REMINDERS (Existing)
       ========================= */

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

        var timesLot = "in 5 minutes"
        if (requestCodeSuffix == 1) {
            timesLot = "in 5 hours"
        } else if (requestCodeSuffix == 2) {
            timesLot = "in 1 hour"
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

    /* =========================
       MOOD DAILY REMINDERS
       ========================= */

    fun scheduleDailyMoodReminders() {
        scheduleDailyMoodReminder(hour = 8, minute = 0, requestCode = 1001, label = "Morning")
        scheduleDailyMoodReminder(hour = 13, minute = 0, requestCode = 1002, label = "Afternoon")
        scheduleDailyMoodReminder(hour = 20, minute = 0, requestCode = 1003, label = "Evening")
    }

    fun cancelDailyMoodReminders() {
        cancelDailyMoodReminder(1001)
        cancelDailyMoodReminder(1002)
        cancelDailyMoodReminder(1003)
    }

    private fun scheduleDailyMoodReminder(
        hour: Int,
        minute: Int,
        requestCode: Int,
        label: String
    ) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // If time already passed today → schedule tomorrow
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, MoodNotificationReceiver::class.java).apply {
            putExtra("timeOfDayLabel", label)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    private fun scheduleWaterReminder(
        hour: Int,
        minute: Int,
        requestCode: Int,
        label: String
    ) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // If time already passed today → schedule tomorrow
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, DrinkSomeWaterReceiver::class.java).apply {
            putExtra("timeOfDayLabel", label)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    private fun cancelDailyMoodReminder(requestCode: Int) {
        val intent = Intent(context, MoodNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let { alarmManager.cancel(it) }
    }

    fun  cancelWaterReminders() {
        val intent = Intent(context, DrinkSomeWaterReceiver::class.java)
        for (rc in (1004..1009)) {
            val pendingIntent = PendingIntent.getBroadcast(
                context,
            rc,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
            pendingIntent?.let { alarmManager.cancel(it) }
        }
    }

    fun  scheduleWaterReminders() {
        scheduleWaterReminder(hour = 9, minute = 0, requestCode = 1004, label = "Morning")
        scheduleWaterReminder(hour = 11, minute = 0, requestCode = 1005, label = "Afternoon")
        scheduleWaterReminder(hour = 14, minute = 0, requestCode = 1007, label = "Evening")
        scheduleWaterReminder(hour = 17, minute = 0, requestCode = 1008, label = "Evening")
        scheduleWaterReminder(hour = 20, minute = 0, requestCode = 1009, label = "Evening")
    }
}