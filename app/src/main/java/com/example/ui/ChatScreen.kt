package com.example.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ChatMessageEntity
import com.example.data.UserEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONObject
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.offset
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.util.Base64
import android.provider.OpenableColumns
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    viewModel: MainViewModel,
    chatMessages: List<ChatMessageEntity>,
    activeUser: UserEntity?,
    users: List<UserEntity>,
    onBack: () -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf("NONE") }
    var showReactionFor by remember { mutableStateOf<Long?>(null) }
    var displayedCount by remember { mutableIntStateOf(25) }
    var showMoreDialog by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var searchJumpToMessageId by remember { mutableStateOf<Long?>(null) }
    var showMeetingDialog by remember { mutableStateOf(false) }
    var showNeedDialog by remember { mutableStateOf(false) }
    var selectedNeedCategory by remember { mutableStateOf("Předělat obrázek") }
    
    val vjmPositiveTemplates = remember {
        listOf(
            "VJM: Dneska jedeme skvele, diky za spolupraci.",
            "VJM: Super tempo, pokracujeme stejne kvalitne.",
            "VJM: Diky tymu za nasazeni, jsme na dobre ceste.",
            "VJM: Dnesni progress je vyborny, jen tak dal.",
            "VJM: Skvela prace, drzte fokus a dokonctime to."
        )
    }

    val listState = rememberLazyListState()
    var didInitialScroll by remember { mutableStateOf(false) }
    val totalCount = chatMessages.size
    val visibleMessages = remember(chatMessages, displayedCount) {
        if (totalCount <= displayedCount) chatMessages else chatMessages.takeLast(displayedCount)
    }

    var restoreAnchorMessageId by remember { mutableStateOf<Long?>(null) }
    var restoreAnchorOffset by remember { mutableIntStateOf(0) }
    var loadingOlder by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        displayedCount = 25
        didInitialScroll = false
    }

    LaunchedEffect(visibleMessages.size, didInitialScroll) {
        if (!didInitialScroll && visibleMessages.isNotEmpty()) {
            listState.scrollToItem(visibleMessages.size - 1)
            delay(50)
            listState.animateScrollToItem(visibleMessages.size - 1)
            didInitialScroll = true
        }
    }

    LaunchedEffect(chatMessages.lastOrNull()?.id, didInitialScroll, activeUser?.id, activeUser?.nickname) {
        val last = chatMessages.lastOrNull() ?: return@LaunchedEffect
        if (!didInitialScroll || visibleMessages.isEmpty()) return@LaunchedEffect
        val isMine = last.senderId == activeUser?.id || last.senderName == activeUser?.nickname
        if (isMine) {
            listState.animateScrollToItem(visibleMessages.size - 1)
        }
    }
    LaunchedEffect(showReactionFor) {
        if (showReactionFor != null) {
            val idx = visibleMessages.indexOfFirst { it.id == showReactionFor }
            if (idx != -1) listState.animateScrollToItem(idx)
        }
    }
    LaunchedEffect(listState, visibleMessages, displayedCount, totalCount) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }.collect { (idx, offset) ->
            if (idx == 0 && displayedCount < totalCount && !loadingOlder) {
                restoreAnchorMessageId = visibleMessages.firstOrNull()?.id
                restoreAnchorOffset = offset
                loadingOlder = true
                displayedCount = (displayedCount + 25).coerceAtMost(totalCount)
            }
        }
    }
    LaunchedEffect(displayedCount, restoreAnchorMessageId, visibleMessages.size) {
        val anchorId = restoreAnchorMessageId ?: return@LaunchedEffect
        val newIdx = visibleMessages.indexOfFirst { it.id == anchorId }
        if (newIdx >= 0) {
            listState.scrollToItem(newIdx, restoreAnchorOffset)
        }
        restoreAnchorMessageId = null
        loadingOlder = false
    }
    LaunchedEffect(searchJumpToMessageId, visibleMessages.size, displayedCount) {
        val targetId = searchJumpToMessageId ?: return@LaunchedEffect
        val idx = visibleMessages.indexOfFirst { it.id == targetId }
        if (idx != -1) {
            listState.scrollToItem(idx)
            searchJumpToMessageId = null
        }
    }

    val uploadedItems = remember(chatMessages) {
        chatMessages.mapNotNull { msg ->
            val hasImage = !msg.imageUri.isNullOrBlank()
            val hasFile = !msg.attachmentDataUri.isNullOrBlank() || !msg.attachmentName.isNullOrBlank()
            val fileLabel = msg.attachmentName ?: parseAttachedFileLabel(msg.content)
            if (!hasImage && !hasFile && fileLabel == null) {
                null
            } else {
                UploadedAttachmentItem(
                    id = msg.id,
                    label = if (hasImage) (fileLabel ?: "Obrázek") else fileLabel ?: "Soubor",
                    subtitle = msg.content.take(50),
                    uri = if (hasImage) msg.imageUri else msg.attachmentDataUri,
                    isImage = hasImage,
                    mimeType = if (hasImage) "image/*" else msg.attachmentMimeType,
                    time = SimpleDateFormat("d.M. HH:mm", Locale.getDefault()).format(Date(msg.timestamp))
                )
            }
        }.sortedBy { it.id }
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    BackHandler(enabled = showReactionFor != null) {
        showReactionFor = null
    }
    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val mime = context.contentResolver.getType(uri).orEmpty()
        if (mime.startsWith("image/")) {
            val text = textInput.trim().ifBlank { "[Obrázek]" }
            val fileName = resolveDisplayName(context, uri)
            scope.launch {
                val success = viewModel.uploadAttachmentAndSendChatMessage(uri.toString(), fileName, mime.ifBlank { "image/jpeg" }, text, selectedPriority, true)
                Toast.makeText(context, if (success) "UPLOAD OK: obrázek" else "UPLOAD FAIL: obrázek", Toast.LENGTH_SHORT).show()
            }
        } else {
            val fileName = resolveDisplayName(context, uri)
            val text = if (textInput.isBlank()) "📎 Soubor: $fileName" else "${textInput.trim()}\n📎 Soubor: $fileName"
            scope.launch {
                val success = viewModel.uploadAttachmentAndSendChatMessage(uri.toString(), fileName, mime.ifBlank { guessMimeFromName(fileName) }, text, selectedPriority, false)
                Toast.makeText(context, if (success) "UPLOAD OK: soubor" else "UPLOAD FAIL: soubor", Toast.LENGTH_SHORT).show()
            }
        }
        textInput = ""
        selectedPriority = "NONE"
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap == null) return@rememberLauncherForActivityResult
        val uri = saveBitmapToCache(context, bitmap)
        if (uri != null) {
            val text = textInput.trim().ifBlank { "[Foto]" }
            scope.launch {
                val success = viewModel.uploadAttachmentAndSendChatMessage(uri.toString(), "chat_photo_${System.currentTimeMillis()}.jpg", "image/jpeg", text, selectedPriority, true)
                Toast.makeText(context, if (success) "UPLOAD OK: foto" else "UPLOAD FAIL: foto", Toast.LENGTH_SHORT).show()
            }
            textInput = ""
            selectedPriority = "NONE"
        } else {
            Toast.makeText(context, "Foto se nepodařilo uložit", Toast.LENGTH_SHORT).show()
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) cameraLauncher.launch(null)
        else Toast.makeText(context, "Bez povolení kamery nelze fotit", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Týmový Chat", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("3 členové (Michal, Adélka, Tom)", color = Color(0xFF60A5FA), fontSize = 11.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showMoreDialog = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Soubory", tint = Color(0xFF60A5FA))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0B0F19)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .navigationBarsPadding()
                .clickable(enabled = showReactionFor != null) { showReactionFor = null }
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            // Messages - FIX černé obrazovky: odstraněn velký clickable Box přes celou obrazovku
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 8.dp, top = 4.dp)
            ) {
                if (loadingOlder && displayedCount < totalCount) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("Nacitam starsi zpravy...", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                    }
                }
                if (chatMessages.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                            Text("Zatím žádné zprávy v chatu", color = Color(0xFF475569), fontSize = 14.sp)
                        }
                    }
                } else {
                    items(visibleMessages, key = { it.id }) { msg ->
                        val isMe = msg.senderId == activeUser?.id || msg.senderName == activeUser?.nickname
                        val sender = users.find { it.id == msg.senderId } ?: users.find { it.nickname == msg.senderName }
                        ChatMessageBubble(
                            message = msg,
                            isMe = isMe,
                            senderUser = sender,
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
                            onDoubleTapPraise = {
                                viewModel.incrementPraise(msg)
                            },
                            onDismissReactions = { showReactionFor = null },
                            onDownloadAttachment = { uri, fileName, mimeType, isImage ->
                                val saved = downloadAttachment(context, uri, fileName, mimeType, isImage)
                                Toast.makeText(context, if (saved) "Staženo" else "Stažení se nezdařilo", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // INPUT DOCK - NOVÝ DESIGN podle předlohy image_3135eb
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(6.dp)
                ) {
                    // 1. PRIORITA - jen obrys
                    PriorityBadge(
                        priority = selectedPriority,
                        selected = true,
                        onClick = {
                            selectedPriority = when(selectedPriority) {
                                "NONE" -> "LOW"
                                "LOW" -> "MEDIUM"
                                "MEDIUM" -> "HIGH"
                                else -> "NONE"
                            }
                        },
                        size = 38.dp
                    )
                    val priorityBorderColor = priorityColor(selectedPriority)

                    Spacer(modifier = Modifier.width(4.dp))

                    // 2. RYCHLÉ AKCE - blesk
                    Box(
                        modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF334155))
                            .clickable { showMoreDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = "Rychlé akce", tint = Color(0xFFFACC15), modifier = Modifier.size(20.dp))
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // 3. SOUBOR + FOTKA
                    Box(
                        modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F172A))
                            .clickable { filePickerLauncher.launch("*/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Soubor", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Box(
                        modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F172A))
                            .clickable {
                                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                if (granted) cameraLauncher.launch(null) else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = "Foto", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("Napiš zprávu...", color = Color(0xFF64748B), fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = false,
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A),
                            focusedBorderColor = if(selectedPriority!="NONE") priorityBorderColor else Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFF1E293B),
                            cursorColor = Color(0xFF3B82F6)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Prázdný input = VJM zpráva, při psaní klasické odeslání
                    Box(
                        modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                            .background(if(selectedPriority!="NONE") priorityBorderColor else Color(0xFF2563EB))
                            .clickable {
                                if (textInput.isNotBlank()) {
                                    viewModel.sendChatMessage(textInput, priority = selectedPriority)
                                    textInput = ""
                                    selectedPriority = "NONE"
                                } else {
                                    val msg = vjmPositiveTemplates[(System.currentTimeMillis() % vjmPositiveTemplates.size).toInt()]
                                    viewModel.sendChatMessage(msg, priority = selectedPriority)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (textInput.isBlank()) {
                            Text("VJM", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Odeslat",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
        }

        if (showMoreDialog) {
            ChatMoreDialog(
                onDismiss = { showMoreDialog = false },
                onSearch = { showMoreDialog = false; showSearchDialog = true },
                onMeeting = { showMoreDialog = false; showMeetingDialog = true },
                onNeed = {
                    selectedNeedCategory = "Potřebuji"
                    showMoreDialog = false
                    showNeedDialog = true
                },
                onCustomCategory = {
                    selectedNeedCategory = "Vlastní kategorie"
                    showMoreDialog = false
                    showNeedDialog = true
                },
            )
        }

        if (showSearchDialog) {
            ChatSearchDialog(
                messages = chatMessages,
                onDismiss = { showSearchDialog = false },
                onSelectMessage = { message ->
                    val targetIndex = chatMessages.indexOfFirst { it.id == message.id }
                    if (targetIndex >= 0) {
                        val neededCount = chatMessages.size - targetIndex
                        displayedCount = maxOf(displayedCount, neededCount)
                        searchJumpToMessageId = message.id
                    }
                    showSearchDialog = false
                }
            )
        }

        if (showNeedDialog) {
            NeedRequestDialog(
                category = selectedNeedCategory,
                onDismiss = { showNeedDialog = false },
                onSubmit = { content, attachmentUri, attachmentName, attachmentMimeType, isImage, priority ->
                    scope.launch {
                        val sent = if (attachmentUri.isNullOrBlank()) {
                            viewModel.sendChatMessage(content = content, priority = priority)
                            true
                        } else {
                            viewModel.uploadAttachmentAndSendChatMessage(
                                localUri = attachmentUri,
                                fileName = attachmentName ?: resolveDisplayName(context, Uri.parse(attachmentUri)),
                                mimeType = attachmentMimeType ?: if (isImage) "image/jpeg" else guessMimeFromName(attachmentName ?: "soubor"),
                                content = content,
                                priority = priority,
                                isImage = isImage
                            )
                        }
                        if (!sent) {
                            Toast.makeText(context, "Odeslání přílohy se nezdařilo", Toast.LENGTH_SHORT).show()
                        }
                    }
                    showNeedDialog = false
                }
            )
        }

        if (showMeetingDialog) {
            MeetingRequestDialog(
                onDismiss = { showMeetingDialog = false },
                onConfirm = { dateTime, title ->
                    // 1. Do kalendáře
                    viewModel.addCalendarEvent(
                        title = title.ifBlank { "Meeting" },
                        description = "Žádost od ${activeUser?.nickname}",
                        colorCategoryHex = "#3B82F6",
                        startTs = dateTime,
                        endTs = dateTime + 3600000,
                        isAllDay = false, location = "", isRecurring = false, recurrenceRule = "Žádné", reminderMins = 15, priority = "HIGH"
                    )
                    // 2. Do chatu jako speciální zpráva
                    viewModel.sendChatMessage("📅 ŽÁDOST O MEETING: $title - ${SimpleDateFormat("d.M. HH:mm", Locale.getDefault()).format(Date(dateTime))} | ${activeUser?.nickname} žádá o čas. Reagujte: Mám čas / Nemám čas", priority = "HIGH")
                    showMeetingDialog = false
                }
            )
        }

    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMessageBubble(
    message: ChatMessageEntity,
    isMe: Boolean,
    senderUser: UserEntity?,
    activeUser: UserEntity?,
    users: List<UserEntity>,
    onLongPress: () -> Unit,
    showReactions: Boolean,
    onReaction: (String) -> Unit,
    onDoubleTapPraise: () -> Unit,
    onDismissReactions: () -> Unit = {},
    onDownloadAttachment: (String, String, String?, Boolean) -> Unit = { _, _, _, _ -> }
) {
    val reactionByUser = remember(message.reactionEmoji) {
        try {
            val raw = message.reactionEmoji
            if (raw.isNullOrBlank()) emptyMap<String, String>()
            else {
                val obj = JSONObject(raw)
                val map = mutableMapOf<String, String>()
                obj.keys().forEach { key -> map[key] = obj.getString(key) }
                map
            }
        } catch (e: Exception) { emptyMap() }
    }

    val reactionVotes = reactionByUser.filterKeys { !it.startsWith("__praise") }
    val reactionCount1 = reactionVotes.values.count { it == "like" }
    val reactionCount2 = reactionVotes.values.count { it == "disslike" || it == "dislike" }
    val reactionCount3 = reactionVotes.values.count { it == "fucker" }
    val praiseCount = reactionByUser.keys.count { it.startsWith("__praise_") } +
        (reactionByUser["__praise"]?.toIntOrNull() ?: 0)

    val myBubbleColor = parseHexColor(activeUser?.chatBubbleColorHex, Color(0xFFDC2626))
    val senderBubbleColor = parseHexColor(senderUser?.chatBubbleColorHex, Color(0xFF2563EB))
    val bubbleBg = if (isMe) myBubbleColor else senderBubbleColor
    val priorityBorderColor = when (message.priority) {
        "HIGH" -> Color(0xFFEF4444)
        "MEDIUM" -> Color(0xFFF97316)
        "LOW" -> Color(0xFFEAB308)
        else -> Color.White.copy(alpha = 0.14f)
    }
    val priorityBorderWidth = when (message.priority) {
        "HIGH" -> 2.dp
        "MEDIUM" -> 1.5.dp
        "LOW" -> 1.dp
        else -> 0.8.dp
    }
    val isSquare = if (isMe) activeUser?.chatBubbleShape == "SQUARE" else senderUser?.chatBubbleShape == "SQUARE"
    // Hranatost/zaoblenost jen na jedne strane
    val bubbleShape = if (isSquare) {
        if (isMe) RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
        else RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
    } else {
        if (isMe) RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
        else RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
    }
    val formattedTime = remember(message.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    }

    val bubbleTopPadding = if (showReactions) 30.dp else 12.dp
    val displayName = if (isMe) activeUser?.nickname ?: message.senderName else senderUser?.nickname ?: message.senderName

    Column(modifier = Modifier.fillMaxWidth()) {
        var bubbleVisible by remember(message.id) { mutableStateOf(false) }
        LaunchedEffect(message.id) {
            bubbleVisible = true
        }
        AnimatedVisibility(
            visible = bubbleVisible,
            enter = slideInHorizontally(initialOffsetX = { if (isMe) -it else it }) + fadeIn(),
            exit = fadeOut()
        ) {
        Box(modifier = Modifier.fillMaxWidth().padding(top = bubbleTopPadding, bottom = 6.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(if (isMe) Alignment.CenterStart else Alignment.CenterEnd),
                horizontalArrangement = if (isMe) Arrangement.Start else Arrangement.End,
                verticalAlignment = Alignment.Top
            ) {
                if (!isMe) {
                    MessageTiles(
                        displayName = displayName,
                        formattedTime = formattedTime,
                        avatarEmoji = senderUser?.avatarEmoji ?: "👤",
                        borderHex = senderUser?.borderHexColor,
                        priority = message.priority,
                        tile2Count = praiseCount
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Card(
                    shape = bubbleShape,
                    colors = CardDefaults.cardColors(containerColor = bubbleBg),
                    border = androidx.compose.foundation.BorderStroke(priorityBorderWidth, priorityBorderColor),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 92.dp)
                        .combinedClickable(
                            onClick = { if(showReactions) onDismissReactions() },
                            onLongClick = onLongPress,
                            onDoubleClick = onDoubleTapPraise
                        )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        if (!message.imageUri.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            ) {
                                AsyncImage(
                                    model = toAttachmentModel(message.imageUri),
                                    contentDescription = "Příloha",
                                    modifier = Modifier.fillMaxSize()
                                )
                                IconButton(
                                    onClick = {
                                        onDownloadAttachment(
                                            message.imageUri,
                                            message.attachmentName ?: "foto_${message.id}.jpg",
                                            message.attachmentMimeType ?: "image/*",
                                            true
                                        )
                                    },
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(6.dp)
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.55f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Download,
                                        contentDescription = "Stáhnout fotku",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        if (message.attachmentName != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0F172A).copy(alpha = 0.35f))
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    "📎 ${message.attachmentName}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(end = 34.dp)
                                )
                                IconButton(
                                    onClick = {
                                        val attachmentUri = message.attachmentDataUri ?: message.imageUri
                                        if (!attachmentUri.isNullOrBlank()) {
                                            onDownloadAttachment(
                                                attachmentUri,
                                                message.attachmentName ?: "soubor_${message.id}",
                                                message.attachmentMimeType,
                                                false
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1E293B))
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Download,
                                        contentDescription = "Stáhnout soubor",
                                        tint = Color(0xFF93C5FD),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        val displayText = message.content.takeIf { it.isNotBlank() && it != "[Obrázek]" && it != "[Foto]" }
                        if (displayText != null) {
                            Text(displayText, color = Color.White, fontSize = 14.sp, lineHeight = 18.sp)
                        }
                    }
                }
                if (isMe) {
                    Spacer(modifier = Modifier.width(8.dp))
                    MessageTiles(
                        displayName = displayName,
                        formattedTime = formattedTime,
                        avatarEmoji = activeUser?.avatarEmoji ?: "👑",
                        borderHex = activeUser?.borderHexColor,
                        priority = message.priority,
                        tile2Count = praiseCount
                    )
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = showReactions,
                enter = slideInVertically(initialOffsetY = { -it / 2 }) + fadeIn() + scaleIn(),
                exit = slideOutVertically(targetOffsetY = { -it / 2 }) + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter).offset(y = (-8).dp)
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReactionImageButton(drawableRes = R.drawable.like, onClick = { onReaction("like") })
                        ReactionImageButton(drawableRes = R.drawable.disslike, onClick = { onReaction("disslike") })
                        ReactionImageButton(drawableRes = R.drawable.fucker, onClick = { onReaction("fucker") })
                    }
                }
            }
        }
        }

    }
}

@Composable
private fun MessageTiles(
    displayName: String,
    formattedTime: String,
    avatarEmoji: String,
    borderHex: String?,
    priority: String,
    tile2Count: Int
) {
    val borderColor = parseHexColor(borderHex, Color(0xFF3B82F6))
    val priorityColor = when (priority) {
        "HIGH" -> Color(0xFFEF4444)
        "MEDIUM" -> Color(0xFFF97316)
        "LOW" -> Color(0xFFEAB308)
        else -> Color(0xFF64748B)
    }
    var praisePulse by remember(tile2Count) { mutableStateOf(false) }
    val praiseScale by animateFloatAsState(
        targetValue = if (praisePulse) 1.14f else 1f,
        animationSpec = tween(180),
        label = "praisePulse"
    )

    LaunchedEffect(tile2Count) {
        if (tile2Count > 0) {
            praisePulse = true
            delay(220)
            praisePulse = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.width(92.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .scale(praiseScale)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                        .border(2.dp, borderColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(avatarEmoji, fontSize = 14.sp)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(contentAlignment = Alignment.TopEnd, modifier = Modifier.size(30.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.gj),
                        contentDescription = "Pochvala",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .offset(x = 2.dp, y = (-2).dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("$tile2Count", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 8.sp)
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF0F172A))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(displayName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, maxLines = 1)
                    Text(formattedTime, color = Color(0xFF94A3B8), fontSize = 9.sp, maxLines = 1)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(priorityColor)
                        .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                )
            }
        }
    }
}

@Composable
private fun ReactionImageButton(drawableRes: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color(0xFF0F172A))
            .border(1.dp, Color(0xFF334155), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            contentScale = ContentScale.Fit
        )
    }
}

data class UploadedAttachmentItem(
    val id: Long,
    val label: String,
    val subtitle: String,
    val uri: String?,
    val isImage: Boolean,
    val mimeType: String?,
    val time: String
)

@Composable
fun ChatMoreDialog(
    onDismiss: () -> Unit,
    onSearch: () -> Unit,
    onMeeting: () -> Unit,
    onNeed: () -> Unit,
    onCustomCategory: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("⋯ Rychlé akce", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                ActionButton("🔎 Hledat v chatu", Color(0xFF8B5CF6), onClick = onSearch)
                ActionButton("📅 Vytvoř žádost o meeting", Color(0xFF3B82F6), onClick = onMeeting)
                ActionButton("🧩 Potřebuji", Color(0xFF10B981), onClick = onNeed)
                ActionButton("➕ Přidat vlastní kategorii", Color(0xFF22C55E), onClick = onCustomCategory)
            }
        }
    }
}

@Composable
fun ChatSearchDialog(
    messages: List<ChatMessageEntity>,
    onDismiss: () -> Unit,
    onSelectMessage: (ChatMessageEntity) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val matches = remember(messages, query) {
        val q = query.trim()
        if (q.isBlank()) emptyList() else messages.filter {
            it.content.contains(q, ignoreCase = true) ||
                it.senderName.contains(q, ignoreCase = true) ||
                (!it.attachmentName.isNullOrBlank() && it.attachmentName!!.contains(q, ignoreCase = true))
        }.takeLast(50)
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Hledat v chatu", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Zadej slovo nebo jméno", color = Color(0xFF64748B)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF111827),
                        unfocusedContainerColor = Color(0xFF111827),
                        focusedBorderColor = Color(0xFF8B5CF6),
                        unfocusedBorderColor = Color(0xFF334155)
                    )
                )
                if (query.isBlank()) {
                    Text("Vyhledávání prohledá celý chat a vrátí shody.", color = Color(0xFF94A3B8), fontSize = 12.sp)
                } else {
                    Text("Nalezeno: ${matches.size}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    LazyColumn(modifier = Modifier.height(220.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(matches, key = { it.id }) { msg ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF111827))
                                    .clickable { onSelectMessage(msg) }
                                    .padding(10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(msg.senderName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(msg.content.ifBlank { msg.attachmentName ?: "[Příloha]" }, color = Color(0xFFE2E8F0), fontSize = 12.sp, maxLines = 2)
                                }
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Color(0xFF334155)).clickable(onClick = onDismiss).padding(12.dp), contentAlignment = Alignment.Center) {
                        Text("Zavřít", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun UploadedFilesDialog(
    attachments: List<UploadedAttachmentItem>,
    onDismiss: () -> Unit,
    onOpenAttachment: (UploadedAttachmentItem) -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Nahrané soubory", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (attachments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF111827))
                            .padding(12.dp)
                    ) {
                        Text("Zatím nic nahraného", color = Color(0xFF64748B), fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(attachments, key = { it.id }) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF111827))
                                    .clickable { onOpenAttachment(item) }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (item.isImage) Color(0xFF1D4ED8) else Color(0xFF334155)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(if (item.isImage) "IMG" else "FILE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    Text(item.subtitle, color = Color(0xFF94A3B8), fontSize = 10.sp, maxLines = 1)
                                }
                                Text(item.time, color = Color(0xFF64748B), fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NeedRequestDialog(
    category: String,
    onDismiss: () -> Unit,
    onSubmit: (content: String, attachmentUri: String?, attachmentName: String?, attachmentMimeType: String?, isImage: Boolean, priority: String) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var customCategory by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf("MEDIUM") }
    var attachmentMode by remember { mutableStateOf<String?>(null) }
    var selectedAttachmentUri by remember { mutableStateOf<String?>(null) }
    var selectedAttachmentName by remember { mutableStateOf<String?>(null) }
    var selectedAttachmentMime by remember { mutableStateOf<String?>(null) }
    var selectedAttachmentIsImage by remember { mutableStateOf(false) }
    var attachedFileLabel by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val mime = context.contentResolver.getType(uri).orEmpty()
        if (mime.startsWith("image/")) {
            val localUri = copyUriToCache(context, uri) ?: uri
            selectedAttachmentUri = localUri.toString()
            selectedAttachmentName = resolveDisplayName(context, uri)
            selectedAttachmentMime = mime.ifBlank { "image/jpeg" }
            selectedAttachmentIsImage = true
            attachedFileLabel = ""
        } else {
            selectedAttachmentUri = uri.toString()
            selectedAttachmentName = resolveDisplayName(context, uri)
            selectedAttachmentMime = mime.ifBlank { guessMimeFromName(resolveDisplayName(context, uri)) }
            selectedAttachmentIsImage = false
            attachedFileLabel = "📎 Soubor: ${resolveDisplayName(context, uri)}"
        }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap == null) return@rememberLauncherForActivityResult
        val uri = saveBitmapToCache(context, bitmap)
        if (uri != null) {
            selectedAttachmentUri = uri.toString()
            selectedAttachmentName = "chat_photo_${System.currentTimeMillis()}.jpg"
            selectedAttachmentMime = "image/jpeg"
            selectedAttachmentIsImage = true
            attachedFileLabel = ""
        }
    }
    val camPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) camera.launch(null)
        else Toast.makeText(context, "Bez povolení kamery nelze fotit", Toast.LENGTH_SHORT).show()
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Nový požadavek", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                val finalCategory = if (category == "Vlastní kategorie") customCategory else category

                if (attachmentMode == null) {
                    Text("Co chceš přidat?", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        ActionButton("🖼 Fotka", Color(0xFF1D4ED8), onClick = { attachmentMode = "IMAGE" })
                        ActionButton("📎 Soubor", Color(0xFF334155), onClick = { attachmentMode = "FILE" })
                    }
                    TextButton(onClick = onDismiss) { Text("Zrušit", color = Color(0xFF94A3B8)) }
                } else {
                    if (attachmentMode == "IMAGE") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            ActionButton("📷 Vyfotit", Color(0xFF2563EB), onClick = {
                                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                if (granted) camera.launch(null) else camPermission.launch(Manifest.permission.CAMERA)
                            })
                            ActionButton("🖼 Z galerie", Color(0xFF1D4ED8), onClick = { picker.launch("image/*") })
                        }
                    } else {
                        ActionButton("📎 Vybrat soubor", Color(0xFF334155), onClick = { picker.launch("*/*") })
                    }

                    if (category == "Vlastní kategorie") {
                        OutlinedTextField(
                            value = customCategory,
                            onValueChange = { customCategory = it },
                            label = { Text("Název kategorie", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Název", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Popis co je potřeba", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth().height(90.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("LOW" to "🟡", "MEDIUM" to "🟠", "HIGH" to "🔴").forEach { (pri, label) ->
                        val sel = selectedPriority == pri
                        val bg = when (pri) { "HIGH" -> Color(0xFF7F1D1D); "MEDIUM" -> Color(0xFF7C2D12); else -> Color(0xFF713F12) }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sel) bg else Color(0xFF1F2937))
                                .border(if (sel) 2.dp else 1.dp, if (sel) Color.White else Color(0xFF334155), RoundedCornerShape(8.dp))
                                .clickable { selectedPriority = pri }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                    if (selectedAttachmentUri != null) {
                        Text("Přiložena příloha", color = Color(0xFF93C5FD), fontSize = 11.sp)
                    }
                    if (attachedFileLabel.isNotBlank()) {
                        Text(attachedFileLabel, color = Color(0xFF93C5FD), fontSize = 11.sp)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1D4ED8))
                                .clickable {
                                    if (description.isNotBlank()) {
                                        val categoryLabel = finalCategory.ifBlank { "Vlastní" }
                                        val payload = buildString {
                                            append("🧩 POTŘEBUJI: $categoryLabel")
                                            if (title.isNotBlank()) append("\nNázev: ${title.trim()}")
                                            append("\nPopis: ${description.trim()}")
                                            if (attachedFileLabel.isNotBlank()) append("\n$attachedFileLabel")
                                            append("\nReagujte textem, obrázkem nebo souborem.")
                                        }
                                        onSubmit(payload, selectedAttachmentUri, selectedAttachmentName, selectedAttachmentMime, selectedAttachmentIsImage, selectedPriority)
                                    }
                                }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) { Text("Odeslat", color = Color.White, fontWeight = FontWeight.Bold) }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF334155))
                                .clickable(onClick = onDismiss)
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) { Text("Zrušit", color = Color.White) }
                    }
                }
            }
        }
    }
}


