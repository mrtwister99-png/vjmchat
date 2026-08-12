package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.CalendarScreen
import com.example.ui.ChatScreen
import com.example.ui.HomeScreen
import com.example.ui.IdeaScreen
import com.example.ui.PinLoginScreen
import com.example.ui.MainViewModel
import com.example.ui.NotificationBanner
import com.example.ui.ProfileSettingsDialog
import com.example.ui.PersistentBottomStrip
import com.example.ui.SecretChatScreen
import com.example.ui.TaskNoteScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.notification.SystemNotificationHelper
import com.example.NotificationScheduler
import com.example.widget.VJMWidgetProvider
import android.Manifest
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            NotificationScheduler.schedulePeriodicWork(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SystemNotificationHelper.createChannels(this)
        SystemNotificationHelper.cancelAll(this)
        VJMWidgetProvider.updateAllWidgets(this)
        handleWidgetIntent(intent)
        
        // Zeptat se na POST_NOTIFICATIONS na Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                NotificationScheduler.schedulePeriodicWork(this)
            }
        } else {
            NotificationScheduler.schedulePeriodicWork(this)
        }

        setContent {
            MyApplicationTheme {
                TeamAppContent(viewModel = viewModel, activity = this)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        SystemNotificationHelper.cancelAll(this)
        viewModel.setAppInForeground(true)
        viewModel.setCurrentUserOnline(true)
        VJMWidgetProvider.updateAllWidgets(this)
    }

    override fun onStop() {
        super.onStop()
        viewModel.setAppInForeground(false)
        viewModel.setCurrentUserOnline(false)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetIntent(intent)
    }

    private fun handleWidgetIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.getBooleanExtra("widget_open_chat", false)) {
            viewModel.setPendingWidgetChat("open_from_widget")
            intent.removeExtra("widget_open_chat")
        }
        if (intent.getBooleanExtra("widget_open_home", false)) {
            intent.removeExtra("widget_open_home")
        }
        if (intent.getBooleanExtra("widget_action", false) == true) {
            val type = intent.getStringExtra("widget_type") ?: "CHAT"
            val text = intent.getStringExtra("widget_text") ?: ""
            if (text.isBlank()) return

            when (type) {
                "CHAT" -> {
                    viewModel.sendChatMessage(text)
                    viewModel.setPendingWidgetChat(text)
                }
                "ÚKOLY" -> {
                    viewModel.addTaskOrNote("TASK", text, "", "#3B82F6", false, false, "#FFFFFF", 16, null, "NONE")
                }
                "POZNÁMKY" -> {
                    viewModel.addTaskOrNote("NOTE", text, "", "#3B82F6", false, false, "#FFFFFF", 16, null, "NONE")
                }
                "NÁPADY" -> {
                    viewModel.addIdea(text, "", 0, "NONE")
                }
                "KALENDÁŘ" -> {
                    val now = System.currentTimeMillis()
                    viewModel.addCalendarEvent(text, "", "#3B82F6", now, now + 3600000, false, "", false, "Žádné", 15, "NONE")
                }
            }
            intent.removeExtra("widget_action")
        }
    }
}

