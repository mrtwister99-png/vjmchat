package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.example.R
import com.example.MainActivity
import com.example.data.TeamDatabase
import com.example.data.DataStoreManager
import com.example.data.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class VJMWidgetProvider : AppWidgetProvider() {
    companion object {
        const val ACTION_OPEN_CHAT = "com.example.widget.OPEN_CHAT"
        const val ACTION_OPEN_HOME = "com.example.widget.OPEN_HOME"

        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val component = android.content.ComponentName(context, VJMWidgetProvider::class.java)
                val ids = appWidgetManager.getAppWidgetIds(component)
                if (ids.isNotEmpty()) {
                    val intent = Intent(context, VJMWidgetProvider::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                    }
                    context.sendBroadcast(intent)
                }
            } catch (_: Exception) {}
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            updateWidget(context, appWidgetManager, id)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        try {
            when (intent.action) {
                ACTION_OPEN_CHAT -> {
                    val launch = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("widget_open_chat", true)
                    }
                    context.startActivity(launch)
                }
                ACTION_OPEN_HOME -> {
                    val launch = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("widget_open_home", true)
                    }
                    context.startActivity(launch)
                }
            }
        } catch (_: Exception) {}
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val views = RemoteViews(context.packageName, R.layout.vjm_widget_layout)
                views.setTextViewText(R.id.widget_header_left_title, "Poslední aktivity")
                views.setTextViewText(R.id.widget_header_right_title, "VJM Chat")
                views.setTextViewText(R.id.widget_open_chat_btn, "Otevřít Chat")
                views.setTextViewText(R.id.widget_new_messages_count, "0")
                views.setTextViewText(R.id.widget_new_messages_label, "žádné nové")
                views.setTextViewText(R.id.widget_activity_1_title, "Načítám...")
                views.setViewVisibility(R.id.widget_activity_2, android.view.View.GONE)
                views.setViewVisibility(R.id.widget_activity_3, android.view.View.GONE)

                try {
                    val db = TeamDatabase.getDatabase(context)
                    val dao = db.teamDao()

                    val activities = try {
                        withTimeoutOrNull(3000) {
                            dao.getAllActivitiesList()
                              .filter { it.category!= "SECRET" && it.category!= "SECRET_CHAT" &&!it.title.contains("tajný", true) &&!it.title.contains("secret", true) }
                              .sortedByDescending { it.timestamp }
                              .take(3)
                        }?: emptyList()
                    } catch (_: Exception) { emptyList() }

                    val activeId = try {
                        withTimeoutOrNull(1500) {
                            val prefs = context.dataStore.data.first()
                            prefs[DataStoreManager.ACTIVE_USER_ID]
                        }
                    } catch (_: Exception) { null }

                    val activeUser = try {
                        if (activeId!= null) dao.getUserById(activeId) else null
                    } catch (_: Exception) { null }

                    val lastRead = activeUser?.lastChatReadTimestamp?: 0L
                    val unread = try {
                        withTimeoutOrNull(2000) {
                            dao.getAllChatMessagesList().count {
                                it.timestamp > lastRead && it.senderId!= activeId && it.senderName!= activeUser?.nickname
                            }
                        }?: 0
                    } catch (_: Exception) { 0 }

                    val activityViews = listOf(
                        R.id.widget_activity_1 to R.id.widget_activity_1_title,
                        R.id.widget_activity_2 to R.id.widget_activity_2_title,
                        R.id.widget_activity_3 to R.id.widget_activity_3_title
                    )
                    for ((idx, pair) in activityViews.withIndex()) {
                        val (cont, title) = pair
                        try {
                            if (idx < activities.size) {
                                val act = activities[idx]
                                views.setTextViewText(title, "${act.title.take(26)} • ${act.authorName}")
                                views.setViewVisibility(cont, android.view.View.VISIBLE)
                            } else {
                                if (idx == 0) {
                                    views.setTextViewText(title, if (activities.isEmpty()) "Žádné aktivity" else "")
                                    views.setViewVisibility(cont, android.view.View.VISIBLE)
                                } else {
                                    views.setViewVisibility(cont, android.view.View.GONE)
                                }
                            }
                        } catch (_: Exception) {}
                    }

                    views.setTextViewText(R.id.widget_new_messages_count, "$unread")
                    val label = when (unread) {
                        0 -> "žádné nové"
                        1 -> "nová zpráva"
                        2, 3, 4 -> "$unread nové zprávy"
                        else -> "$unread nových zpráv"
                    }
                    views.setTextViewText(R.id.widget_new_messages_label, label)

                } catch (_: Exception) {}

                try {
                    val openChat = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("widget_open_chat", true)
                        data = Uri.parse("vjm://chat/$appWidgetId/${System.currentTimeMillis()}")
                    }
                    val piChat = PendingIntent.getActivity(
                        context, appWidgetId * 10 + 1, openChat,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_open_chat_container, piChat)
                    views.setOnClickPendingIntent(R.id.widget_new_messages_container, piChat)
                    views.setOnClickPendingIntent(R.id.widget_open_chat_btn, piChat)
                } catch (_: Exception) {}

                try {
                    val openHome = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("widget_open_home", true)
                        data = Uri.parse("vjm://home/$appWidgetId/${System.currentTimeMillis()}")
                    }
                    val piHome = PendingIntent.getActivity(
                        context, appWidgetId * 10 + 2, openHome,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_header_left_title, piHome)
                    views.setOnClickPendingIntent(R.id.widget_activity_1, piHome)
                    views.setOnClickPendingIntent(R.id.widget_activity_2, piHome)
                    views.setOnClickPendingIntent(R.id.widget_activity_3, piHome)
                } catch (_: Exception) {}

                try {
                    val openChatHeader = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("widget_open_chat", true)
                        data = Uri.parse("vjm://chat_header/$appWidgetId/${System.currentTimeMillis()}")
                    }
                    val piChatHeader = PendingIntent.getActivity(
                        context, appWidgetId * 10 + 3, openChatHeader,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_header_right_title, piChatHeader)
                } catch (_: Exception) {}

                appWidgetManager.updateAppWidget(appWidgetId, views)

            } catch (e: Exception) {
                try {
                    val fallback = RemoteViews(context.packageName, R.layout.vjm_widget_layout)
                    fallback.setTextViewText(R.id.widget_header_left_title, "Poslední aktivity")
                    fallback.setTextViewText(R.id.widget_header_right_title, "VJM Chat")
                    fallback.setTextViewText(R.id.widget_activity_1_title, "VJM Chat - otevři aplikaci")
                    fallback.setTextViewText(R.id.widget_new_messages_count, "0")
                    fallback.setTextViewText(R.id.widget_new_messages_label, "žádné nové")
                    fallback.setTextViewText(R.id.widget_open_chat_btn, "Otevřít Chat")
                    fallback.setViewVisibility(R.id.widget_activity_2, android.view.View.GONE)
                    fallback.setViewVisibility(R.id.widget_activity_3, android.view.View.GONE)

                    val openChat = Intent(context, VJMWidgetProvider::class.java).apply {
                        action = ACTION_OPEN_CHAT
                    }
                    val piChat = PendingIntent.getBroadcast(
                        context, appWidgetId * 10 + 1, openChat,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    fallback.setOnClickPendingIntent(R.id.widget_open_chat_container, piChat)
                    fallback.setOnClickPendingIntent(R.id.widget_open_chat_btn, piChat)
                    appWidgetManager.updateAppWidget(appWidgetId, fallback)
                } catch (_: Exception) {}
            } finally {
                try { pendingResult.finish() } catch (_: Exception) {}
            }
        }
    }
}