package com.nobadhabbits.cornytask

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.nobadhabbits.cornytask.features.login.AuthSession
import com.nobadhabbits.cornytask.features.login.LoginActivity
import com.nobadhabbits.cornytask.features.main.MainMenuActivity
import com.nobadhabbits.cornytask.features.widget.OneTimeNotificationSchedulerWorker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Firebase persists the session across restarts; it is the only login check.
        if (AuthSession.isSignedIn()) {
            // Pass every extra along (mood / timer deep links), and reuse an already open
            // main screen instead of stacking a second one on top of it.
            val nextIntent = Intent(this, MainMenuActivity::class.java).apply {
                intent.extras?.let { putExtras(it) }
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(nextIntent)
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        enqueueOneTimeNotificationSchedulerWorker()

        finish()
    }

    private fun enqueueOneTimeNotificationSchedulerWorker() {
        val workRequest = OneTimeWorkRequestBuilder<OneTimeNotificationSchedulerWorker>().build()
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "one-time-notification-scheduler",
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }
}