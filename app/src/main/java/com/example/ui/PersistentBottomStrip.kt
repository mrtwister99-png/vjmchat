package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ActivityEntity
import com.example.data.UserEntity

@Composable
fun PersistentBottomStrip(
    users: List<UserEntity>,
    activeUser: UserEntity?,
    activities: List<ActivityEntity>,
    onOpenActivity: (ActivityEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser = activeUser ?: return
    val otherUsers = remember(users, currentUser.id) {
        users.filter { it.id != currentUser.id && (it.id == "kamaradka" || it.id == "tata") }
    }
    val unreadActivities = remember(activities, currentUser.id, currentUser.lastActivityReadTimestamp) {
        activities
            .filter { it.timestamp > currentUser.lastActivityReadTimestamp && it.authorId != currentUser.id }
            .sortedByDescending { it.timestamp }
            .take(10)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1D4ED8)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1D4ED8))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color(0xFF22C55E))
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    otherUsers.forEach { user ->
                        val statusColor = if (user.isOnline) Color(0xFF22C55E) else Color(0xFFEF4444)
                        val avatarBorder = if (user.isOnline) Color(0xFF22C55E) else Color(0xFFEF4444)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0F172A))
                                .border(1.5.dp, avatarBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(user.avatarEmoji, fontSize = 14.sp)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                                    .border(1.5.dp, Color(0xFF0F172A), CircleShape)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(26.dp)
                            .background(Color(0xFFDC2626))
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(26.dp)
                            .background(Color.White.copy(alpha = 0.85f))
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (unreadActivities.isEmpty()) {
                        Text("Bez nových aktivit", color = Color(0xFFE2E8F0), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    } else {
                        unreadActivities.forEach { activity ->
                            val activityColor = activityCircleColor(activity)
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(activityColor)
                                    .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                                    .clickable { onOpenActivity(activity) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = activityMarker(activity),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun activityCircleColor(activity: ActivityEntity): Color = when (activity.category.uppercase()) {
    "CHAT" -> Color(0xFF3B82F6)
    "IDEA" -> Color(0xFFEAB308)
    "TASK" -> Color(0xFFF97316)
    "NOTE" -> Color(0xFFFB923C)
    "CALENDAR" -> Color(0xFF22C55E)
    "COMMENT" -> Color(0xFF8B5CF6)
    else -> Color(0xFF64748B)
}

private fun activityMarker(activity: ActivityEntity): String = when (activity.category.uppercase()) {
    "CHAT" -> "C"
    "IDEA" -> "I"
    "TASK" -> "T"
    "NOTE" -> "N"
    "CALENDAR" -> "K"
    "COMMENT" -> "•"
    else -> "•"
}