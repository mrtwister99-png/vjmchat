package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserEntity

@Composable
fun NotificationBanner(
    notification: ActiveNotification?,
    users: List<UserEntity> = emptyList(),
    onDismiss: () -> Unit,
    onNavigateToCategory: (String) -> Unit,
    onQuickReply: (String) -> Unit
) {
    AnimatedVisibility(
        visible = notification != null,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        if (notification != null) {
            var isReplying by remember(notification.id) { mutableStateOf(false) }
            var replyText by remember(notification.id) { mutableStateOf("") }
            val focusRequester = remember(notification.id) { FocusRequester() }
            val keyboardController = LocalSoftwareKeyboardController.current

            val senderUser = users.find { it.id == notification.senderId || it.nickname == notification.senderName }

            val priorityColor = when (notification.priority) {
                "HIGH" -> Color(0xFFEF4444)
                "MEDIUM" -> Color(0xFFF97316)
                "LOW" -> Color(0xFFEAB308)
                else -> Color.Transparent
            }
            val borderWidth = when (notification.priority) {
                "HIGH" -> 3.dp
                "MEDIUM" -> 2.dp
                "LOW" -> 1.dp
                else -> 0.dp
            }
            val accentColor = if (priorityColor == Color.Transparent) Color(0xFF64748B) else priorityColor
            val priorityMark = priorityPrefix(notification.priority)

            // pozadí: komentář = fialová #8B5CF6, secret = stejná jako chat, high = tmavě červená, jinak tmavě modrá
            val isComment = notification.title.contains("Komentář", true)
            val isSecret = notification.category == "SECRET" || notification.category == "SECRET_CHAT"
            val bannerBg = when {
                isComment -> Color(0xFF8B5CF6)
                isSecret -> Color(0xFF0F172A)
                notification.priority == "HIGH" || notification.isWarningAlert -> Color(0xFF450A0A)
                else -> Color(0xFF0F172A)
            }

            // auto-dismiss po 10s (HIGH 12s) - běžně 4-5s LOW, 6-8s MEDIUM, 10-15s HIGH
            androidx.compose.runtime.LaunchedEffect(notification.id) {
                val duration = when (notification.priority) {
                    "HIGH" -> 12000L
                    "MEDIUM" -> 10000L
                    "LOW" -> 8000L
                    else -> 10000L
                }
                kotlinx.coroutines.delay(duration)
                onDismiss()
            }

            androidx.compose.runtime.LaunchedEffect(isReplying, notification.id) {
                if (isReplying) {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .then(
                        if (borderWidth > 0.dp) {
                            Modifier.border(borderWidth, priorityColor, RoundedCornerShape(16.dp))
                        } else {
                            Modifier
                        }
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = bannerBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        // IKONKA toho kdo psal
                        if (senderUser != null) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E293B))
                                    .border(2.dp, try { Color(android.graphics.Color.parseColor(senderUser.borderHexColor)) } catch (e: Exception) { priorityColor }, CircleShape)
                            ) {
                                Text(senderUser.avatarEmoji, fontSize = 18.sp)
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(accentColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(notification.senderName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "$priorityMark ${notification.title} • od ${notification.senderName}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Text(text = "$priorityMark ${notification.message}", color = Color(0xFFE2E8F0), fontSize = 13.sp, maxLines = 2)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Zavřít", tint = Color.White)
                        }
                    }

                    if (isReplying) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = replyText,
                                onValueChange = { replyText = it },
                                placeholder = { Text("Rychlá odpověď...", color = Color.Gray, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color(0xFF0F172A), unfocusedContainerColor = Color(0xFF0F172A))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(onClick = {
                                if (replyText.isNotBlank()) {
                                    onQuickReply(replyText)
                                    replyText = ""
                                    isReplying = false
                                    onDismiss()
                                }
                            }) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Odeslat", tint = accentColor) }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.align(Alignment.End)) {
                            if (notification.category == "CHAT") {
                                TextButton(onClick = { isReplying = true }) { Text("Rychlá odpověď", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Button(onClick = { onNavigateToCategory(notification.category); onDismiss() }, colors = ButtonDefaults.buttonColors(containerColor = accentColor)) {
                                Text("Zobrazit", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}