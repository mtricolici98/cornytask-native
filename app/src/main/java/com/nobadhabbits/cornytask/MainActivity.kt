package com.nobadhabbits.cornytask

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.nobadhabbits.cornytask.features.login.LoginActivity
import com.nobadhabbits.cornytask.features.main.MainMenuActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.nobadhabbits.cornytask.features.widget.OneTimeNotificationSchedulerWorker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val account = GoogleSignIn.getLastSignedInAccount(this)

        if (account != null) {
            val nextIntent = Intent(this, MainMenuActivity::class.java)
            nextIntent.putExtra("timeGoalId", intent.getStringExtra("timeGoalId"))
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