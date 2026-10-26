package com.nobadhabbits.cornytask.features.login

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.google.firebase.auth.FirebaseAuth
import com.nobadhabbits.cornytask.features.time_goals.TimeGoalManager
import com.nobadhabbits.cornytask.features.widget.ACTION_DATA_UPDATED
import com.nobadhabbits.cornytask.features.widget.TodoWidget
import com.nobadhabbits.cornytask.features.widget.TodoWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Firebase is the single source of truth for "is someone logged in".
 * Credential Manager is only used on the login screen to get a Google ID token.
 */
object AuthSession {

    // Not tied to an Activity, so sign-out finishes even if the screen goes away.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun isSignedIn(): Boolean = FirebaseAuth.getInstance().currentUser != null

    /**
     * Full sign-out: stops the timer, clears the widget, forgets the Google account choice
     * (so the login screen won't auto-pick it) and signs out of Firebase.
     * Navigation to the login screen happens via [MainMenuActivity]'s auth listener.
     */
    fun signOut(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            TimeGoalManager.reset()

            try {
                GlanceAppWidgetManager(appContext).getGlanceIds(TodoWidget::class.java)
                    .forEach { glanceId -> updateAppWidgetState(appContext, glanceId) { it.clear() } }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear widget state", e)
            }

            try {
                CredentialManager.create(appContext).clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear credential state", e)
            }

            FirebaseAuth.getInstance().signOut()

            appContext.sendBroadcast(
                Intent(appContext, TodoWidgetReceiver::class.java).setAction(ACTION_DATA_UPDATED)
            )
        }
    }

    fun loginIntent(context: Context): Intent =
        Intent(context, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

    private const val TAG = "AuthSession"
}
