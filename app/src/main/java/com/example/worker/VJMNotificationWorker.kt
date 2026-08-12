package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.TeamDatabase
import com.example.data.DataStoreManager
import com.example.data.dataStore
import com.example.data.remote.ApiConfig
import com.example.data.remote.ApiService
import com.example.notification.SystemNotificationHelper
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.first
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.example.data.SoundManager
import androidx.datastore.preferences.core.edit

class VJMNotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        try {
            val ctx = applicationContext
            SystemNotificationHelper.createChannels(ctx)
            val retrofit = Retrofit.Builder().baseUrl(ApiConfig.BASE_URL).addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build())).build()
            val api = retrofit.create(ApiService::class.java)

            val prefs = try { ctx.dataStore.data.first() } catch (_: Exception) { null }
            val activeIdRaw = prefs?.get(DataStoreManager.ACTIVE_USER_ID)
            if (activeIdRaw.isNullOrBlank()) {
                return Result.success()
            }
            val activeId = activeIdRaw
            val isMichal = activeId == "tata" || activeId.contains("tata", true)
            val db = TeamDatabase.getDatabase(ctx)
            val dao = db.teamDao()

            // FIX: notifikace chodi vzdy, i kdyz je app v popredi (in-app banner + system) - pouze autor nedostane
            // isForeground check odstranen dle pozadavku TOM

            val lastNotifiedId = prefs?.get(DataStoreManager.LAST_NOTIFIED_MSG_ID) ?: 0L
            val now = System.currentTimeMillis()

            try {
                val remoteMessages = api.getMessages()
                val newMessages = remoteMessages.filter { it.timestamp > lastNotifiedId }.sortedBy { it.timestamp }
                if (newMessages.isNotEmpty()) {
                    val latest = newMessages.last()
                    // FIX: porovnat i nickname/defaultName, ne jen id - jinak autor dostane vlastni notifikaci
                    val activeUserForCheck = try { db.teamDao().getUserById(activeId) } catch (_: Exception) { null }
                    val isAuthor = latest.sender == activeId || latest.sender == activeUserForCheck?.nickname || latest.sender == activeUserForCheck?.defaultName
                    if (!isAuthor) {
                        SystemNotificationHelper.showNotification(ctx, SystemNotificationHelper.CHANNEL_CHAT, "Nova zprava v chatu", "${latest.sender}: ${latest.text.take(80)}", 1001, "CHAT", "MEDIUM")
                        ctx.dataStore.edit {
                            it[DataStoreManager.LAST_NOTIFIED_MSG_ID] = latest.timestamp
                            it[DataStoreManager.LAST_NOTIFIED_TIME] = now
                        }
                    } else {
                        ctx.dataStore.edit { it[DataStoreManager.LAST_NOTIFIED_MSG_ID] = latest.timestamp }
                    }
                }
            } catch (_: Exception) {}

            if (!isMichal) {
                try {
                    val activeUser = try { dao.getUserById(activeId) } catch (_: Exception) { null }
                    val lastRead = activeUser?.lastChatReadTimestamp ?: 0L
                    val secretUnread = try { dao.getAllSecretChatMessagesList().count { it.timestamp > lastRead && it.senderId != activeId } } catch (_: Exception) { 0 }
                    if (secretUnread > 0) {
                        SystemNotificationHelper.showNotification(ctx, SystemNotificationHelper.CHANNEL_SECRET, "🔒 Secret chat", if (secretUnread == 1) "Nova tajna zprava" else "$secretUnread novych tajnych zprav", 1005, "SECRET", "MEDIUM")
                        try { SoundManager.vibrateSecret(ctx) } catch (_: Exception) {}
                    }
                } catch (_: Exception) {}
            }

            // DIGEST 1x za beh workeru: mene caste veci (ukoly/poznamky/napady/kalendar)
            try {
                val lastDigestTs = prefs?.get(DataStoreManager.LAST_NON_CHAT_DIGEST_TS) ?: 0L
                if (lastDigestTs == 0L) {
                    ctx.dataStore.edit { it[DataStoreManager.LAST_NON_CHAT_DIGEST_TS] = now }
                } else {
                    val changed = dao.getAllActivitiesList().filter {
                        it.timestamp > lastDigestTs &&
                            it.authorId != activeId &&
                            (it.category == "TASK" || it.category == "NOTE" || it.category == "IDEA" || it.category == "CALENDAR")
                    }
                    if (changed.isNotEmpty()) {
                        val taskCount = changed.count { it.category == "TASK" || it.category == "NOTE" }
                        val ideaCount = changed.count { it.category == "IDEA" }
                        val calendarCount = changed.count { it.category == "CALENDAR" }

                        val parts = mutableListOf<String>()
                        if (taskCount > 0) parts.add("${taskCount}x ukoly/poznamky")
                        if (ideaCount > 0) parts.add("${ideaCount}x napady")
                        if (calendarCount > 0) parts.add("${calendarCount}x kalendar")

                        if (parts.isNotEmpty()) {
                            SystemNotificationHelper.showNotification(
                                ctx,
                                SystemNotificationHelper.CHANNEL_COMMENT,
                                "Souhrn zmen",
                                parts.joinToString(" • "),
                                1011,
                                "COMMENT",
                                "LOW"
                            )
                        }

                        val newestTs = changed.maxOfOrNull { it.timestamp } ?: now
                        ctx.dataStore.edit { it[DataStoreManager.LAST_NON_CHAT_DIGEST_TS] = newestTs }
                    }
                }
            } catch (_: Exception) {}

            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }
}