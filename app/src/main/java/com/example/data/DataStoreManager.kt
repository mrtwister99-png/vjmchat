package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

val Context.dataStore by preferencesDataStore(name = "vjm_prefs")

class DataStoreManager(private val context: Context) {
    companion object {
        val ACTIVE_USER_ID = stringPreferencesKey("active_user_id")
        val LAST_LOGOUT_TIME = longPreferencesKey("last_logout_time")
        val LAST_CHAT_READ = longPreferencesKey("last_chat_read")
        val LAST_TASK_READ = longPreferencesKey("last_task_read")
        val LAST_IDEA_READ = longPreferencesKey("last_idea_read")
        val LAST_CALENDAR_READ = longPreferencesKey("last_calendar_read")
        val LAST_NOTIFIED_MSG_ID = longPreferencesKey("last_notified_msg_id")
        val LAST_NOTIFIED_TIME = longPreferencesKey("last_notified_time")
        val LAST_NON_CHAT_DIGEST_TS = longPreferencesKey("last_non_chat_digest_ts")
    }

    suspend fun saveLastNotifiedId(id: Long) {
        context.dataStore.edit { it[LAST_NOTIFIED_MSG_ID] = id }
    }

    suspend fun getLastNotifiedId(): Long {
        return context.dataStore.data.first()[LAST_NOTIFIED_MSG_ID] ?: 0L
    }

    suspend fun saveLastNotifiedTime(time: Long) {
        context.dataStore.edit { it[LAST_NOTIFIED_TIME] = time }
    }

    suspend fun getLastNotifiedTime(): Long {
        return context.dataStore.data.first()[LAST_NOTIFIED_TIME] ?: 0L
    }

    suspend fun saveActiveUserId(userId: String) {
        context.dataStore.edit { it[ACTIVE_USER_ID] = userId }
    }

    suspend fun getActiveUserId(): String? {
        return context.dataStore.data.first()[ACTIVE_USER_ID]
    }

    suspend fun saveLogoutTime(time: Long) {
        context.dataStore.edit { it[LAST_LOGOUT_TIME] = time }
    }

    suspend fun getLastLogoutTime(): Long {
        return context.dataStore.data.first()[LAST_LOGOUT_TIME] ?: 0L
    }

    suspend fun saveLastChatRead(time: Long) {
        context.dataStore.edit { it[LAST_CHAT_READ] = time }
    }

    suspend fun getLastChatRead(): Long {
        return context.dataStore.data.first()[LAST_CHAT_READ] ?: 0L
    }

    suspend fun saveLastTaskRead(time: Long) {
        context.dataStore.edit { it[LAST_TASK_READ] = time }
    }

    suspend fun getLastTaskRead(): Long {
        return context.dataStore.data.first()[LAST_TASK_READ] ?: 0L
    }

    suspend fun saveLastIdeaRead(time: Long) {
        context.dataStore.edit { it[LAST_IDEA_READ] = time }
    }

    suspend fun getLastIdeaRead(): Long {
        return context.dataStore.data.first()[LAST_IDEA_READ] ?: 0L
    }

    suspend fun saveLastCalendarRead(time: Long) {
        context.dataStore.edit { it[LAST_CALENDAR_READ] = time }
    }

    suspend fun getLastCalendarRead(): Long {
        return context.dataStore.data.first()[LAST_CALENDAR_READ] ?: 0L
    }
}