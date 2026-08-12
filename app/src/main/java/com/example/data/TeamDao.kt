package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamDao {
    // Users
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users")
    suspend fun getAllUsersList(): List<UserEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    // UKOL 3: pouze 20 poslednich
    @Query("SELECT * FROM activities ORDER BY timestamp DESC LIMIT 20")
    fun getRecentActivities(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities ORDER BY timestamp ASC")
    suspend fun getAllActivitiesList(): List<ActivityEntity>

    @Query("SELECT * FROM activities WHERE id = :id")
    suspend fun getActivityById(id: Long): ActivityEntity?

    @Query("SELECT * FROM chat_messages WHERE recipientId = 'GROUP' ORDER BY timestamp ASC")
    suspend fun getAllChatMessagesList(): List<ChatMessageEntity>

    @Query("DELETE FROM activities WHERE id = :id")
    suspend fun deleteActivityById(id: Long)

    @Query("DELETE FROM activities")
    suspend fun deleteAllActivities()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityEntity)

    @Update
    suspend fun updateActivity(activity: ActivityEntity)

    // Tasks & Notes
    @Query("SELECT * FROM tasks_notes ORDER BY createdAt DESC")
    fun getAllTasksAndNotes(): Flow<List<TaskNoteEntity>>

    @Query("SELECT * FROM tasks_notes WHERE id = :id")
    suspend fun getTaskNoteById(id: Long): TaskNoteEntity?

    @Query("SELECT * FROM tasks_notes")
    suspend fun getAllTasksAndNotesList(): List<TaskNoteEntity>

    @Query("SELECT * FROM tasks_notes ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLastTaskNote(): TaskNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskNote(item: TaskNoteEntity)

    @Update
    suspend fun updateTaskNote(item: TaskNoteEntity)

    @Query("DELETE FROM tasks_notes WHERE id = :id")
    suspend fun deleteTaskNote(id: Long)

    @Query("DELETE FROM tasks_notes")
    suspend fun deleteAllTaskNotes()

    // Task & Note Comments
    @Query("SELECT * FROM task_note_comments WHERE taskNoteId = :taskNoteId ORDER BY timestamp ASC")
    fun getCommentsForTaskNote(taskNoteId: Long): Flow<List<TaskNoteCommentEntity>>

    @Query("SELECT * FROM task_note_comments ORDER BY timestamp ASC")
    suspend fun getAllTaskNoteCommentsList(): List<TaskNoteCommentEntity>

    @Query("SELECT * FROM task_note_comments WHERE id = :id")
    suspend fun getTaskNoteCommentById(id: Long): TaskNoteCommentEntity?

    @Query("SELECT * FROM task_note_comments ORDER BY id DESC LIMIT 1")
    suspend fun getLastTaskNoteComment(): TaskNoteCommentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskNoteComment(comment: TaskNoteCommentEntity)

    // Ideas & Projects
    @Query("SELECT * FROM ideas ORDER BY createdAt DESC")
    fun getAllIdeas(): Flow<List<IdeaEntity>>

    @Query("SELECT * FROM ideas WHERE id = :id")
    suspend fun getIdeaById(id: Long): IdeaEntity?

    @Query("SELECT * FROM ideas")
    suspend fun getAllIdeasList(): List<IdeaEntity>

    @Query("SELECT * FROM ideas ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLastIdea(): IdeaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIdea(idea: IdeaEntity)

    @Update
    suspend fun updateIdea(idea: IdeaEntity)

    @Query("DELETE FROM ideas WHERE id = :id")
    suspend fun deleteIdea(id: Long)

    @Query("DELETE FROM ideas")
    suspend fun deleteAllIdeas()

    // Project Comments
    @Query("SELECT * FROM project_comments WHERE ideaId = :ideaId ORDER BY timestamp ASC")
    fun getCommentsForIdea(ideaId: Long): Flow<List<ProjectCommentEntity>>

    @Query("SELECT * FROM project_comments ORDER BY timestamp ASC")
    suspend fun getAllProjectCommentsList(): List<ProjectCommentEntity>

    @Query("SELECT * FROM project_comments WHERE id = :id")
    suspend fun getProjectCommentById(id: Long): ProjectCommentEntity?

    @Query("SELECT * FROM project_comments ORDER BY id DESC LIMIT 1")
    suspend fun getLastProjectComment(): ProjectCommentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjectComment(comment: ProjectCommentEntity)

    // Chat Messages
    @Query("SELECT * FROM chat_messages WHERE recipientId = 'GROUP' ORDER BY timestamp ASC")
    fun getAllChatMessages(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE recipientId = 'SECRET' ORDER BY timestamp ASC")
    fun getSecretChatMessages(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE recipientId = 'SECRET' ORDER BY timestamp ASC")
    suspend fun getAllSecretChatMessagesList(): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageEntity)

    @Update
    suspend fun updateChatMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE recipientId = 'GROUP'")
    suspend fun deleteAllChatMessages()

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAllChatMessagesIncludingSecret()

    // Calendar Events
    @Query("SELECT * FROM calendar_events ORDER BY startDateTimestamp ASC")
    fun getAllCalendarEvents(): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events WHERE id = :id")
    suspend fun getCalendarEventById(id: Long): CalendarEventEntity?

    @Query("SELECT * FROM calendar_events")
    suspend fun getAllCalendarEventsList(): List<CalendarEventEntity>

    @Query("SELECT * FROM calendar_events ORDER BY startDateTimestamp DESC LIMIT 1")
    suspend fun getLastCalendarEvent(): CalendarEventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalendarEvent(event: CalendarEventEntity)

    @Update
    suspend fun updateCalendarEvent(event: CalendarEventEntity)

    @Query("DELETE FROM calendar_events WHERE id = :id")
    suspend fun deleteCalendarEvent(id: Long)

    @Query("DELETE FROM calendar_events")
    suspend fun deleteAllCalendarEvents()

    // Calendar Comments
    @Query("SELECT * FROM calendar_comments WHERE eventId = :eventId ORDER BY timestamp ASC")
    fun getCommentsForCalendarEvent(eventId: Long): Flow<List<CalendarCommentEntity>>

    @Query("SELECT * FROM calendar_comments ORDER BY timestamp ASC")
    suspend fun getAllCalendarCommentsList(): List<CalendarCommentEntity>

    @Query("SELECT * FROM calendar_comments WHERE id = :id")
    suspend fun getCalendarCommentById(id: Long): CalendarCommentEntity?

    @Query("SELECT * FROM calendar_comments ORDER BY id DESC LIMIT 1")
    suspend fun getLastCalendarComment(): CalendarCommentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalendarComment(comment: CalendarCommentEntity)
}
