package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val defaultName: String,
    val nickname: String,
    val role: String,
    val pin: String,
    val email: String = "",
    val avatarEmoji: String = "👤",
    val borderHexColor: String = "#3B82F6",
    val isOnline: Boolean = true,
    val lastSeenTimestamp: Long = 0L,
    val lastChatReadTimestamp: Long = 0L,
    val lastTaskReadTimestamp: Long = 0L,
    val lastIdeaReadTimestamp: Long = 0L,
    val lastCalendarReadTimestamp: Long = 0L,
    val lastActivityReadTimestamp: Long = 0L,
    // NOVÉ PRO CHAT BUBLINY
    val chatBubbleColorHex: String = "#1D4ED8",
    val chatBubbleShape: String = "ROUNDED" // ROUNDED nebo SQUARE
)
@Entity(tableName = "activities")
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String, // "TASK", "NOTE", "IDEA", "CHAT", "CALENDAR"
    val title: String,
    val description: String,
    val authorId: String,
    val authorName: String,
    val isReadByActiveUser: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val likedByIds: String = "",
    val priority: String = "NONE"
)

@Entity(tableName = "tasks_notes")
data class TaskNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "TASK" or "NOTE"
    val title: String,
    val content: String,
    val colorHex: String = "#FF9500",
    val isBold: Boolean = false,
    val isUnderline: Boolean = false,
    val textColorHex: String = "#FFFFFF",
    val fontSizeSp: Int = 16,
    val isCompleted: Boolean = false,
    val reminderTimestamp: Long? = null,
    val authorId: String,
    val authorName: String,
    val editCount: Int = 0,
    val deletionRequestsJson: String = "[]",
    val priority: String = "NONE", // NONE, LOW, MEDIUM, HIGH - 4 úrovně
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "task_note_comments")
data class TaskNoteCommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskNoteId: Long,
    val authorId: String,
    val authorName: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "ideas")
data class IdeaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val stage: String = "LIST",
    val starsJson: String = "[]",
    val potentialsJson: String = "{}",
    val crownsJson: String = "[]",
    val infoListJson: String = "[]",
    val attachmentsJson: String = "[]",
    val proposalName: String = "",
    val proposalDesc: String = "",
    val proposalImagesJson: String = "[]",
    val approvalStatus: String = "NONE",
    val approvalsJson: String = "[]",
    val rejectionsJson: String = "{}",
    val approvalNotesJson: String = "{}",
    val priority: String = "NONE",
    val authorId: String,
    val authorName: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "project_comments")
data class ProjectCommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ideaId: Long,
    val authorId: String,
    val authorName: String,
    val text: String,
    val imageUri: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val senderId: String,
    val senderName: String,
    val recipientId: String = "GROUP",
    val content: String,
    val imageUri: String? = null,
    val attachmentDataUri: String? = null,
    val attachmentName: String? = null,
    val attachmentMimeType: String? = null,
    val priority: String = "NONE", // NONE=bez, LOW=1dp žlutá, MEDIUM=2dp oranžová, HIGH=3dp červená
    val reactionEmoji: String? = null,
    val readByJson: String = "[]",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val colorCategoryHex: String = "#FF9500",
    val startDateTimestamp: Long,
    val endDateTimestamp: Long,
    val isAllDay: Boolean = false,
    val location: String = "",
    val isRecurring: Boolean = false,
    val recurrenceRule: String = "Žádné",
    val reminderMinutes: Int = 15,
    val priority: String = "NONE", // NONE, LOW, MEDIUM, HIGH
    val authorId: String,
    val authorName: String,
    val editCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "calendar_comments")
data class CalendarCommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: Long,
    val authorId: String,
    val authorName: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
