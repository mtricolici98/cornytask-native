package com.nobadhabbits.cornytask.features.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class TodoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodoWidget()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        enqueueDataUpdateWorker(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_DATA_UPDATED) {
            enqueueDataUpdateWorker(context)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        val workRequest = PeriodicWorkRequestBuilder<TodoWidgetWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "TodoWidgetWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WorkManager.getInstance(context).cancelUniqueWork("TodoWidgetWorker")
    }

    companion object {
        private const val ACTION_DATA_UPDATED = "com.nobadhabbits.cornytask.ACTION_DATA_UPDATED"
    }
}

fun enqueueDataUpdateWorker(context: Context) {
    val workRequest = OneTimeWorkRequestBuilder<TodoWidgetWorker>()
        .build()
    WorkManager.getInstance(context).enqueue(workRequest)
}
