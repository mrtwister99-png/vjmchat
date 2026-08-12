package com.example.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ChatMessageEntity
import com.example.data.UserEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONObject
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecretChatScreen(
    viewModel: MainViewModel,
    activeUser: UserEntity?,
    users: List<UserEntity>,
    onBack: () -> Unit
) {
    val secretMessages by viewModel.secretChatMessages.collectAsState(initial = emptyList())
    var textInput by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf("NONE") }
    var showReactionFor by remember { mutableStateOf<Long?>(null) }
    var displayedCount by remember { androidx.compose.runtime.mutableIntStateOf(20) }
    val listState = rememberLazyListState()
    val totalCount = secretMessages.size
    var didInitialScroll by remember { mutableStateOf(false) }
    val visibleMessages = remember(secretMessages, displayedCount) {
        if (totalCount <= displayedCount) secretMessages else secretMessages.takeLast(displayedCount)
    }

    LaunchedEffect(Unit) {
        displayedCount = 20
        didInitialScroll = false
    }

    LaunchedEffect(visibleMessages.size, didInitialScroll) {
        if (!didInitialScroll && visibleMessages.isNotEmpty()) {
            listState.scrollToItem(visibleMessages.size - 1)
            didInitialScroll = true
        }
    }

    LaunchedEffect(totalCount, secretMessages.lastOrNull()?.id) {
        if (visibleMessages.isNotEmpty()) {
            listState.animateScrollToItem(visibleMessages.size - 1)
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { idx ->
                if (idx == 0 && displayedCount < totalCount) {
                    displayedCount += 20
                }
            }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Adélka", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            val adelka = users.find { it.id == "kamaradka" }
                            val isOnline = adelka?.isOnline == true
                            val lastSeen = adelka?.lastSeenTimestamp ?: 0L
                            val mins = (System.currentTimeMillis() - lastSeen)/60000
                            val txt = if(isOnline) "aktivní" else if(mins<1) "aktivní před chvílí" else if(mins<60) "aktivní před ${mins} min" else "aktivní před ${mins/60} h"
                            Text(txt, color = if(isOnline) Color(0xFF22C55E) else Color(0xFFE2E8F0), fontSize = 11.sp)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět", tint = Color.White)
                    }
                },
                actions = { },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0B0F19)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            // Messages List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (secretMessages.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Zatím žádné tajné zprávy", color = Color(0xFF7E22CE), fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(visibleMessages, key = { it.id }) { msg ->
                            val isMe = msg.senderId == activeUser?.id
                            val senderUser = users.find { it.id == msg.senderId }
                            ChatMessageBubble(
                                message = msg,
                                isMe = isMe,
                                senderUser = senderUser,
                                activeUser = activeUser,
                                users = users,
                                onLongPress = {
                                    if (msg.content.trim().length >= 1) {
                                        showReactionFor = if (showReactionFor == msg.id) null else msg.id
                                    }
                                },
                                showReactions = showReactionFor == msg.id,
                                onReaction = { type ->
                                    viewModel.setMessageReaction(msg, if(type.isEmpty()) null else type)
                                    showReactionFor = null
                                },
                                onDoubleTapPraise = {},
                                onDismissReactions = { showReactionFor = null }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Input Dock - už jen vnější imePadding drží vše
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(8.dp)
                ) {
                    val priorityData = when (selectedPriority) {
                        "HIGH" -> Triple(Color(0xFFEF4444), 3.dp, "🔴")
                        "MEDIUM" -> Triple(Color(0xFFF97316), 2.dp, "🟠")
                        "LOW" -> Triple(Color(0xFFEAB308), 1.dp, "🟡")
                        else -> Triple(Color.Transparent, 0.dp, "⚪")
                    }
                    val priorityColor = priorityData.first
                    val priorityWidth = priorityData.second
                    Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF0F172A)).border(if (selectedPriority=="NONE") 1.dp else priorityWidth, if (selectedPriority=="NONE") Color(0xFF334155) else priorityColor, RoundedCornerShape(8.dp)).clickable(onClick = {
                        selectedPriority = when (selectedPriority) { "NONE" -> "LOW"; "LOW" -> "MEDIUM"; "MEDIUM" -> "HIGH"; else -> "NONE" }
                    }), contentAlignment = Alignment.Center) {
                        Text(text = when (selectedPriority) { "HIGH" -> "🔴"; "MEDIUM" -> "🟠"; "LOW" -> "🟡"; else -> "⚪" }, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("Napiš tajnou zprávu...", color = Color(0xFF9CA3AF), fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A),
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFF334155)
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                viewModel.sendSecretChatMessage(textInput, selectedPriority)
                                textInput = ""
                                selectedPriority = "NONE"
                            }
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF3B82F6))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Odeslat", tint = Color.White)
                    }
                }
            }
        }
    }
    
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SecretMessageBubble(
    message: ChatMessageEntity,
    isMe: Boolean,
    senderUser: UserEntity?,
    activeUser: UserEntity?,
    onLongPress: () -> Unit,
    showReactions: Boolean,
    onReaction: (String) -> Unit,
    onDismissReactions: () -> Unit = {}
) {
    val reactionMap = remember(message.reactionEmoji) {
        try {
            val raw = message.reactionEmoji
            if (raw.isNullOrBlank()) emptyMap<String, Int>()
            else {
                val obj = JSONObject(raw)
                val values = mutableListOf<String>()
                obj.keys().forEach { k -> values.add(obj.getString(k)) }
                values.groupingBy { it }.eachCount()
            }
        } catch (e: Exception) { emptyMap() }
    }

    val myBubbleColor = parseHexColor(activeUser?.chatBubbleColorHex, Color(0xFFDC2626))
    val senderBubbleColor = parseHexColor(senderUser?.chatBubbleColorHex, Color(0xFF2563EB))
    val bubbleBg = if (isMe) myBubbleColor else senderBubbleColor
    val isSquare = if (isMe) activeUser?.chatBubbleShape == "SQUARE" else senderUser?.chatBubbleShape == "SQUARE"
    val bubbleShape = if (isSquare) {
        if (isMe) RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
        else RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
    } else {
        if (isMe) RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
        else RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
    }
    val formattedTime = remember(message.timestamp) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)) }
    val avatarBg = if (isSquare) Color.White else Color(0xFFE91E8F)
    val avatarShape = if (isSquare) CircleShape else RoundedCornerShape(4.dp)

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 6.dp)) {
            Box(modifier = Modifier.fillMaxWidth(0.8f).align(if (isMe) Alignment.CenterStart else Alignment.CenterEnd)) {
                Card(shape = bubbleShape, colors = CardDefaults.cardColors(containerColor = bubbleBg), modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { if(showReactions) onDismissReactions() }, onLongClick = onLongPress)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(message.content, color = Color.White, fontSize = 14.sp, lineHeight = 18.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(formattedTime, color = Color.White.copy(alpha=0.7f), fontSize = 10.sp, modifier = Modifier.align(if(isMe) Alignment.Start else Alignment.End))
                    }
                }
            }
            Box(modifier = Modifier.size(28.dp).clip(avatarShape).background(avatarBg).border(1.5.dp, Color.White, avatarShape).align(if (isMe) Alignment.TopEnd else Alignment.TopStart).offset(x = if(isMe) 8.dp else (-8).dp, y = (-8).dp), contentAlignment = Alignment.Center) {
                Text(if (isMe) activeUser?.avatarEmoji ?: "👑" else senderUser?.avatarEmoji ?: "💜", fontSize = 14.sp)
            }
            Box(modifier = Modifier.align(if (isMe) Alignment.TopEnd else Alignment.TopStart).padding(top = 30.dp, start = if(isMe) 0.dp else 36.dp, end = if(isMe) 36.dp else 0.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFFFCC80)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                Text(if (isMe) "Já" else "Adélka", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (reactionMap.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth(0.8f).padding(top = 2.dp).align(if(isMe) Alignment.End else Alignment.Start), horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start) {
            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF22D3EE)).padding(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    reactionMap.entries.take(3).forEach { (type, _) ->
                        val res = when(type) { "like" -> com.example.R.drawable.like; "disslike","dislike" -> com.example.R.drawable.disslike; "fucker" -> com.example.R.drawable.fucker; else -> com.example.R.drawable.like }
                        Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(0xFFFFEB3B)).border(1.dp, Color.Black, CircleShape), contentAlignment = Alignment.Center) {
                            androidx.compose.foundation.Image(painter = androidx.compose.ui.res.painterResource(id = res), contentDescription = type, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }

    AnimatedVisibility(visible = showReactions, enter = slideInVertically(initialOffsetY = { -it/2 }) + fadeIn() + scaleIn(), exit = slideOutVertically(targetOffsetY = { -it/2 }) + fadeOut()) {
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)), modifier = Modifier.padding(top = 6.dp)) {
            Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(Color(0xFFFFEB3B)).border(2.dp, Color.White, CircleShape).clickable { onReaction("like") }, contentAlignment = Alignment.Center) {
                    androidx.compose.foundation.Image(painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.like), contentDescription = "like", modifier = Modifier.size(24.dp))
                }
                Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(Color(0xFFFFEB3B)).border(2.dp, Color.White, CircleShape).clickable { onReaction("disslike") }, contentAlignment = Alignment.Center) {
                    androidx.compose.foundation.Image(painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.disslike), contentDescription = "disslike", modifier = Modifier.size(24.dp))
                }
                Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(Color(0xFFFFEB3B)).border(2.dp, Color.White, CircleShape).clickable { onReaction("fucker") }, contentAlignment = Alignment.Center) {
                    androidx.compose.foundation.Image(painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.fucker), contentDescription = "fucker", modifier = Modifier.size(24.dp))
                }
            }
        }
    }
     }
}                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   

private fun parseHexColor(hex: String?, fallback: Color): Color {
    return try {
        if (hex.isNullOrBlank()) fallback else Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        fallback
    }
}