@Composable
fun ActionButton(text: String, color: Color, enabled: Boolean = true, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if(enabled) color.copy(alpha=0.2f) else Color(0xFF1E293B)).border(1.dp, if(enabled) color else Color(0xFF334155), RoundedCornerShape(12.dp)).clickable(enabled=enabled, onClick=onClick).padding(12.dp), contentAlignment = Alignment.CenterStart) {
        Text(text, color = if(enabled) Color.White else Color(0xFF64748B), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun MeetingRequestDialog(onDismiss: () -> Unit, onConfirm: (Long, String) -> Unit) {
    var title by remember { mutableStateOf("Meeting") }
    var dateTime by remember { mutableStateOf(System.currentTimeMillis() + 3600000) }
    val context = LocalContext.current
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("📅 Žádost o meeting", color=Color.White, fontWeight=FontWeight.Bold, fontSize=16.sp)
                OutlinedTextField(value=title, onValueChange={title=it}, label={Text("Název", color=Color.Gray)}, colors = OutlinedTextFieldDefaults.colors(focusedTextColor=Color.White, unfocusedTextColor=Color.White, focusedContainerColor=Color(0xFF1E293B), unfocusedContainerColor=Color(0xFF1E293B)), modifier=Modifier.fillMaxWidth())
                Text("Čas: ${SimpleDateFormat("d.M.yyyy HH:mm", Locale.getDefault()).format(Date(dateTime))}", color=Color(0xFF94A3B8), fontSize=13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Color(0xFF1D4ED8)).clickable {
                        val cal = java.util.Calendar.getInstance().apply { timeInMillis = dateTime }
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val updated = java.util.Calendar.getInstance().apply { timeInMillis = dateTime }
                                updated.set(java.util.Calendar.YEAR, year)
                                updated.set(java.util.Calendar.MONTH, month)
                                updated.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth)
                                dateTime = updated.timeInMillis
                            },
                            cal.get(java.util.Calendar.YEAR),
                            cal.get(java.util.Calendar.MONTH),
                            cal.get(java.util.Calendar.DAY_OF_MONTH)
                        ).show()
                    }.padding(12.dp), contentAlignment = Alignment.Center) {
                        Text("Vybrat datum", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Color(0xFF0EA5E9)).clickable {
                        val cal = java.util.Calendar.getInstance().apply { timeInMillis = dateTime }
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                val updated = java.util.Calendar.getInstance().apply { timeInMillis = dateTime }
                                updated.set(java.util.Calendar.HOUR_OF_DAY, hour)
                                updated.set(java.util.Calendar.MINUTE, minute)
                                dateTime = updated.timeInMillis
                            },
                            cal.get(java.util.Calendar.HOUR_OF_DAY),
                            cal.get(java.util.Calendar.MINUTE),
                            true
                        ).show()
                    }.padding(12.dp), contentAlignment = Alignment.Center) {
                        Text("Vybrat čas", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Color(0xFF3B82F6)).clickable { onConfirm(dateTime, title) }.padding(12.dp), contentAlignment=Alignment.Center) { Text("Odeslat žádost", color=Color.White, fontWeight=FontWeight.Bold) }
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Color(0xFF334155)).clickable(onClick=onDismiss).padding(12.dp), contentAlignment=Alignment.Center) { Text("Zrušit", color=Color.White) }
                }
            }
        }
    }
}

