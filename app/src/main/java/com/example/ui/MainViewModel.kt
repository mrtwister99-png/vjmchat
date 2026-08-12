package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ActivityEntity
import com.example.data.CalendarCommentEntity
import com.example.data.CalendarEventEntity
import com.example.data.ChatMessageEntity
import com.example.data.DataStoreManager
import com.example.data.IdeaEntity
import com.example.data.ProjectCommentEntity
import com.example.data.TaskNoteCommentEntity
import com.example.data.TaskNoteEntity
import com.example.data.TeamDatabase
import com.example.data.TeamRepository
import com.example.data.UserEntity
import com.example.data.SoundManager
import com.example.data.remote.ApiConfig
import com.example.data.remote.ApiService
import com.example.data.remote.MessageReactionUpdateDto
import com.example.data.remote.VjmIdeaDto
import com.example.data.remote.VjmMessageDto
import com.example.data.remote.VjmTaskDto
import com.example.data.remote.VjmCalendarDto
import com.example.data.remote.VjmUserDto
import com.example.data.remote.VjmSecretMessageDto
import com.example.data.remote.WebSocketManager
import com.example.notification.SystemNotificationHelper
import com.example.R
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import org.json.JSONObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

data class ActiveNotification(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val senderId: String,
    val senderName: String,
    val message: String,
    val category: String,
    val colorHex: String = "#3B82F6",
    val isWarningAlert: Boolean = false,
    val priority: String = "NONE"
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TeamRepository
    private val dataStoreManager = DataStoreManager(application)
    private val apiService: ApiService
    private val wsManager = WebSocketManager()

    val users: StateFlow<List<UserEntity>>
    val activities: StateFlow<List<ActivityEntity>>
    val tasksAndNotes: StateFlow<List<TaskNoteEntity>>
    val ideas: StateFlow<List<IdeaEntity>>
    val chatMessages: StateFlow<List<ChatMessageEntity>>
    val secretChatMessages: StateFlow<List<ChatMessageEntity>>
    val calendarEvents: StateFlow<List<CalendarEventEntity>>



    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _activeUserId = MutableStateFlow("admin")
    val activeUserId: StateFlow<String> = _activeUserId.asStateFlow()

    val activeUser: StateFlow<UserEntity?>

    private val _unreadChatCount = MutableStateFlow(0)
    val unreadChatCount: StateFlow<Int> = _unreadChatCount.asStateFlow()

    private val _unreadSecretCount = MutableStateFlow(0)
    val unreadSecretCount: StateFlow<Int> = _unreadSecretCount.asStateFlow()

    private val _unreadTaskCount = MutableStateFlow(0)
    val unreadTaskCount: StateFlow<Int> = _unreadTaskCount.asStateFlow()
    private val _unreadIdeaCount = MutableStateFlow(0)
    val unreadIdeaCount: StateFlow<Int> = _unreadIdeaCount.asStateFlow()
    private val _unreadCalendarCount = MutableStateFlow(0)
    val unreadCalendarCount: StateFlow<Int> = _unreadCalendarCount.asStateFlow()

    private val _lastLogoutTime = MutableStateFlow(0L)
    private val _lastTaskRead = MutableStateFlow(0L)
    private val _lastIdeaRead = MutableStateFlow(0L)
    private val _lastCalendarRead = MutableStateFlow(0L)

    private val _notification = MutableStateFlow<ActiveNotification?>(null)
    val notification: StateFlow<ActiveNotification?> = _notification.asStateFlow()

    // WIDGET - pending tituly z widgetu
    private val _pendingWidgetTask = MutableStateFlow<Pair<String, String>?>(null) // title to type TASK/NOTE
    val pendingWidgetTask: StateFlow<Pair<String, String>?> = _pendingWidgetTask.asStateFlow()

    private val _pendingWidgetIdea = MutableStateFlow<String?>(null)
    val pendingWidgetIdea: StateFlow<String?> = _pendingWidgetIdea.asStateFlow()

    private val _pendingWidgetCalendar = MutableStateFlow<String?>(null)
    val pendingWidgetCalendar: StateFlow<String?> = _pendingWidgetCalendar.asStateFlow()

    private val _pendingWidgetChat = MutableStateFlow<String?>(null)
    val pendingWidgetChat: StateFlow<String?> = _pendingWidgetChat.asStateFlow()

    private var appInForeground: Boolean = true
    private var currentScreenRoute: String = "home"
    private val lastSystemNotificationAt = mutableMapOf<String, Long>()
    private val lastSoundAt = mutableMapOf<String, Long>()

    init {
        val database = TeamDatabase.getDatabase(application)
        repository = TeamRepository(database.teamDao())
        // --- RAILWAY REMOTE ---
        val retrofit = Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)
        wsManager.connect()

        // LIVE - WebSocket pro chat
        viewModelScope.launch {
            wsManager.messages.collect { json ->
                try {
                    val adapter = Moshi.Builder().build().adapter(VjmMessageDto::class.java)
                        val dto = adapter.fromJson(json)
                    if (dto != null) {
                            val inserted = repository.insertRemoteChatMessage(dto.sender, dto.text, dto.imageUri, dto.attachmentDataUri, dto.attachmentName, dto.attachmentMimeType, dto.reactionEmoji, dto.timestamp)
                        if (inserted) {
                            // notifikace live když nejsem autor
                            val me = _activeUserId.value
                            if (dto.sender != users.value.find { it.id == me }?.nickname) {
                                triggerNotificationForOthers(dto.sender, "Nová zpráva v chatu", dto.sender, dto.text, "CHAT", "#22C55E", false, "NONE")
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }
        // LIVE POLLING pro tasks/ideas/calendar + initial sync
        viewModelScope.launch {
            while (true) {
                try {
                    var hasWidgetChanges = false

                    val remoteMessages = apiService.getMessages()
                    remoteMessages.forEach {
                        if (repository.insertRemoteChatMessage(it.sender, it.text, it.imageUri, it.attachmentDataUri, it.attachmentName, it.attachmentMimeType, it.reactionEmoji, it.timestamp)) {
                            hasWidgetChanges = true
                        }
                    }

                    val remoteTasks = apiService.getTasks()
                    remoteTasks.forEach {
                        if (repository.insertRemoteTask(it.id, it.type, it.title, it.content, it.colorHex, it.authorId, it.authorName, it.priority, it.isCompleted, it.createdAt)) {
                            hasWidgetChanges = true
                        }
                    }
                    if (repository.reconcileTasks(remoteTasks.map { it.id }.toSet())) {
                        hasWidgetChanges = true
                    }

                    val remoteIdeas = apiService.getIdeas()
                    remoteIdeas.forEach {
                        if (repository.insertRemoteIdea(it.id, it.title, it.description, it.stage, it.authorId, it.authorName, it.priority, it.starsJson, it.crownsJson, it.potentialsJson, it.createdAt)) {
                            hasWidgetChanges = true
                        }
                    }
                    if (repository.reconcileIdeas(remoteIdeas.map { it.id }.toSet())) {
                        hasWidgetChanges = true
                    }

                    val remoteCal = apiService.getCalendar()
                    remoteCal.forEach {
                        if (repository.insertRemoteCalendar(it.id, it.title, it.description, it.colorCategoryHex, it.startDateTimestamp, it.endDateTimestamp, it.authorId, it.authorName, it.priority, it.createdAt)) {
                            hasWidgetChanges = true
                        }
                    }
                    if (repository.reconcileCalendar(remoteCal.map { it.id }.toSet())) {
                        hasWidgetChanges = true
                    }

                    val remoteUsers = apiService.getUsers()
                    remoteUsers.forEach { dto ->
                        val local = repository.getUserById(dto.id)
                        if (local != null) {
                            val updated = local.copy(
                                nickname = dto.nickname.ifBlank { local.nickname },
                                defaultName = dto.defaultName.ifBlank { local.defaultName },
                                isOnline = dto.isOnline,
                                lastSeenTimestamp = if (dto.isOnline) local.lastSeenTimestamp else System.currentTimeMillis()
                            )
                            if (updated != local) repository.updateUserFull(updated)
                        }
                    }
                    // KOMENTÁŘE - VARIANTA 2 - LIVE PRO VŠECHNY
                    try {
                        val remoteCalComments = apiService.getCalendarComments()
                        remoteCalComments.forEach { if (repository.insertRemoteCalendarComment(it)) hasWidgetChanges = true }
                        val remoteTaskComments = apiService.getTaskComments()
                        remoteTaskComments.forEach { if (repository.insertRemoteTaskComment(it)) hasWidgetChanges = true }
                        val remoteProjectComments = apiService.getProjectComments()
                        remoteProjectComments.forEach { if (repository.insertRemoteProjectComment(it)) hasWidgetChanges = true }
                    } catch (_: Exception) {}
                    // SECRET CHAT LIVE SYNC - jen Tom a Adélka
                    try {
                        val me = activeUser.value
                        if (me != null && me.id != "tata" && !me.defaultName.contains("Michal", true)) {
                            val remoteSecret = apiService.getSecretMessages()
                            remoteSecret.forEach {
                                val inserted = repository.insertRemoteSecretMessage(it)
                                if (inserted && it.senderId != me.id) {
                                    triggerNotificationForOthers(it.senderId, "🔒 Secret chat", it.senderName, it.text, "SECRET", "#22C55E", false, "MEDIUM")
                                }
                            }
                        }
                    } catch (_: Exception) {}
                    // LIVE LIKE SYNC - obrysy +1/+2/+3 pro všechny 3 uživatele
                    try {
                        val remoteLikes = apiService.getActivityLikes()
                        remoteLikes.forEach { if (repository.insertRemoteActivityLike(it.id, it.likedByIds)) hasWidgetChanges = true }
                    } catch (_: Exception) {
                        try {
                            val remoteActivities = apiService.getActivities()
                            repository.insertRemoteActivities(remoteActivities)
                            hasWidgetChanges = true
                        } catch (_: Exception) {}
                    }

                    if (hasWidgetChanges) {
                        try { com.example.widget.VJMWidgetProvider.updateAllWidgets(getApplication()) } catch (_: Exception) {}
                    }
                } catch (_: Exception) {}
                kotlinx.coroutines.delay(1200) // agresivnější live sync napříč všemi screeny
            }
        }
        
        viewModelScope.launch {
            repository.initDefaultDataIfNeeded()
            val savedId = dataStoreManager.getActiveUserId()
            if (savedId != null && savedId.isNotBlank()) {
                _activeUserId.value = savedId
                _isLoggedIn.value = true
                repository.setUserOnline(savedId, true)
            }
            _lastLogoutTime.value = dataStoreManager.getLastLogoutTime()
            _lastTaskRead.value = dataStoreManager.getLastTaskRead()
            _lastIdeaRead.value = dataStoreManager.getLastIdeaRead()
            _lastCalendarRead.value = dataStoreManager.getLastCalendarRead()
        }

        users = repository.users.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        activities = repository.activities.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        tasksAndNotes = repository.tasksAndNotes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        ideas = repository.ideas.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        chatMessages = repository.chatMessages.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        secretChatMessages = repository.secretChatMessages.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        calendarEvents = repository.calendarEvents.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        activeUser = combine(users, _activeUserId) { userList, currentId ->
            userList.find { it.id == currentId }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        // LOGIKA +4 ZPRÁVY - opraveno: filtr i podle nickname (remote zprávy mají senderId = nickname)
        viewModelScope.launch {
            combine(chatMessages, activeUser, _lastLogoutTime) { msgs, user, lastLogout ->
                if (user == null) 0 else {
                    val lastRead = maxOf(user.lastChatReadTimestamp, lastLogout)
                    msgs.count { it.timestamp > lastRead && it.senderId != user.id && it.senderName != user.nickname }
                }
            }.collect { _unreadChatCount.value = it }
        }
        // LOGIKA SECRET - žlutý obrys pro Tom + Adélka - jen nové od posledního otevření
        viewModelScope.launch {
            combine(secretChatMessages, activeUser, _lastLogoutTime) { msgs, user, lastLogout ->
                if (user == null) 0 else {
                    if (user.id == "tata" || user.defaultName.contains("Michal", true)) 0
                    else {
                        val lastRead = maxOf(user.lastChatReadTimestamp, lastLogout)
                        msgs.count { it.timestamp > lastRead && it.senderId != user.id && it.senderName != user.nickname }
                    }
                }
            }.collect { _unreadSecretCount.value = it }
        }
        // INDIVIDUÁLNĚ PER-USER - Adélka 5+3, Tom 0 po rozkliknutí, Michal 8+3 - každý zvlášť
        viewModelScope.launch {
            combine(tasksAndNotes, activeUser, _lastLogoutTime, _lastTaskRead) { list, user, lastLogout, lastTaskRead ->
                if (user == null) 0 else {
                    val lastRead = maxOf(user.lastChatReadTimestamp, user.lastTaskReadTimestamp, lastLogout, lastTaskRead)
                    list.count { it.createdAt > lastRead && it.authorId != user.id }
                }
            }.collect { _unreadTaskCount.value = it }
        }
        viewModelScope.launch {
            combine(ideas, activeUser, _lastLogoutTime, _lastIdeaRead) { list, user, lastLogout, lastIdeaRead ->
                if (user == null) 0 else {
                    val lastRead = maxOf(user.lastChatReadTimestamp, user.lastIdeaReadTimestamp, lastLogout, lastIdeaRead)
                    list.count { it.createdAt > lastRead && it.authorId != user.id }
                }
            }.collect { _unreadIdeaCount.value = it }
        }
        viewModelScope.launch {
            combine(calendarEvents, activeUser, _lastLogoutTime, _lastCalendarRead) { list, user, lastLogout, lastCalRead ->
                if (user == null) 0 else {
                    val lastRead = maxOf(user.lastChatReadTimestamp, user.lastCalendarReadTimestamp, lastLogout, lastCalRead)
                    list.count { it.createdAt > lastRead && it.authorId != user.id }
                }
            }.collect { _unreadCalendarCount.value = it }
        }
        // HEARTBEAT online stavu aktivního uživatele
        viewModelScope.launch {
            while (true) {
                try {
                    val me = activeUser.value
                    if (_isLoggedIn.value && me != null) {
                        apiService.updateUserOnline(VjmUserDto(id = me.id, pin = me.pin, nickname = me.nickname, defaultName = me.defaultName, isOnline = true))
                    }
                } catch (_: Exception) {}
                kotlinx.coroutines.delay(15000)
            }
        }
    }

    fun loginUser(userId: String) {
        _activeUserId.value = userId
        _isLoggedIn.value = true
        viewModelScope.launch {
            dataStoreManager.saveActiveUserId(userId)
            repository.setUserOnline(userId, true)
            try {
                val u = repository.getUserById(userId)
                if (u != null) {
                    apiService.updateUserOnline(VjmUserDto(id = u.id, pin = u.pin, nickname = u.nickname, defaultName = u.defaultName, isOnline = true))
                }
            } catch (_: Exception) {}
        }
    }

    fun logout() {
        val currentUser = activeUser.value
        viewModelScope.launch {
            dataStoreManager.saveLogoutTime(System.currentTimeMillis())
            if (currentUser != null) {
                repository.setUserOnline(currentUser.id, false)
                repository.updateUserLastSeen(currentUser.id, System.currentTimeMillis())
                try {
                    apiService.updateUserOnline(VjmUserDto(id = currentUser.id, pin = currentUser.pin, nickname = currentUser.nickname, defaultName = currentUser.defaultName, isOnline = false))
                } catch (_: Exception) {}
            }
        }
        _isLoggedIn.value = false
    }

    fun switchActiveUser(userId: String) {
        _activeUserId.value = userId
        viewModelScope.launch {
            dataStoreManager.saveActiveUserId(userId)
            repository.setUserOnline(userId, true)
            try {
                val u = repository.getUserById(userId)
                if (u != null) {
                    apiService.updateUserOnline(VjmUserDto(id = u.id, pin = u.pin, nickname = u.nickname, defaultName = u.defaultName, isOnline = true))
                }
            } catch (_: Exception) {}
        }
    }

    fun setCurrentUserOnline(isOnline: Boolean) {
        val user = activeUser.value ?: return
        viewModelScope.launch {
            repository.setUserOnline(user.id, isOnline)
            if (!isOnline) repository.updateUserLastSeen(user.id, System.currentTimeMillis())
            try {
                apiService.updateUserOnline(VjmUserDto(id = user.id, pin = user.pin, nickname = user.nickname, defaultName = user.defaultName, isOnline = isOnline))
            } catch (_: Exception) {}
        }
    }

    fun onChatOpened() {
        val user = activeUser.value ?: return
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            repository.updateLastChatRead(user.id, now)
            repository.updateLastActivityRead(user.id, now)
            dataStoreManager.saveLastChatRead(now)
            _unreadChatCount.value = 0
        }
    }

    fun onSecretChatOpened() {
        val user = activeUser.value ?: return
        if (user.id == "tata" || user.defaultName.contains("Michal", true) || user.nickname.contains("Michal", true)) return
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            repository.updateLastChatRead(user.id, now)
            dataStoreManager.saveLastChatRead(now)
            _unreadSecretCount.value = 0
        }
    }

    fun onTasksOpened() {
        val user = activeUser.value ?: return
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            repository.updateLastTaskRead(user.id, now)
            repository.updateLastActivityRead(user.id, now)
            dataStoreManager.saveLastTaskRead(now)
            _lastTaskRead.value = now
            _unreadTaskCount.value = 0
        }
    }

    fun onIdeasOpened() {
        val user = activeUser.value ?: return
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            repository.updateLastIdeaRead(user.id, now)
            repository.updateLastActivityRead(user.id, now)
            dataStoreManager.saveLastIdeaRead(now)
            _lastIdeaRead.value = now
            _unreadIdeaCount.value = 0
        }
    }

    fun onCalendarOpened() {
        val user = activeUser.value ?: return
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            repository.updateLastCalendarRead(user.id, now)
            repository.updateLastActivityRead(user.id, now)
            dataStoreManager.saveLastCalendarRead(now)
            _lastCalendarRead.value = now
            _unreadCalendarCount.value = 0
        }
    }

    fun markActivityRead(activityTimestamp: Long) {
        val user = activeUser.value ?: return
        viewModelScope.launch {
            val current = user.lastActivityReadTimestamp
            if (activityTimestamp > current) {
                repository.updateLastActivityRead(user.id, activityTimestamp)
            }
        }
    }


    fun updateProfile(nickname: String, avatarEmoji: String, borderHexColor: String, email: String, pin: String, bubbleColor: String?, bubbleShape: String?) {
        val userId = _activeUserId.value
        viewModelScope.launch {
            repository.updateUserProfile(userId, nickname, avatarEmoji, borderHexColor, email, pin)
            if (bubbleColor != null || bubbleShape != null) {
                val user = repository.getUserById(userId)
                if (user != null) repository.updateUserFull(user.copy(chatBubbleColorHex = bubbleColor ?: user.chatBubbleColorHex, chatBubbleShape = bubbleShape ?: user.chatBubbleShape))
            }
            if (pin.isNotBlank()) { try { apiService.updateUserPin(VjmUserDto(id = userId, pin = pin, nickname = nickname)) } catch (_: Exception) {} }
        }
    }

    fun updateUserBubbleStyle(userId: String, bubbleColor: String, bubbleShape: String? = null) {
        viewModelScope.launch {
            val user = repository.getUserById(userId) ?: return@launch
            repository.updateUserFull(
                user.copy(
                    chatBubbleColorHex = bubbleColor,
                    chatBubbleShape = bubbleShape ?: user.chatBubbleShape
                )
            )
        }
    }

    fun toggleActivityReadStatus(activity: ActivityEntity) {
        viewModelScope.launch { repository.toggleActivityReadStatus(activity) }
    }
    fun toggleActivityLike(activity: ActivityEntity, userId: String) {
        viewModelScope.launch {
            repository.toggleActivityLike(activity, userId)
            try { com.example.widget.VJMWidgetProvider.updateAllWidgets(getApplication()) } catch (_: Exception) {}
            try {
                val updated = repository.getActivityById(activity.id)
                if (updated != null) {
                    apiService.sendActivityLike(com.example.data.remote.VjmActivityLikeDto(id = updated.id, likedByIds = updated.likedByIds))
                }
            } catch (_: Exception) {}
        }
    }

      fun addTaskOrNote(type: String, title: String, content: String, colorHex: String, isBold: Boolean, isUnderline: Boolean, textColorHex: String, fontSize: Int, reminderTimestamp: Long?, priority: String) {
        val currentUser = activeUser.value ?: return
        viewModelScope.launch {
            val item = TaskNoteEntity(type = type, title = title, content = content, colorHex = colorHex, isBold = isBold, isUnderline = isUnderline, textColorHex = textColorHex, fontSizeSp = fontSize, reminderTimestamp = reminderTimestamp, authorId = currentUser.id, authorName = currentUser.nickname, priority = priority)
            repository.addTaskOrNote(item)
            try { com.example.widget.VJMWidgetProvider.updateAllWidgets(getApplication()) } catch (_: Exception) {}
            try { val inserted = repository.getLastTaskNote(); if (inserted != null) apiService.sendTask(VjmTaskDto(id = inserted.id, type = type, title = title, content = content, colorHex = colorHex, authorId = currentUser.id, authorName = currentUser.nickname, priority = priority, isCompleted = false, createdAt = inserted.createdAt)) } catch (_: Exception) {}
            triggerNotificationForOthers(currentUser.id, if (type == "TASK") "Nový úkol" else "Nová poznámka", currentUser.nickname, title, type, "#3B82F6", priority = priority)
        }
    }
    fun updateTaskOrNote(item: TaskNoteEntity) {
        val currentUser = activeUser.value ?: return
        viewModelScope.launch {
            repository.updateTaskOrNote(item, currentUser.id)
            val updated = repository.getTaskNoteById(item.id) ?: return@launch
            try {
                apiService.sendTask(
                    VjmTaskDto(
                        id = updated.id,
                        type = updated.type,
                        title = updated.title,
                        content = updated.content,
                        colorHex = updated.colorHex,
                        authorId = updated.authorId,
                        authorName = updated.authorName,
                        priority = updated.priority,
                        isCompleted = updated.isCompleted,
                        createdAt = updated.createdAt
                    )
                )
            } catch (_: Exception) {}
        }
    }
    fun toggleTaskCompletion(item: TaskNoteEntity) {
        viewModelScope.launch {
            repository.toggleTaskCompletion(item)
            val updated = repository.getTaskNoteById(item.id) ?: return@launch
            try {
                apiService.sendTask(
                    VjmTaskDto(
                        id = updated.id,
                        type = updated.type,
                        title = updated.title,
                        content = updated.content,
                        colorHex = updated.colorHex,
                        authorId = updated.authorId,
                        authorName = updated.authorName,
                        priority = updated.priority,
                        isCompleted = updated.isCompleted,
                        createdAt = updated.createdAt
                    )
                )
            } catch (_: Exception) {}
        }
    }
    fun requestOrConfirmTaskNoteDeletion(id: Long) {
        val currentUser = activeUser.value ?: return
        viewModelScope.launch {
            repository.requestOrConfirmTaskNoteDeletion(id, currentUser.id)
            val remaining = repository.getTaskNoteById(id)
            if (remaining == null) {
                try { apiService.deleteTask(id) } catch (_: Exception) {}
            } else {
                try {
                    apiService.sendTask(
                        VjmTaskDto(
                            id = remaining.id,
                            type = remaining.type,
                            title = remaining.title,
                            content = remaining.content,
                            colorHex = remaining.colorHex,
                            authorId = remaining.authorId,
                            authorName = remaining.authorName,
                            priority = remaining.priority,
                            isCompleted = remaining.isCompleted,
                            createdAt = remaining.createdAt
                        )
                    )
                } catch (_: Exception) {}
            }
        }
    }
    fun deleteTaskNoteDirect(id: Long) {
        viewModelScope.launch {
            repository.deleteTaskNoteDirect(id)
            try { apiService.deleteTask(id) } catch (_: Exception) {}
        }
    }
    fun getCommentsForTaskNote(taskNoteId: Long) = repository.getCommentsForTaskNote(taskNoteId)
    fun addTaskNoteComment(taskNoteId: Long, text: String, context: android.content.Context? = null) {
        val currentUser = activeUser.value ?: return
        viewModelScope.launch {
            repository.addTaskNoteComment(taskNoteId, currentUser.id, currentUser.nickname, text)
            triggerNotificationForOthers(currentUser.id, "Komentář k úkolu/poznámce", currentUser.nickname, text, "TASK", "#8B5CF6")
            try {
                val last = repository.getLastTaskNoteComment()
                if (last != null) apiService.sendTaskComment(com.example.data.remote.VjmTaskCommentDto(id = last.id, taskNoteId = taskNoteId, authorId = currentUser.id, authorName = currentUser.nickname, text = text, timestamp = last.timestamp))
            } catch (_: Exception) {}
        }
    }
    fun switchTaskNoteType(id: Long, newType: String) {
        viewModelScope.launch {
            repository.switchTaskNoteType(id, newType)
            val updated = repository.getTaskNoteById(id) ?: return@launch
            try {
                apiService.sendTask(
                    VjmTaskDto(
                        id = updated.id,
                        type = updated.type,
                        title = updated.title,
                        content = updated.content,
                        colorHex = updated.colorHex,
                        authorId = updated.authorId,
                        authorName = updated.authorName,
                        priority = updated.priority,
                        isCompleted = updated.isCompleted,
                        createdAt = updated.createdAt
                    )
                )
            } catch (_: Exception) {}
        }
    }
    // Ideas
    fun addIdea(title: String, description: String, initialPotential: Int = 2, priority: String = "NONE", context: android.content.Context? = null) {
        val currentUser = activeUser.value?: return 
        viewModelScope.launch {
            repository.addIdea(title, description, initialPotential, currentUser.id, currentUser.nickname, priority)
            try { com.example.widget.VJMWidgetProvider.updateAllWidgets(getApplication()) } catch (_: Exception) {}
            try {
                val inserted = repository.getLastIdea()
                if (inserted != null) {
                    apiService.sendIdea(VjmIdeaDto(id = inserted.id, title = inserted.title, description = inserted.description, stage = inserted.stage, authorId = inserted.authorId, authorName = inserted.authorName, priority = inserted.priority, starsJson = inserted.starsJson, crownsJson = inserted.crownsJson, potentialsJson = inserted.potentialsJson, createdAt = inserted.createdAt))
                }
            } catch (_: Exception) {}
            triggerNotificationForOthers(currentUser.id, "Nový nápad!", currentUser.nickname, title, "IDEA", "#EAB308", priority = priority)
        }
    }
    fun updateIdea(id: Long, title: String, description: String) {
        viewModelScope.launch {
            val existing = ideas.value.find { it.id == id }?: return@launch
            repository.updateIdea(existing.copy(title = title, description = description))
            pushIdeaToRemote(id)
        }
    }
    fun setUserPotentialForIdea(ideaId: Long, potential: Int) {
        val currentUser = activeUser.value ?: return
        viewModelScope.launch {
            repository.setUserPotentialForIdea(ideaId, currentUser.id, potential)
            pushIdeaToRemote(ideaId)
        }
    }
    fun toggleStar(ideaId: Long) {
        val currentUser = activeUser.value ?: return
        viewModelScope.launch {
            repository.toggleStarOnIdea(ideaId, currentUser.id)
            pushIdeaToRemote(ideaId)
        }
    }
    fun toggleCrownOnIdea(ideaId: Long) {
        val currentUser = activeUser.value ?: return
        viewModelScope.launch {
            repository.toggleCrownOnIdea(ideaId, currentUser.id)
            pushIdeaToRemote(ideaId)
        }
    }
    fun addInfoToIdea(ideaId: Long, infoText: String) {
        viewModelScope.launch {
            repository.addInfoToIdea(ideaId, infoText)
            pushIdeaToRemote(ideaId)
        }
    }
    fun attachFileToIdea(ideaId: Long, fileUri: String) {
        viewModelScope.launch {
            repository.attachFileToIdea(ideaId, fileUri)
            pushIdeaToRemote(ideaId)
        }
    }
    fun submitIdeaProposal(ideaId: Long, name: String, desc: String, photosJson: String) {
        val currentUser = activeUser.value ?: return
        viewModelScope.launch {
            repository.submitIdeaProposal(ideaId, name, desc, photosJson, currentUser.id, currentUser.nickname)
            pushIdeaToRemote(ideaId)
            triggerNotificationForOthers(currentUser.id, "⚠ Návrh projektu k potvrzení", currentUser.nickname, name, "IDEA", "#EF4444", true)
        }
    }
    fun respondToProposal(ideaId: Long, isApproved: Boolean, reason: String = "") {
        val currentUser = activeUser.value ?: return
        viewModelScope.launch {
            repository.respondToProposal(ideaId, currentUser.id, isApproved, reason)
            pushIdeaToRemote(ideaId)
        }
    }
    fun getCommentsForIdea(ideaId: Long) = repository.getCommentsForIdea(ideaId)
    fun addProjectComment(ideaId: Long, text: String, imageUri: String?, context: android.content.Context? = null) {
        val currentUser = activeUser.value ?: return
        viewModelScope.launch {
            repository.addProjectComment(ideaId, text, imageUri, currentUser.id, currentUser.nickname)
            triggerNotificationForOthers(currentUser.id, "Komentář k nápadu", currentUser.nickname, text, "IDEA", "#8B5CF6")
            try {
                val last = repository.getLastProjectComment()
                if (last != null) apiService.sendProjectComment(com.example.data.remote.VjmProjectCommentDto(id = last.id, ideaId = ideaId, authorId = currentUser.id, authorName = currentUser.nickname, text = text, imageUri = imageUri, timestamp = last.timestamp))
            } catch (_: Exception) {}
        }
    }

    // Chat
    fun sendChatMessage(
        content: String,
        imageUri: String? = null,
        attachmentDataUri: String? = null,
        attachmentName: String? = null,
        attachmentMimeType: String? = null,
        priority: String = "NONE"
    ) {
        val currentUser = activeUser.value ?: return
        viewModelScope.launch {
            repository.sendChatMessage(currentUser.id, currentUser.nickname, content, imageUri, attachmentDataUri, attachmentName, attachmentMimeType, priority)
            try { com.example.widget.VJMWidgetProvider.updateAllWidgets(getApplication()) } catch (_: Exception) {}
            try {
                apiService.sendMessage(
                    VjmMessageDto(
                        sender = currentUser.nickname,
                        text = content,
                        imageUri = imageUri,
                        attachmentDataUri = attachmentDataUri,
                        attachmentName = attachmentName,
                        attachmentMimeType = attachmentMimeType,
                        reactionEmoji = null
                    )
                )
            } catch (_: Exception) {}
            val isHigh = priority == "HIGH"
            triggerNotificationForOthers(currentUser.id, if (isHigh) "⚠️ DŮLEŽITÁ zpráva v chatu!" else "Nová zpráva v chatu", currentUser.nickname, content.ifEmpty { "[Obrázek]" }, "CHAT", if (isHigh) "#EF4444" else "#22C55E", isHigh, priority)
        }
    }

    suspend fun uploadAttachmentAndSendChatMessage(
        localUri: String,
        fileName: String,
        mimeType: String,
        content: String,
        priority: String,
        isImage: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        val currentUser = activeUser.value ?: return@withContext false
        try {
            val context = getApplication<Application>()
            val uri = Uri.parse(localUri)
            val bytes = when (uri.scheme?.lowercase()) {
                "file" -> File(uri.path ?: return@withContext false).readBytes()
                else -> context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } ?: return@withContext false

            val uploaded = try {
                val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", fileName, requestBody)
                apiService.uploadChatFile(part)
            } catch (_: Exception) {
                null
            }

            val fallbackLocalUri = runCatching {
                val ext = fileName.substringAfterLast('.', "").ifBlank {
                    when {
                        mimeType.contains("png", true) -> "png"
                        mimeType.contains("webp", true) -> "webp"
                        mimeType.contains("jpeg", true) || mimeType.contains("jpg", true) -> "jpg"
                        mimeType.contains("pdf", true) -> "pdf"
                        mimeType.contains("plain", true) -> "txt"
                        else -> "bin"
                    }
                }
                val safeBase = fileName.substringBeforeLast('.').ifBlank { "attachment" }
                    .replace(Regex("[^A-Za-z0-9._-]"), "_")
                val fallbackFile = File(context.cacheDir, "${safeBase}_${System.currentTimeMillis()}.$ext")
                fallbackFile.writeBytes(bytes)
                Uri.fromFile(fallbackFile).toString()
            }.getOrNull()

            if (uploaded == null && fallbackLocalUri.isNullOrBlank()) return@withContext false

            val finalImageUri = when {
                isImage && uploaded != null -> uploaded.url
                isImage -> fallbackLocalUri
                else -> null
            }
            val finalAttachmentDataUri = when {
                !isImage && uploaded != null -> uploaded.url
                !isImage -> fallbackLocalUri
                else -> null
            }
            val finalAttachmentName = if (isImage) null else (uploaded?.fileName ?: fileName)
            val finalAttachmentMimeType = if (isImage) null else (uploaded?.mimeType ?: mimeType)

            repository.sendChatMessage(
                senderId = currentUser.id,
                senderName = currentUser.nickname,
                content = content,
                imageUri = finalImageUri,
                attachmentDataUri = finalAttachmentDataUri,
                attachmentName = finalAttachmentName,
                attachmentMimeType = finalAttachmentMimeType,
                priority = priority
            )
            try { com.example.widget.VJMWidgetProvider.updateAllWidgets(getApplication()) } catch (_: Exception) {}
            try {
                apiService.sendMessage(
                    VjmMessageDto(
                        sender = currentUser.nickname,
                        text = content,
                        imageUri = finalImageUri,
                        attachmentDataUri = finalAttachmentDataUri,
                        attachmentName = finalAttachmentName,
                        attachmentMimeType = finalAttachmentMimeType,
                        reactionEmoji = null
                    )
                )
            } catch (_: Exception) {}

            val isHigh = priority == "HIGH"
            triggerNotificationForOthers(currentUser.id, if (isHigh) "⚠️ DŮLEŽITÁ zpráva v chatu!" else "Nová zpráva v chatu", currentUser.nickname, content.ifEmpty { if (isImage) "[Obrázek]" else "[Soubor]" }, "CHAT", if (isHigh) "#EF4444" else "#22C55E", isHigh, priority)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun sendSecretChatMessage(content: String, priority: String = "NONE") {
        val currentUser = activeUser.value ?: return
        if (currentUser.id == "tata" || currentUser.defaultName.contains("Michal", true) || currentUser.nickname.contains("Michal", true)) return
        viewModelScope.launch {
            repository.sendSecretChatMessage(currentUser.id, currentUser.nickname, content, priority)
            try {
                apiService.sendSecretMessage(com.example.data.remote.VjmSecretMessageDto(senderId = currentUser.id, senderName = currentUser.nickname, text = content, priority = priority))
            } catch (_: Exception) {}
        }
    }
    fun clearChatHistory() { viewModelScope.launch { repository.clearChatHistory() } }
    fun markChatMessageRead(message: ChatMessageEntity) {
        val currentUser = activeUser.value ?: return
        viewModelScope.launch { repository.markChatMessageRead(message, currentUser.id) }
    }
    fun setMessageReaction(message: ChatMessageEntity, emoji: String?) {
        viewModelScope.launch {
            val userId = activeUser.value?.id?: return@launch
            val currentMap = try {
                val raw = message.reactionEmoji
                if (raw.isNullOrBlank()) mutableMapOf<String, String>()
                else {
                    val obj = JSONObject(raw)
                    val map = mutableMapOf<String, String>()
                    obj.keys().forEach { k -> map[k] = obj.getString(k) }
                    map
                }
            } catch (e: Exception) { mutableMapOf() }

            if (emoji.isNullOrEmpty()) currentMap.remove(userId)
            else {
                if (currentMap[userId] == emoji) currentMap.remove(userId)
                else currentMap[userId] = emoji
            }
            val newStr = if (currentMap.isEmpty()) null else {
                val obj = JSONObject()
                currentMap.forEach { (k,v) -> obj.put(k,v) }
                obj.toString()
            }
            repository.setMessageReaction(message, newStr)
            try {
                apiService.updateMessageReaction(
                    MessageReactionUpdateDto(
                        sender = message.senderName,
                        text = message.content,
                        timestamp = message.timestamp,
                        reactionEmoji = newStr
                    )
                )
            } catch (_: Exception) {}
        }
    }

    fun incrementPraise(message: ChatMessageEntity) {
        val me = activeUser.value ?: return
        if (message.senderId == me.id || message.senderName == me.nickname) return
        viewModelScope.launch {
            val map = try {
                val raw = message.reactionEmoji
                if (raw.isNullOrBlank()) mutableMapOf<String, String>()
                else {
                    val obj = JSONObject(raw)
                    val parsed = mutableMapOf<String, String>()
                    obj.keys().forEach { k -> parsed[k] = obj.getString(k) }
                    parsed
                }
            } catch (_: Exception) {
                mutableMapOf()
            }

            val praiseKey = "__praise_${me.id}"
            if (map[praiseKey] == "1") return@launch
            map[praiseKey] = "1"
            map.remove("__praise")

            val obj = JSONObject()
            map.forEach { (k, v) -> obj.put(k, v) }
            val reactionJson = obj.toString()
            repository.setMessageReaction(message, reactionJson)
            try {
                apiService.updateMessageReaction(
                    MessageReactionUpdateDto(
                        sender = message.senderName,
                        text = message.content,
                        timestamp = message.timestamp,
                        reactionEmoji = reactionJson
                    )
                )
            } catch (_: Exception) {}
        }
    }

    // helper pro UI - už počítá +60 správně, ne jen +4
    fun parseReactions(raw: String?): Map<String, Int> {
        if (raw.isNullOrBlank()) return emptyMap()
        return try {
            val obj = JSONObject(raw)
            val values = mutableListOf<String>()
            obj.keys().forEach { k -> values.add(obj.getString(k)) }
            values.groupingBy { it }.eachCount()
        } catch (e: Exception) { emptyMap() }
    }

    private suspend fun pushIdeaToRemote(ideaId: Long) {
        val idea = repository.getIdeaById(ideaId) ?: return
        try {
            apiService.sendIdea(
                VjmIdeaDto(
                    id = idea.id,
                    title = idea.title,
                    description = idea.description,
                    stage = idea.stage,
                    authorId = idea.authorId,
                    authorName = idea.authorName,
                    priority = idea.priority,
                    starsJson = idea.starsJson,
                    crownsJson = idea.crownsJson,
                    potentialsJson = idea.potentialsJson,
                    createdAt = idea.createdAt
                )
            )
        } catch (_: Exception) {}
    }

    // Calendar
    fun addCalendarEvent(title: String, description: String, colorCategoryHex: String, startTs: Long, endTs: Long, isAllDay: Boolean, location: String, isRecurring: Boolean, recurrenceRule: String, reminderMins: Int, priority: String) {
        val currentUser = activeUser.value ?: return
        viewModelScope.launch {
            val baseEvent = CalendarEventEntity(title = title, description = description, colorCategoryHex = colorCategoryHex, startDateTimestamp = startTs, endDateTimestamp = endTs, isAllDay = isAllDay, location = location, isRecurring = isRecurring, recurrenceRule = recurrenceRule, reminderMinutes = reminderMins, priority = priority, authorId = currentUser.id, authorName = currentUser.nickname)
            repository.addCalendarEvent(baseEvent)
            try { com.example.widget.VJMWidgetProvider.updateAllWidgets(getApplication()) } catch (_: Exception) {}
            try {
                val last = repository.getLastCalendarEvent()
                if (last != null) {
                    val remote = apiService.sendCalendar(
                        VjmCalendarDto(
                            id = last.id,
                            title = title,
                            description = description,
                            colorCategoryHex = colorCategoryHex,
                            startDateTimestamp = startTs,
                            endDateTimestamp = endTs,
                            authorId = currentUser.id,
                            authorName = currentUser.nickname,
                            priority = priority,
                            createdAt = last.createdAt
                        )
                    )
                    repository.insertRemoteCalendar(
                        remote.id,
                        remote.title,
                        remote.description,
                        remote.colorCategoryHex,
                        remote.startDateTimestamp,
                        remote.endDateTimestamp,
                        remote.authorId,
                        remote.authorName,
                        remote.priority,
                        remote.createdAt
                    )
                }
            } catch (_: Exception) {}

            if (isRecurring && recurrenceRule != "Žádné") {
                val count = when (recurrenceRule) {
                    "Denně" -> 30
                    "Týdně" -> 12
                    "Měsíčně" -> 12
                    "Ročně" -> 5
                    else -> 0
                }
                val calStart = java.util.Calendar.getInstance()
                val calEnd = java.util.Calendar.getInstance()
                for (i in 1 until count) {
                    calStart.timeInMillis = startTs
                    calEnd.timeInMillis = endTs
                    when (recurrenceRule) {
                        "Denně" -> { calStart.add(java.util.Calendar.DAY_OF_YEAR, i); calEnd.add(java.util.Calendar.DAY_OF_YEAR, i) }
                        "Týdně" -> { calStart.add(java.util.Calendar.WEEK_OF_YEAR, i); calEnd.add(java.util.Calendar.WEEK_OF_YEAR, i) }
                        "Měsíčně" -> { calStart.add(java.util.Calendar.MONTH, i); calEnd.add(java.util.Calendar.MONTH, i) }
                        "Ročně" -> { calStart.add(java.util.Calendar.YEAR, i); calEnd.add(java.util.Calendar.YEAR, i) }
                    }
                    val recurring = baseEvent.copy(id = 0, startDateTimestamp = calStart.timeInMillis, endDateTimestamp = calEnd.timeInMillis, isRecurring = false, recurrenceRule = "Žádné")
                    repository.addCalendarEventSilently(recurring)
                    try { apiService.sendCalendar(VjmCalendarDto(id = 0, title = title, description = description, colorCategoryHex = colorCategoryHex, startDateTimestamp = calStart.timeInMillis, endDateTimestamp = calEnd.timeInMillis, authorId = currentUser.id, authorName = currentUser.nickname, priority = priority, createdAt = System.currentTimeMillis())) } catch (_: Exception) {}
                }
            }

            val isWarning = priority == "HIGH"
            triggerNotificationForOthers(currentUser.id, if (isWarning) "⚠️ Těžká událost v kalendáři!" else "Nová událost v kalendáři", currentUser.nickname, title, "CALENDAR", if (isWarning) "#EF4444" else "#06B6D4", isWarning, priority)
        }
    }
    fun updateCalendarEvent(event: CalendarEventEntity) {
        val currentUser = activeUser.value ?: return
        viewModelScope.launch {
            repository.updateCalendarEvent(event, currentUser.id)
            val updated = repository.getCalendarEventById(event.id) ?: return@launch
            try {
                apiService.sendCalendar(
                    VjmCalendarDto(
                        id = updated.id,
                        title = updated.title,
                        description = updated.description,
                        colorCategoryHex = updated.colorCategoryHex,
                        startDateTimestamp = updated.startDateTimestamp,
                        endDateTimestamp = updated.endDateTimestamp,
                        authorId = updated.authorId,
                        authorName = updated.authorName,
                        priority = updated.priority,
                        createdAt = updated.createdAt
                    )
                )
            } catch (_: Exception) {}
        }
    }
    fun deleteCalendarEvent(id: Long) {
        viewModelScope.launch {
            repository.deleteCalendarEvent(id)
            try { apiService.deleteCalendar(id) } catch (_: Exception) {}
        }
    }
    fun getCommentsForCalendarEvent(eventId: Long) = repository.getCommentsForCalendarEvent(eventId)
    fun addCalendarComment(eventId: Long, text: String, context: android.content.Context? = null) {
        val currentUser = activeUser.value ?: return
        viewModelScope.launch { 
            repository.addCalendarComment(eventId, currentUser.id, currentUser.nickname, text)
            try {
                val last = repository.getLastCalendarComment()
                if (last != null) apiService.sendCalendarComment(com.example.data.remote.VjmCalendarCommentDto(id = last.id, eventId = eventId, authorId = currentUser.id, authorName = currentUser.nickname, text = text, timestamp = last.timestamp))
            } catch (_: Exception) {}
            triggerNotificationForOthers(currentUser.id, "Komentář ke kalendáři", currentUser.nickname, text, "CALENDAR", "#06B6D4")
        }
    }
    fun dismissNotification() { _notification.value = null }

 
    fun resetUserPin(targetId: String, newPin: String) {
        viewModelScope.launch { repository.resetSinglePin(targetId, newPin) }
    }
    fun resetAllPinsToDefault() {
        viewModelScope.launch { repository.resetPinsForAdmin() }
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.clearAllCoreData()
            try { com.example.widget.VJMWidgetProvider.updateAllWidgets(getApplication()) } catch (_: Exception) {}
        }
    }

    fun setPendingWidgetTask(title: String, type: String = "TASK") {
        _pendingWidgetTask.value = title to type
    }
    fun clearPendingWidgetTask() { _pendingWidgetTask.value = null }

    fun setPendingWidgetIdea(title: String) {
        _pendingWidgetIdea.value = title
    }
    fun clearPendingWidgetIdea() { _pendingWidgetIdea.value = null }

    fun setPendingWidgetCalendarEvent(title: String) {
        _pendingWidgetCalendar.value = title
    }
    fun clearPendingWidgetCalendarEvent() { _pendingWidgetCalendar.value = null }

    fun setPendingWidgetChat(text: String) {
        _pendingWidgetChat.value = text
    }
    fun clearPendingWidgetChat() { _pendingWidgetChat.value = null }

    fun setAppInForeground(inForeground: Boolean) {
        appInForeground = inForeground
    }

    fun setCurrentScreenRoute(route: String?) {
        if (!route.isNullOrBlank()) currentScreenRoute = route
    }

    fun testAdvanceIdea(ideaId: Long) {
        viewModelScope.launch {
            val idea = repository.getIdeaById(ideaId) ?: return@launch
            // Samsung vs POCO: TEST vidí jen admin (id=admin=Michal). Na Samsungu jsi přihlášen jako Tom/Adélka, proto ho nevidíš. Na POCO jako Michal ano.
            if (idea.stage == "LIST") {
                // Krok 1: LIST -> Předrealizace (2 hvězdičky)
                repository.toggleStarOnIdea(ideaId, "kamaradka")
                repository.toggleStarOnIdea(ideaId, "tata")
            } else if (idea.stage == "REALIZACE_DRAFT") {
                // Krok 2: Předrealizace -> Realizace (3 korunky) = 0/3 -> 3/3
                repository.toggleCrownOnIdea(ideaId, "admin")
                repository.toggleCrownOnIdea(ideaId, "kamaradka")
                repository.toggleCrownOnIdea(ideaId, "tata")
            } else if (idea.approvalStatus == "PROPOSED") {
                // Krok 3: Realizace -> HOTOVO (3 potvrzení)
                repository.respondToProposal(ideaId, "kamaradka", true, "Test OK")
                repository.respondToProposal(ideaId, "tata", true, "Test OK")
                repository.respondToProposal(ideaId, "admin", true, "Test OK")
            }
        }
    }

    private fun triggerNotificationForOthers(senderId: String, title: String, sender: String, message: String, category: String, colorHex: String = "#3B82F6", isWarning: Boolean = false, priority: String = "NONE") {
        // autor nikdy nedostane notifikaci ani zvuk
        if (_activeUserId.value == senderId) return
        val active = activeUser.value
        if (active != null && (sender == active.nickname || senderId == active.id)) return

        val normalizedCategory = when (category.uppercase()) {
            "NOTE" -> "TASK"
            else -> category.uppercase()
        }

        val targetRoute = when (normalizedCategory) {
            "CHAT" -> "chat"
            "SECRET", "SECRET_CHAT" -> "secret_chat"
            "TASK" -> "tasks"
            "IDEA" -> "ideas"
            "CALENDAR" -> "calendar"
            else -> "home"
        }
        val sameScreenNow = appInForeground && currentScreenRoute == targetRoute

        if (appInForeground) {
            _notification.value = ActiveNotification(title = title, senderId = senderId, senderName = sender, message = message, category = category, colorHex = colorHex, isWarningAlert = isWarning, priority = priority)
        }

        val now = System.currentTimeMillis()
        val soundCooldownMs = when (normalizedCategory) {
            "CHAT", "SECRET", "SECRET_CHAT" -> 1200L
            else -> 45_000L
        }
        val lastSound = lastSoundAt[normalizedCategory] ?: 0L
        val canPlaySound = !sameScreenNow && now - lastSound >= soundCooldownMs

        if (canPlaySound) {
            val soundRes = when (normalizedCategory) {
                "IDEA" -> R.raw.napad
                "CHAT" -> R.raw.novazprava
                "TASK" -> R.raw.poznamkaukol
                "CALENDAR" -> R.raw.provse
                else -> R.raw.provse
            }
            try {
                SoundManager.play(getApplication<Application>(), soundRes)
                lastSoundAt[normalizedCategory] = now
            } catch (_: Exception) {}
        }

        if (!appInForeground) {
            val systemCooldownMs = when (normalizedCategory) {
                "CHAT", "SECRET", "SECRET_CHAT" -> 1500L
                else -> 5 * 60 * 1000L
            }
            val lastSystem = lastSystemNotificationAt[normalizedCategory] ?: 0L
            if (now - lastSystem >= systemCooldownMs) {
                val channelId = when (normalizedCategory) {
                    "CHAT" -> SystemNotificationHelper.CHANNEL_CHAT
                    "SECRET", "SECRET_CHAT" -> SystemNotificationHelper.CHANNEL_SECRET
                    "TASK" -> SystemNotificationHelper.CHANNEL_TASK
                    "IDEA" -> SystemNotificationHelper.CHANNEL_IDEA
                    "CALENDAR" -> SystemNotificationHelper.CHANNEL_CALENDAR
                    else -> SystemNotificationHelper.CHANNEL_COMMENT
                }
                val notificationId = when (normalizedCategory) {
                    "CHAT" -> 2000 + (now % 1000).toInt()
                    "SECRET", "SECRET_CHAT" -> 2100 + (now % 1000).toInt()
                    "TASK" -> 2201
                    "IDEA" -> 2202
                    "CALENDAR" -> 2203
                    else -> 2299
                }
                try {
                    SystemNotificationHelper.showNotification(
                        context = getApplication(),
                        channelId = channelId,
                        title = title,
                        message = "$sender: ${message.take(120)}",
                        notificationId = notificationId,
                        category = normalizedCategory,
                        priority = priority
                    )
                    lastSystemNotificationAt[normalizedCategory] = now
                } catch (_: Exception) {}
            }
        }
    }
}
