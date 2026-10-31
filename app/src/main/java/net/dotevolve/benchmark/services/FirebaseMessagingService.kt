package net.dotevolve.benchmark.services

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "FirebaseMsgService"

    /**
     * Called when a message is received.
     * This is also called when a notification is received while the app is in the foreground.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        remoteMessage.data.isNotEmpty().let {
            Log.d(TAG, "Message data payload: " + remoteMessage.data)
            // Handle data payload here.
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            // Handle notification payload here by creating a local notification.
        }
    }

    /**
     * Called when the current app instance has been successfully registered with FCM.
     *
     * @param installationId The Firebase Installation ID used for sending messages to this app instance.
     */
    override fun onRegistered(installationId: String) {
        Log.d(TAG, "Registered token: $installationId")
        sendRegistrationToServer(installationId)
    }

    /**
     * Legacy callback retained for backward compatibility and Android Lint compliance.
     */
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String?) {
        Log.d(TAG, "sendRegistrationTokenToServer($token)")
    }
}
