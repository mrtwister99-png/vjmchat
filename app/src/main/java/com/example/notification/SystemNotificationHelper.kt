package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.example.MainActivity
import com.example.R
import com.example.ui.priorityPrefix

object SystemNotificationHelper {
    const val CHANNEL_CHAT = "vjm_chat"
    const val CHANNEL_SECRET = "vjm_secret"
    const val CHANNEL_TASK = "vjm_task"
    const val CHANNEL_IDEA = "vjm_idea"
    const val CHANNEL_CALENDAR = "vjm_calendar"
    const val CHANNEL_COMMENT = "vjm_comment"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notifSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val notifAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val channels = listOf(
                NotificationChannel(CHANNEL_CHAT, "Chat", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Nové zprávy v chatu"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 250, 100, 250)
                    setSound(notifSound, notifAttrs)
                },
                NotificationChannel(CHANNEL_SECRET, "Secret chat", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Tajné zprávy Tom <-> Adélka"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 120, 80, 120, 80, 120)
                    setSound(null, null) // ticho, jen vibrace
                },
                NotificationChannel(CHANNEL_TASK, "Úkoly a poznámky", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Nové úkoly a poznámky"
                    enableVibration(false)
                    setSound(notifSound, notifAttrs)
                },
                NotificationChannel(CHANNEL_IDEA, "Nápady", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Nové nápady a realizace"
                    enableVibration(false)
                    setSound(notifSound, notifAttrs)
                },
                NotificationChannel(CHANNEL_CALENDAR, "Kalendář", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Události v kalendáři"
                    enableVibration(false)
                    setSound(notifSound, notifAttrs)
                },
                NotificationChannel(CHANNEL_COMMENT, "Komentáře / Diskuze", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Komentáře k úkolům, nápadům, kalendáři"
                    enableVibration(true)
                    setSound(notifSound, notifAttrs)
                }
            )
            nm.createNotificationChannels(channels)
        }
    }

    fun showNotification(
        context: Context,
        channelId: String,
        title: String,
        message: String,
        notificationId: Int,
        category: String,
        priority: String = "NONE"
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notif_category", category)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val priorityColor = when (priority) {
            "HIGH" -> 0xFFEF4444.toInt()
            "MEDIUM" -> 0xFFF97316.toInt()
            "LOW" -> 0xFFEAB308.toInt()
            else -> 0xFF3B82F6.toInt()
        }
        val prefix = priorityPrefix(priority)

        // barva pozadí podle kategorie - diskuze fialová, secret = stejná jako chat zelená
        val isSecret = channelId == CHANNEL_SECRET || category.equals("SECRET", true) || category.equals("SECRET_CHAT", true)
        val safeTitle = if (isSecret) "🔒 Secret chat" else "$prefix $title"
        val safeMessage = if (isSecret) "Nová tajná zpráva" else "$prefix $message"

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(safeTitle)
            .setContentText(safeMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(safeMessage))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(
                when (priority) {
                    "HIGH" -> NotificationCompat.PRIORITY_MAX
                    "MEDIUM" -> NotificationCompat.PRIORITY_HIGH
                    else -> NotificationCompat.PRIORITY_DEFAULT
                }
            )
            .setColor(priorityColor)

        if (isSecret) {
            builder
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(
                    NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("🔒 Secret chat")
                        .setContentText("Máš novou tajnou zprávu")
                        .build()
                )

            val remoteInput = RemoteInput.Builder(SecretReplyReceiver.KEY_TEXT_REPLY)
                .setLabel("Odpověď")
                .build()

            val replyIntent = Intent(context, SecretReplyReceiver::class.java).apply {
                action = SecretReplyReceiver.ACTION_REPLY_SECRET
                putExtra(SecretReplyReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            }
            val replyPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 7000,
                replyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

            val action = NotificationCompat.Action.Builder(
                R.mipmap.ic_launcher,
                "Odpovědět",
                replyPendingIntent
            ).addRemoteInput(remoteInput)
                .setAllowGeneratedReplies(true)
                .build()

            builder.addAction(action)
        }

        // obrysy řeší systém, ale pro HIGH dáme full screen a vibrace
        if (priority == "HIGH") {
            builder.setVibrate(longArrayOf(0, 400, 200, 400))
        }

        // SECRET = ticho, jen vibrace
        if (channelId == CHANNEL_SECRET) {
            builder.setSilent(true)
            builder.setVibrate(longArrayOf(0, 120, 80, 120, 80, 120))
        }

        nm.notify(notificationId, builder.build())
    }

    fun cancelAll(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancelAll()
    }
}
