package app.coreme.messenger.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import app.coreme.messenger.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun showMessageNotification(senderName: String, message: String, chatId: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("chatId", chatId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, chatId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(senderName)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        manager.notify(chatId.hashCode(), notification)
    }

    fun showIncomingCallNotification(callerName: String, callId: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("callId", callId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, NOTIFICATION_ID_CALL, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_CALLS)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle("Incoming call")
            .setContentText(callerName)
            .setAutoCancel(true)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
        manager.notify(NOTIFICATION_ID_CALL, notification)
    }

    fun dismissCallNotification() {
        manager.cancel(NOTIFICATION_ID_CALL)
    }

    companion object {
        const val CHANNEL_MESSAGES = "coreme_messages"
        const val CHANNEL_CALLS = "coreme_calls"
        const val CHANNEL_CHANNELS = "coreme_channels"
        private const val NOTIFICATION_ID_CALL = 9001

        fun createChannels(context: Context) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            listOf(
                NotificationChannel(CHANNEL_MESSAGES, "Messages", NotificationManager.IMPORTANCE_HIGH),
                NotificationChannel(CHANNEL_CALLS, "Calls", NotificationManager.IMPORTANCE_MAX),
                NotificationChannel(CHANNEL_CHANNELS, "Channel Posts", NotificationManager.IMPORTANCE_DEFAULT),
            ).forEach { manager.createNotificationChannel(it) }
        }
    }
}