@Composable
fun ChatInfoDialog(users: List<UserEntity>, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ℹ️ Info o chatu", color=Color.White, fontWeight=FontWeight.Bold, fontSize=16.sp)
                Text("Barva chatu: Modrá #3B82F6 - notifikace, aktivity, vše modře", color=Color(0xFF94A3B8), fontSize=12.sp)
                Spacer(modifier=Modifier.height(4.dp))
                users.forEach { u -> Text("${u.avatarEmoji} ${u.nickname} - ${if(u.isOnline) "Online 🟢" else "Offline"}", color=Color.White, fontSize=13.sp) }
                Spacer(modifier=Modifier.height(8.dp))
                Text("Nahrané soubory: (příště)", color=Color(0xFF64748B), fontSize=11.sp)
            }
        }
    }
}

@Composable
fun BubbleSettingsDialog(activeUser: UserEntity?, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var selectedColor by remember { mutableStateOf(activeUser?.chatBubbleColorHex?: "#1D4ED8") }
    var selectedShape by remember { mutableStateOf(activeUser?.chatBubbleShape?: "ROUNDED") }
    val colors = listOf("#1D4ED8" to "Modrá", "#64748B" to "Šedá", "#000000" to "Černá", "#22C55E" to "Zelená", "#EF4444" to "Červená", "#7E22CE" to "Fialová", "#FFFFFF" to "Bílá")
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Barva a tvar bubliny", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))
                colors.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier=Modifier.padding(bottom=8.dp)) {
                        row.forEach { (hex, name) ->
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(try{Color(android.graphics.Color.parseColor(hex))}catch(_:Exception){Color.Gray}).border(2.dp, if(selectedColor==hex) Color.White else Color.Transparent, CircleShape).clickable { selectedColor = hex })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if(selectedShape=="ROUNDED") Color(0xFF3B82F6) else Color(0xFF334155)).clickable{selectedShape="ROUNDED"}.padding(10.dp), contentAlignment=Alignment.Center){ Text("Zaoblené", color=Color.White, fontSize=12.sp) }
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if(selectedShape=="SQUARE") Color(0xFF3B82F6) else Color(0xFF334155)).clickable{selectedShape="SQUARE"}.padding(10.dp), contentAlignment=Alignment.Center){ Text("Hranaté", color=Color.White, fontSize=12.sp) }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Color(0xFF3B82F6)).clickable{onSave(selectedColor, selectedShape)}.padding(12.dp), contentAlignment=Alignment.Center){ Text("Uložit", color=Color.White, fontWeight=FontWeight.Bold) }
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Color(0xFF334155)).clickable(onClick=onDismiss).padding(12.dp), contentAlignment=Alignment.Center){ Text("Zrušit", color=Color.White) }
                }
            }
        }
    }
    }

