package com.mobile.core.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mobile.core.data.R
import com.mobile.core.data.util.PlatformUtils
import com.mobile.core.domain.auth.SessionStorage
import com.mobile.core.domain.notification.DeviceTokenService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class GoZipoFirebaseMessagingService : FirebaseMessagingService() {

    private val deviceTokenService by inject<DeviceTokenService>()
    private val sessionStorage by inject<SessionStorage>()
    private val applicationScope by inject<CoroutineScope>()

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        applicationScope.launch {
            val authInfo = sessionStorage.observeAuthInfo().first()
            if (authInfo != null) {
                deviceTokenService.registerToken(
                    token = token,
                    platform = PlatformUtils.getOSName()
                )
            }
        }
    }

    /**
     * The backend sends Android pushes data-only (title/body/rideId/type in [RemoteMessage.data]),
     * so the OS never displays them on its own; this builds the notification.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.data[KEY_TITLE] ?: message.notification?.title ?: return
        val body = message.data[KEY_BODY] ?: message.notification?.body.orEmpty()
        val rideId = message.data[KEY_RIDE_ID]

        val notificationManager = NotificationManagerCompat.from(this)
        if (!notificationManager.areNotificationsEnabled()) return
        createRideChannel()

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            rideId?.let { putExtra(KEY_RIDE_ID, it) }
        }
        val contentIntent = launchIntent?.let {
            PendingIntent.getActivity(
                this,
                0,
                it,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val notification = NotificationCompat.Builder(this, RIDE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        // One notification per ride: a newer status replaces the previous one
        val notificationId = (rideId ?: message.messageId ?: title).hashCode()
        try {
            notificationManager.notify(notificationId, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS was revoked between the check and the call
        }
    }

    private fun createRideChannel() {
        val channel = NotificationChannel(
            RIDE_CHANNEL_ID,
            getString(R.string.notification_channel_ride_updates),
            NotificationManager.IMPORTANCE_HIGH
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private companion object {
        const val RIDE_CHANNEL_ID = "ride_updates"
        const val KEY_TITLE = "title"
        const val KEY_BODY = "body"
        const val KEY_RIDE_ID = "rideId"
    }
}