@Composable
fun TeamAppContent(viewModel: MainViewModel, activity: MainActivity) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    fun navigateHome() {
        navController.navigate("home") {
            popUpTo("home") {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    androidx.compose.runtime.LaunchedEffect(currentRoute) {
        viewModel.setCurrentScreenRoute(currentRoute)
    }

    // když přijde widget intent, naviguj - i když appka už běží
    val pendingTask by viewModel.pendingWidgetTask.collectAsStateWithLifecycle()
    val pendingIdea by viewModel.pendingWidgetIdea.collectAsStateWithLifecycle()
    val pendingCal by viewModel.pendingWidgetCalendar.collectAsStateWithLifecycle()
    val pendingChat by viewModel.pendingWidgetChat.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(pendingTask, pendingIdea, pendingCal, pendingChat) {
        when {
            pendingChat != null -> {
                navController.navigate("chat") {
                    launchSingleTop = true
                }
                viewModel.clearPendingWidgetChat()
            }
            pendingTask != null -> navController.navigate("tasks")
            pendingIdea != null -> navController.navigate("ideas")
            pendingCal != null -> navController.navigate("calendar")
        }
    }

    androidx.compose.runtime.LaunchedEffect(activity.intent) {
        val intent = activity.intent
        if (intent?.getBooleanExtra("widget_open_home", false) == true) {
            navController.navigate("home") {
                popUpTo("home") { inclusive = false }
                launchSingleTop = true
            }
            activity.intent?.removeExtra("widget_open_home")
        }
        if (intent?.getBooleanExtra("widget_action", false) == true) {
            val type = intent.getStringExtra("widget_type") ?: "CHAT"
            when (type) {
                "CHAT" -> navController.navigate("chat")
                "ÚKOLY", "POZNÁMKY" -> navController.navigate("tasks")
                "NÁPADY" -> navController.navigate("ideas")
                "KALENDÁŘ" -> navController.navigate("calendar")
            }
            activity.intent?.removeExtra("widget_action")
        }
    }

    val users by viewModel.users.collectAsStateWithLifecycle()
    val activeUser by viewModel.activeUser.collectAsStateWithLifecycle()
    val activities by viewModel.activities.collectAsStateWithLifecycle()
    val tasksAndNotes by viewModel.tasksAndNotes.collectAsStateWithLifecycle()
    val ideas by viewModel.ideas.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val calendarEvents by viewModel.calendarEvents.collectAsStateWithLifecycle()
    val notification by viewModel.notification.collectAsStateWithLifecycle()
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val showBottomStrip = isLoggedIn && activeUser != null
    val bottomStripHeight = if (showBottomStrip) 70.dp else 0.dp

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0B0F19))) {
        NavHost(
            navController = navController,
            startDestination = if (isLoggedIn && activeUser != null) "home" else "login"
            ,
            enterTransition = { fadeIn(animationSpec = tween(260)) },
            exitTransition = { fadeOut(animationSpec = tween(260)) },
            popEnterTransition = { fadeIn(animationSpec = tween(260)) },
            popExitTransition = { fadeOut(animationSpec = tween(260)) },
            modifier = Modifier.fillMaxSize().padding(bottom = bottomStripHeight)
        ) {
            composable("login") {
                PinLoginScreen(
                    users = users,
                    onLoginSuccess = { user ->
                        viewModel.loginUser(user.id)
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    users = users,
                    activeUser = activeUser,
                    activities = activities,
                    chatMessages = chatMessages,
                    onNavigateToTasks = { navController.navigate("tasks") },
                    onNavigateToIdeas = { navController.navigate("ideas") },
                    onNavigateToChat = { navController.navigate("chat") },
                    onNavigateToCalendar = { navController.navigate("calendar") },
                    onOpenProfileSettings = { navController.navigate("settings") }
                )
            }

            composable("tasks") {
                TaskNoteScreen(
                    viewModel = viewModel,
                    tasksAndNotes = tasksAndNotes,
                    activeUser = activeUser,
                    onBack = { navigateHome() }
                )
            }

            composable("ideas") {
                IdeaScreen(
                    viewModel = viewModel,
                    ideas = ideas,
                    activeUser = activeUser,
                    onBack = { navigateHome() }
                )
            }

            composable("chat") {
                ChatScreen(
                    viewModel = viewModel,
                    chatMessages = chatMessages,
                    activeUser = activeUser,
                    users = users,
                    onBack = { navigateHome() }
                )
            }

            composable("calendar") {
                CalendarScreen(
                    viewModel = viewModel,
                    calendarEvents = calendarEvents,
                    activeUser = activeUser,
                    onBack = { navigateHome() }
                )
            }

            composable("secret_chat") {
                val blocked = activeUser?.id == "tata" || activeUser?.defaultName?.contains("Michal", true) == true || activeUser?.nickname?.contains("Michal", true) == true
                androidx.compose.runtime.LaunchedEffect(blocked) {
                    if (blocked) {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    } else {
                        viewModel.onSecretChatOpened()
                    }
                }
                if (!blocked) {
                    SecretChatScreen(
                        viewModel = viewModel,
                        activeUser = activeUser,
                        users = users,
                        onBack = { navigateHome() }
                    )
                }
            }

            composable("settings") {
                ProfileSettingsDialog(
                    user = activeUser,
                    allUsers = users,
                    onSave = { nickname, avatar, border, email, pin ->
                        viewModel.updateProfile(nickname, avatar, border, email, pin, null, null)
                    },
                    onSaveBubbleStyleForUser = { targetId, bubbleColor, bubbleShape ->
                        viewModel.updateUserBubbleStyle(targetId, bubbleColor, bubbleShape)
                    },
                    onResetPin = { targetId, newPin ->
                        viewModel.resetUserPin(targetId, newPin)
                    },
                    onResetAllData = {
                        viewModel.resetAllData()
                    },
                    onOpenSecretChat = {
                        navController.navigate("secret_chat")
                    },
                    onDismiss = { navigateHome() }
                )
            }
        }

        // Notification Overlay Banner - s ikonkou autora + priority barva + zvuky
        NotificationBanner(
            notification = notification,
            users = users,
            onDismiss = { viewModel.dismissNotification() },
            onNavigateToCategory = { category ->
                val isMichal = activeUser?.id == "tata" || activeUser?.defaultName?.contains("Michal", true) == true || activeUser?.nickname?.contains("Michal", true) == true
                when (category.uppercase()) {
                    "CHAT" -> navController.navigate("chat")
                    "SECRET", "SECRET_CHAT" -> if (!isMichal) navController.navigate("secret_chat") else navController.navigate("home")
                    "TASK", "NOTE" -> navController.navigate("tasks")
                    "IDEA" -> navController.navigate("ideas")
                    "CALENDAR" -> navController.navigate("calendar")
                    else -> navController.navigate("home")
                }
            },
            onQuickReply = { replyMsg ->
                val currentNotif = notification
                val isMichal = activeUser?.id == "tata" || activeUser?.defaultName?.contains("Michal", true) == true || activeUser?.nickname?.contains("Michal", true) == true
                if ((currentNotif?.category == "SECRET" || currentNotif?.category == "SECRET_CHAT") && !isMichal) {
                    viewModel.sendSecretChatMessage(replyMsg)
                } else {
                    viewModel.sendChatMessage(replyMsg)
                }
            }
        )

        if (showBottomStrip) {
            PersistentBottomStrip(
                users = users,
                activeUser = activeUser,
                activities = activities,
                onOpenActivity = { activity ->
                    viewModel.markActivityRead(activity.timestamp)
                    when (activity.category.uppercase()) {
                        "CHAT" -> navController.navigate("chat") { launchSingleTop = true }
                        "TASK", "NOTE" -> navController.navigate("tasks") { launchSingleTop = true }
                        "IDEA" -> navController.navigate("ideas") { launchSingleTop = true }
                        "CALENDAR" -> navController.navigate("calendar") { launchSingleTop = true }
                        else -> navController.navigate("home") { launchSingleTop = true }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }

    }
}