private fun resolveDisplayName(context: android.content.Context, uri: Uri): String {
    return try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else "soubor"
        } ?: "soubor"
    } catch (_: Exception) {
        "soubor"
    }
}

private fun saveBitmapToCache(context: android.content.Context, bitmap: Bitmap): Uri? {
    return try {
        val file = File(context.cacheDir, "chat_photo_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        Uri.fromFile(file)
    } catch (_: Exception) {
        null
    }
}

private fun copyUriToCache(context: android.content.Context, source: Uri): Uri? {
    return try {
        val ext = when (context.contentResolver.getType(source)) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        val outFile = File(context.cacheDir, "chat_image_${System.currentTimeMillis()}.$ext")
        context.contentResolver.openInputStream(source)?.use { input ->
            FileOutputStream(outFile).use { output ->
                input.copyTo(output)
            }
        } ?: return null
        Uri.fromFile(outFile)
    } catch (_: Exception) {
        null
    }
}

private fun parseAttachedFileLabel(content: String): String? {
    val marker = "📎 Soubor:"
    val idx = content.indexOf(marker)
    if (idx == -1) return null
    return content.substring(idx + marker.length).lineSequence().firstOrNull()?.trim()?.ifBlank { "Soubor" }
}

private fun toAttachmentModel(uriString: String?): Any? {
    if (uriString.isNullOrBlank()) return null
    if (uriString.startsWith("data:", true)) return uriString
    val parsed = Uri.parse(uriString)
    return if (parsed.scheme.equals("file", true) && !parsed.path.isNullOrBlank()) {
        File(parsed.path!!)
    } else {
        parsed
    }
}

private fun openUploadedAttachment(context: android.content.Context, item: UploadedAttachmentItem): Boolean {
    return try {
        if (item.uri.isNullOrBlank()) return false
        val targetUri = if (item.uri.startsWith("data:", true)) {
            materializeDataUriToFile(context, item.uri, item.label, item.mimeType ?: if (item.isImage) "image/*" else "*/*")
        } else {
            Uri.parse(item.uri)
        } ?: return false

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(targetUri, item.mimeType ?: if (item.isImage) "image/*" else "*/*")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: Exception) {
        false
    }
}

private fun materializeDataUriToFile(context: android.content.Context, dataUri: String, fileNameHint: String, mimeHint: String): Uri? {
    return try {
        val commaIdx = dataUri.indexOf(',')
        if (commaIdx == -1) return null
        val meta = dataUri.substring(0, commaIdx)
        val mime = meta.substringAfter("data:").substringBefore(';').ifBlank { mimeHint }
        val bytes = Base64.decode(dataUri.substring(commaIdx + 1), Base64.DEFAULT)
        val ext = extensionFromMime(mime, fileNameHint)
        val safeName = fileNameHint.substringBeforeLast('.').ifBlank { "chat_attachment" }
        val file = File(context.cacheDir, "${safeName}_${System.currentTimeMillis()}.$ext")
        FileOutputStream(file).use { it.write(bytes) }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } catch (_: Exception) {
        null
    }
}

private fun downloadAttachment(
    context: android.content.Context,
    uriString: String,
    fileNameHint: String,
    mimeType: String?,
    isImage: Boolean
): Boolean {
    return try {
        val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
        if (!downloadsDir.exists()) downloadsDir.mkdirs()
        val baseName = fileNameHint.substringBeforeLast('.').ifBlank { if (isImage) "photo" else "file" }
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
        val ext = extensionFromMime(mimeType ?: "", fileNameHint)
        val targetFile = File(downloadsDir, "${baseName}_${System.currentTimeMillis()}.$ext")

        when {
            uriString.startsWith("data:", true) -> {
                val commaIdx = uriString.indexOf(',')
                if (commaIdx == -1) return false
                val decoded = Base64.decode(uriString.substring(commaIdx + 1), Base64.DEFAULT)
                FileOutputStream(targetFile).use { it.write(decoded) }
            }
            uriString.startsWith("http://", true) || uriString.startsWith("https://", true) -> {
                URL(uriString).openStream().use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            else -> {
                val parsed = Uri.parse(uriString)
                if (parsed.scheme.equals("file", true) && !parsed.path.isNullOrBlank()) {
                    File(parsed.path!!).inputStream().use { input ->
                        FileOutputStream(targetFile).use { output -> input.copyTo(output) }
                    }
                } else {
                    context.contentResolver.openInputStream(parsed)?.use { input ->
                        FileOutputStream(targetFile).use { output -> input.copyTo(output) }
                    } ?: return false
                }
            }
        }
        true
    } catch (_: Exception) {
        false
    }
}

private fun extensionFromMime(mime: String, fileNameHint: String): String {
    val nameExt = fileNameHint.substringAfterLast('.', "")
    if (nameExt.isNotBlank()) return nameExt
    return when {
        mime.contains("pdf") -> "pdf"
        mime.contains("png") -> "png"
        mime.contains("jpeg") || mime.contains("jpg") -> "jpg"
        mime.contains("webp") -> "webp"
        mime.contains("plain") -> "txt"
        else -> "bin"
    }
}

private fun guessMimeFromName(fileName: String): String {
    return when (fileName.substringAfterLast('.', "").lowercase()) {
        "pdf" -> "application/pdf"
        "txt" -> "text/plain"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "webp" -> "image/webp"
        else -> "application/octet-stream"
    }
}

private fun parseHexColor(hex: String?, fallback: Color): Color {
    return try {
        if (hex.isNullOrBlank()) fallback else Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        fallback
    }
}

