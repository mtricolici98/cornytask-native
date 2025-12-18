package com.example.cornytask_v2.services

import android.util.Log
import com.example.cornytask_v2.features.user.UserRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val userRepository = UserRepository()

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Handle FCM messages here.
        // Not getting messages here? See README(https://goo.gl/39bRNJ) for more.
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // FCM registration token to your app server.
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String?) {
        if (token != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    userRepository.updateFcmToken(token)
                    Log.d(TAG, "FCM token updated successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating FCM token", e)
                }
            }
        }
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }
}
