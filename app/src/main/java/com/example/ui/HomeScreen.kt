package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ActivityEntity
import com.example.data.ChatMessageEntity
import com.example.data.UserEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import kotlinx.coroutines.delay
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    users: List<UserEntity>,
    activeUser: UserEntity?,
    activities: List<ActivityEntity>,
    chatMessages: List<ChatMessageEntity>,
    onNavigateToTasks: () -> Unit,
    onNavigateToIdeas: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onOpenProfileSettings: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showHelpGuide by remember { mutableStateOf(false) }

   
    val context = androidx.compose.ui.platform.LocalContext.current
    var prevOnlineIds by remember { mutableStateOf(setOf<String>()) }
    val activeUserId by viewModel.activeUserId.collectAsState()
    val currentOnlineIds = users.filter { it.isOnline && it.id != activeUserId }.map { it.id }.toSet()
    val onlineCount = currentOnlineIds.size.coerceIn(0, 2)
    androidx.compose.runtime.LaunchedEffect(currentOnlineIds) {
        val newlyOnline = currentOnlineIds - prevOnlineIds
        if (newlyOnline.isNotEmpty() && prevOnlineIds.isNotEmpty()) {
            com.example.data.SoundManager.play(context, com.example.R.raw.plusonline)
        }
        prevOnlineIds = currentOnlineIds
    }
    val unreadTaskCount by viewModel.unreadTaskCount.collectAsState()
    val unreadIdeaCount by viewModel.unreadIdeaCount.collectAsState()
    val unreadCalendarCount by viewModel.unreadCalendarCount.collectAsState()
    val praiseCount = remember(chatMessages, activeUser?.id, activeUser?.nickname) {
        val meId = activeUser?.id
        val meNick = activeUser?.nickname
        if (meId.isNullOrBlank() && meNick.isNullOrBlank()) {
            0
        } else {
            chatMessages.sumOf { msg ->
                val isMyMessage = msg.senderId == meId || (!meNick.isNullOrBlank() && msg.senderName == meNick)
                if (!isMyMessage || msg.reactionEmoji.isNullOrBlank()) {
                    0
                } else {
                    try {
                        val obj = JSONObject(msg.reactionEmoji!!)
                        obj.keys().asSequence().count { it.startsWith("__praise_") }
                    } catch (_: Exception) {
                        0
                    }
                }
            }
        }
    }

    val displayActivities = activities.filter { act ->
        val isSecret = act.category == "SECRET_CHAT" || act.title.contains("tajný", true) || act.authorName.lowercase() == "secret"
        if (isSecret) {
            val isMichal = activeUser?.nickname?.contains("Michal", true) == true
            !isMichal
        } else true
    }.sortedByDescending { it.timestamp }.take(20)

    var activityNudge by remember { mutableStateOf(false) }
    val activityShift by animateFloatAsState(
        targetValue = if (activityNudge) -3f else 0f,
        animationSpec = tween(180),
        label = "activityShift"
    )
    LaunchedEffect(displayActivities.firstOrNull()?.id, displayActivities.size) {
        if (displayActivities.isNotEmpty()) {
            activityNudge = true
            delay(90)
            activityNudge = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    if (activeUser != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Image(painter = painterResource(id = R.drawable.vjmchat1), contentDescription = "VJM chat", modifier = Modifier.height(36.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF1E293B)).border(2.dp, try { Color(android.graphics.Color.parseColor(activeUser.borderHexColor)) } catch (e: Exception) { Color.Blue }, CircleShape)) {
                                Text(activeUser.avatarEmoji, fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            val displayName = activeUser.nickname.takeIf { it.lowercase() != "secret" } ?: users.find { it.id == activeUser.id }?.defaultName ?: "Tom"
                            Text(text = displayName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF1E293B))
                                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.gj),
                                    contentDescription = "Pochvaly",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("x$praiseCount", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E293B))
                                    .border(1.dp, Color(0xFF334155), CircleShape)
                                    .clickable { showHelpGuide = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("?", color = Color(0xFFFBBF24), fontWeight = FontWeight.Black, fontSize = 18.sp)
                            }
                        }
                    }

                    if (false) { DropdownMenu(
                        expanded = false,
                        onDismissRequest = {},
                        modifier = Modifier.background(Color(0xFF1E293B))
                    ) {
                        Text(
                            text = "Přepnout člena:",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                        users.forEach { u ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(u.avatarEmoji, fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = u.nickname,
                                            color = if (u.id == activeUser?.id) Color(0xFF60A5FA) else Color.White,
                                            fontWeight = if (u.id == activeUser?.id) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.switchActiveUser(u.id)
                                }
                            )
                        }
                    } }
                },
                actions = {},
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0B0F19)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // TOP - Poslední aktivity jako samostatné záznamy
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.72f)
                    .padding(bottom = 8.dp)
                    .graphicsLayer { translationY = activityShift },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(12.dp).fillMaxSize()) {
                    Text("Poslední aktivity", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White, modifier = Modifier.padding(bottom = 12.dp))
                    if (displayActivities.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Zatím žádné aktivity", color = Color(0xFF64748B), fontSize = 13.sp) }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(displayActivities, key = { it.id }) { act ->
                                var visible by remember(act.id) { mutableStateOf(false) }
                                LaunchedEffect(act.id) { visible = true }
                                AnimatedVisibility(
                                    visible = visible,
                                    enter = slideInHorizontally(initialOffsetX = { -it / 2 }) + fadeIn()
                                ) {
                                    ActivityItemRow(
                                        activity = act,
                                        activeUser = activeUser,
                                        users = users,
                                        onClickItem = {
                                            when (act.category) {
                                                "TASK", "NOTE" -> onNavigateToTasks()
                                                "IDEA" -> onNavigateToIdeas()
                                                "CHAT" -> onNavigateToChat()
                                                "CALENDAR" -> onNavigateToCalendar()
                                            }
                                        },
                                        onLongPress = {}
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // BOTTOM HALF (6 Buttons Grid: 3 Left, 3 Right)
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.76f)
                    .padding(top = 4.dp)
            ) {
                // LEFT COLUMN (Úkoly, Nápady, Chat)
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        var taskAnim by remember { mutableStateOf(false) }
                        var lastTaskCount by remember { mutableStateOf(unreadTaskCount) }
                        val taskScale by animateFloatAsState(targetValue = if (taskAnim) 1.35f else 1f, animationSpec = tween(250), label = "taskPulse")
                        LaunchedEffect(unreadTaskCount) {
                            if (unreadTaskCount > lastTaskCount && unreadTaskCount > 0) {
                                taskAnim = true
                                kotlinx.coroutines.delay(250)
                                taskAnim = false
                            }
                            lastTaskCount = unreadTaskCount
                        }
                        NavTileButtonImg(title = "Úkoly & Poznámky", drawableRes = R.drawable.task, color = Color(0xFFF97316), modifier = Modifier.fillMaxSize(), onClick = { viewModel.onTasksOpened(); onNavigateToTasks() })
                        if (unreadTaskCount > 0) {
                            Box(modifier = Modifier.padding(6.dp).align(Alignment.TopStart).scale(taskScale).size(26.dp).clip(CircleShape).background(if (unreadTaskCount > 4) Color(0xFFEF4444) else Color(0xFFEAB308)).border(2.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
                                Text("$unreadTaskCount", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        var ideaAnim by remember { mutableStateOf(false) }
                        var lastIdeaCount by remember { mutableStateOf(unreadIdeaCount) }
                        val ideaScale by animateFloatAsState(targetValue = if (ideaAnim) 1.35f else 1f, animationSpec = tween(250), label = "ideaPulse")
                        LaunchedEffect(unreadIdeaCount) {
                            if (unreadIdeaCount > lastIdeaCount && unreadIdeaCount > 0) {
                                ideaAnim = true
                                kotlinx.coroutines.delay(250)
                                ideaAnim = false
                            }
                            lastIdeaCount = unreadIdeaCount
                        }
                        NavTileButtonImg(title = "Nápady", drawableRes = R.drawable.idea, color = Color(0xFFFFEA03), modifier = Modifier.fillMaxSize(), onClick = { viewModel.onIdeasOpened(); onNavigateToIdeas() })
                        if (unreadIdeaCount > 0) {
                            Box(modifier = Modifier.padding(6.dp).align(Alignment.TopStart).scale(ideaScale).size(26.dp).clip(CircleShape).background(if (unreadIdeaCount > 4) Color(0xFFEF4444) else Color(0xFFEAB308)).border(2.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
                                Text("$unreadIdeaCount", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        val unreadCount by viewModel.unreadChatCount.collectAsState()
                        var shouldAnimate by remember { mutableStateOf(false) }
                        var lastChatCount by remember { mutableStateOf(unreadCount) }
                        val scale by animateFloatAsState(targetValue = if (shouldAnimate) 1.35f else 1f, animationSpec = tween(250), label = "pulse")
                        LaunchedEffect(unreadCount) {
                            if (unreadCount > lastChatCount) {
                                shouldAnimate = true
                                kotlinx.coroutines.delay(250)
                                shouldAnimate = false
                            }
                            lastChatCount = unreadCount
                        }
                        NavTileButtonImg(title = "Chat", drawableRes = R.drawable.chat, color = Color(0xFF3B82F6), modifier = Modifier.fillMaxSize(), onClick = { viewModel.onChatOpened(); onNavigateToChat() })
                        val badgeColor = when { unreadCount == 0 -> Color(0xFF3B82F6); unreadCount in 1..4 -> Color(0xFFEAB308); else -> Color(0xFFEF4444) }
                        Box(modifier = Modifier.padding(6.dp).align(Alignment.TopStart).scale(scale).size(26.dp).clip(CircleShape).background(badgeColor).border(2.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
                            Text("$unreadCount", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        val onlineBg = if (onlineCount == 0) Color(0xFF334155) else Color(0xFF22C55E)
                        Box(modifier = Modifier.padding(6.dp).align(Alignment.TopEnd).size(26.dp).clip(CircleShape).background(onlineBg).border(2.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
                            Text("$onlineCount", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Box(modifier = Modifier.weight(1f)) {
                        var calAnim by remember { mutableStateOf(false) }
                        var lastCalCount by remember { mutableStateOf(unreadCalendarCount) }
                        val calScale by animateFloatAsState(targetValue = if (calAnim) 1.35f else 1f, animationSpec = tween(250), label = "calPulse")
                        LaunchedEffect(unreadCalendarCount) {
                            if (unreadCalendarCount > lastCalCount && unreadCalendarCount > 0) {
                                calAnim = true
                                kotlinx.coroutines.delay(250)
                                calAnim = false
                            }
                            lastCalCount = unreadCalendarCount
                        }
                        NavTileButtonImg(title = "Kalendář", drawableRes = R.drawable.kalendar, color = Color(0xFF00C43B), modifier = Modifier.fillMaxSize(), onClick = { viewModel.onCalendarOpened(); onNavigateToCalendar() })
                        if (unreadCalendarCount > 0) {
                            Box(modifier = Modifier.padding(6.dp).align(Alignment.TopStart).scale(calScale).size(26.dp).clip(CircleShape).background(if (unreadCalendarCount > 4) Color(0xFFEF4444) else Color(0xFFEAB308)).border(2.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
                                Text("$unreadCalendarCount", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    NavTileButton(title = "Dále...", icon = Icons.AutoMirrored.Filled.HelpOutline, color = Color(0xFF64748B), modifier = Modifier.weight(1f), enabled = false, subtitle = "ve výstavbě", onClick = {})
                    NavTileButtonImg(title = "Nastavení", drawableRes = R.drawable.nastaveni, color = Color(0xFF94A3B8), modifier = Modifier.weight(1f), onClick = { onOpenProfileSettings() })
                }
            }
        }
    }

    if (showHelpGuide) {
        AlertDialog(
            onDismissRequest = { showHelpGuide = false },
            containerColor = Color(0xFF0F172A),
            title = {
                Text("Co tu jde dělat", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Chat, úkoly, nápady a kalendář jsou živé a sdílené pro všechny.", color = Color(0xFFE2E8F0), fontSize = 13.sp)
                    Text("• Kuličky dole ukazují lidi a nové aktivity. Kliknutí tě přesune na danou položku.", color = Color(0xFFE2E8F0), fontSize = 13.sp)
                    Text("• V chatu můžeš posílat soubory, fotky, reakce i rychlé odpovědi.", color = Color(0xFFE2E8F0), fontSize = 13.sp)
                    Text("• V nápadech a úkolech jde přidávat komentáře, priority a stavy.", color = Color(0xFFE2E8F0), fontSize = 13.sp)
                    Text("• Kalendář má události s časem, komentáři a animací výběru dne.", color = Color(0xFFE2E8F0), fontSize = 13.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpGuide = false }) {
                    Text("Zavřít", color = Color(0xFF60A5FA), fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ActivityItemRow(
    activity: ActivityEntity,
    activeUser: UserEntity?,
    users: List<UserEntity>,
    onClickItem: () -> Unit,
    onLongPress: () -> Unit
) {
    val isComment = activity.title.contains("Komentář", true) || activity.title.contains("Nový komentář", true)
    val categoryBg = when {
        isComment -> Color(0xFF5500FF)
        activity.category == "TASK" -> Color(0xFFFF6F00)
        activity.category == "NOTE" -> Color(0xFFFFCC00)
        activity.category == "IDEA" -> Color(0xFFFFEA03)
        activity.category == "CHAT" -> Color(0xFF3B82F6)
        activity.category == "CALENDAR" -> Color(0xFF00C43B)
        else -> Color(0xFF475569)
    }

    val formattedTime = remember(activity.timestamp) {
        val sdf = SimpleDateFormat("HH:mm, d.M.", Locale.getDefault())
        sdf.format(Date(activity.timestamp))
    }

    val likedIds = activity.likedByIds.split(",").filter { it.isNotBlank() }
    val isLiked = activeUser != null && likedIds.contains(activeUser.id)
    // OBLÍBENÉ BARVY - 1x světlá, 2x žlutá, 3x nejvíc žlutá
    val favoriteColor = when (likedIds.size) {
        1 -> Color(0xFFFEF3C7) // nejméně žlutá - světlá
        2 -> Color(0xFFFBBF24) // žlutá
        3 -> Color(0xFFEAB308) // nejvíce žlutá
        else -> Color(0xFF334155)
    }
    val borderWidth = when (likedIds.size) {
        1 -> 1.dp
        2 -> 2.dp
        3 -> 3.dp
        else -> 1.dp
    }
    val borderCol = if (likedIds.isNotEmpty()) favoriteColor else Color(0xFF334155)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClickItem, onLongClick = onLongPress),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = androidx.compose.foundation.BorderStroke(borderWidth, borderCol)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            val prioColor = priorityColor(activity.priority)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(categoryBg)
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(prioColor).border(1.dp, Color.White, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    val categoryCz = when {
                        isComment -> "KOMENTÁŘ"
                        else -> when (activity.category) {
                        "TASK" -> "ÚKOL"
                        "NOTE" -> "POZNÁMKA"
                        "IDEA" -> "NÁPAD"
                        "CALENDAR" -> "KALENDÁŘ"
                        "CHAT" -> "CHAT"
                        else -> activity.category
                        }
                    }
                    Text(text = categoryCz, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.title,
                    fontWeight = FontWeight.SemiBold,
                    color = if (activity.isReadByActiveUser) Color(0xFF94A3B8) else Color.White,
                    fontSize = 13.sp,
                    textDecoration = if (activity.isReadByActiveUser) TextDecoration.LineThrough else TextDecoration.None
                )
                if (activity.description.isNotBlank()) {
                    Text(
                        text = activity.description,
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val realAuthorName = users.find { it.id == activity.authorId }?.nickname?.takeIf { it.lowercase() != "secret" } ?: activity.authorName.takeIf { it.lowercase() != "secret" } ?: "Tom"
                Text(text = realAuthorName, color = Color(0xFF60A5FA), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(text = formattedTime, color = Color(0xFF64748B), fontSize = 10.sp)
            }
        }
            // Druhý řádek pro liky - Líbí se : Tom, Adélka / Líbí se to všem
            Row(modifier = Modifier.fillMaxWidth()) {
            if (likedIds.isNotEmpty()) {
                Spacer(modifier = Modifier.width(6.dp))
                val text = if (likedIds.size >= 3) {
                    "Toto se líbí všem"
                } else {
                    val names = likedIds.mapNotNull { id -> users.find { it.id == id }?.nickname }
                    "Líbí se: ${names.joinToString(", ")}"
                }
                Text(
                    text = text,
                    color = favoriteColor,
                    fontSize = 10.sp,
                    fontWeight = if (likedIds.size >= 3) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(favoriteColor.copy(alpha = 0.15f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
            }
        }
    }
}

@Composable
fun NavTileButton(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) color.copy(alpha = 0.22f) else Color(0xFF1E293B)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (enabled) color.copy(alpha = 0.6f) else Color(0xFF334155)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (enabled) color else Color(0xFF64748B),
                    modifier = Modifier.size(27.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (enabled) Color.White else Color(0xFF94A3B8)
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 9.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

@Composable
fun NavTileButtonImg(
    title: String,
    drawableRes: Int,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.22f)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, color.copy(alpha = 0.6f))
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painter = painterResource(id = drawableRes), contentDescription = title, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
            }
        }
    }
}

@Composable
fun HelpGuideDialog(onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = Color(0xFF60A5FA))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Průvodce & Vysvětlení aplikace", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
            ) {
                item {
                    HelpCardSection(title = "1. Domovská stránka", icon = Icons.Default.Settings, color = Color(0xFF3B82F6), description = "Zde je hlavní přehled posledních aktivit + základní navigace")
                }
                item {
                    HelpCardSection(title = "2. Úkoly a poznámky", icon = Icons.Default.CheckCircle, color = Color(0xFF3B82F6), description = "Zde můžeme vytvořit nový úkol nebo poznámku")
                }
                item {
                    HelpCardSection(title = "3. Nápady a realizace", icon = Icons.Default.Lightbulb, color = Color(0xFFEAB308), description = "Máš nápad? Přidej ho sem! Ostatní členové můžou hlasovat pro postup do realizace - zde pak hlasují všichni a nápad se přesune do realizace - a zde bude finální hlasování. Když všichni schválíme = nápad se pokusíme zrealizovat")
                }
                item {
                    HelpCardSection(title = "4. Chat", icon = Icons.AutoMirrored.Filled.Chat, color = Color(0xFF10B981), description = "Chat v reálném čase - Důležitá zpráva vytvoří varovný banner na obrazovce + možnost rychlé odpovědi")
                }
                item {
                    HelpCardSection(title = "5. Kalendář", icon = Icons.Default.CalendarMonth, color = Color(0xFF06B6D4), description = "Měsíční mřížka, vytváření událostí")
                }
                item {
                    HelpCardSection(title = "6. Profil a nastavení", icon = Icons.Default.Settings, color = Color(0xFFEC4899), description = "Změna přezdívky, e-mailu, přihlašovacího pinu, ikonka, obrys")
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = onDismiss,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
            ) {
                Text("Rozumím, zavřít", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun HelpCardSection(
    title: String,
    icon: ImageVector,
    color: Color,
    description: String
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(description, color = Color(0xFFCBD5E1), fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}
