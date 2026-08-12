package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.example.data.DataStoreManager
import com.example.data.TeamDatabase
import com.example.data.dataStore
import com.example.data.remote.ApiConfig
import com.example.data.remote.ApiService
import com.example.data.remote.VjmSecretMessageDto
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class SecretReplyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REPLY_SECRET) return

        val replyText = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(KEY_TEXT_REPLY)
            ?.toString()
            ?.trim()
            .orEmpty()

        if (replyText.isBlank()) return

        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = appContext.dataStore.data.first()
                val activeId = prefs[DataStoreManager.ACTIVE_USER_ID] ?: return@launch
                val dao = TeamDatabase.getDatabase(appContext).teamDao()
                val activeUser = dao.getUserById(activeId) ?: return@launch
                val isMichal = activeUser.id == "tata" || activeUser.defaultName.contains("Michal", true) || activeUser.nickname.contains("Michal", true)
                if (isMichal) return@launch

                dao.insertChatMessage(
                    com.example.data.ChatMessageEntity(
                        senderId = activeUser.id,
                        senderName = activeUser.nickname,
                        recipientId = "SECRET",
                        content = replyText,
                        priority = "MEDIUM"
                    )
                )

                val retrofit = Retrofit.Builder()
                    .baseUrl(ApiConfig.BASE_URL)
                    .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build()))
                    .build()
                val api = retrofit.create(ApiService::class.java)
                api.sendSecretMessage(
                    VjmSecretMessageDto(
                        senderId = activeUser.id,
                        senderName = activeUser.nickname,
                        text = replyText,
                        priority = "MEDIUM"
                    )
                )
            } catch (_: Exception) {
            }
        }

        val id = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 1005)
        try { NotificationManagerCompat.from(appContext).cancel(id) } catch (_: Exception) {}
    }

    companion object {
        const val ACTION_REPLY_SECRET = "com.example.notification.ACTION_REPLY_SECRET"
        const val KEY_TEXT_REPLY = "key_text_reply_secret"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
