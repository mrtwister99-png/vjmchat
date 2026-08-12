package com.example.data

import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class TeamRepository(private val dao: TeamDao) {
    private fun parseJsonList(jsonStr: String): List<String> {
        return try {
            val arr = JSONArray(jsonStr)
            List(arr.length()) { arr.getString(it) }
        } catch (e: Exception) { emptyList() }
    }
    private fun stringListToJson(list: List<String>): String {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        return arr.toString()
    }
    private fun stringMapToJson(map: Map<String, String>): String {
        val obj = JSONObject()
        map.forEach { (k,v) -> obj.put(k,v) }
        return obj.toString()
    }
    private fun jsonToStringMap(jsonStr: String): MutableMap<String, String> {
        return try {
            val obj = JSONObject(jsonStr)
            val result = mutableMapOf<String,String>()
            obj.keys().forEach { key -> result[key] = obj.getString(key) }
            result
        } catch (e: Exception) { mutableMapOf() }
    }

    val users: Flow<List<UserEntity>> = dao.getAllUsers()
    val activities: Flow<List<ActivityEntity>> = dao.getRecentActivities()
    val tasksAndNotes: Flow<List<TaskNoteEntity>> = dao.getAllTasksAndNotes()
    val ideas: Flow<List<IdeaEntity>> = dao.getAllIdeas()
    val chatMessages: Flow<List<ChatMessageEntity>> = dao.getAllChatMessages()
    val secretChatMessages: Flow<List<ChatMessageEntity>> = dao.getSecretChatMessages()
    val calendarEvents: Flow<List<CalendarEventEntity>> = dao.getAllCalendarEvents()

    suspend fun initDefaultDataIfNeeded() {
        // FINAL PINY: Tom(admin)=2242, Adélka(kamaradka)=2221, Michal(tata)=3331 - zadne stare
        val finalPins = mapOf("admin" to "2242", "kamaradka" to "2221", "tata" to "3331")
        val finalNames = mapOf("admin" to Pair("Tom", "Tom"), "kamaradka" to Pair("Adélka", "Adélka"), "tata" to Pair("Michal", "Michal"))
        if (dao.getUserCount() == 0) {
            val defaultUsers = listOf(
                UserEntity(
                    id = "admin",
                    defaultName = "Tom",
                    nickname = "Tom",
                    role = "Admin",
                    pin = "2242",
                    email = "tom@firma.cz",
                    avatarEmoji = "👑",
                    borderHexColor = "#0086cf",
                    isOnline = true
                ),
                UserEntity(
                    id = "kamaradka",
                    defaultName = "Adélka",
                    nickname = "Adélka",
                    role = "Člen",
                    pin = "2221",
                    email = "adelka@firma.cz",
                    avatarEmoji = "🌸",
                    borderHexColor = "#a8006d",
                    isOnline = true
                ),
                UserEntity(
                    id = "tata",
                    defaultName = "Michal",
                    nickname = "Michal",
                    role = "Člen",
                    pin = "3331",
                    email = "michal@firma.cz",
                    avatarEmoji = "🛠️",
                    borderHexColor = "#09c702",
                    isOnline = false
                )
            )
            dao.insertUsers(defaultUsers)
        } else {
            // oprava existujicich - smazat stare PINy VSUDE FURT
            val existing = dao.getAllUsersList()
            existing.forEach { u ->
                val correctPin = finalPins[u.id]
                val correctNames = finalNames[u.id]
                if (correctPin != null && u.pin != correctPin) {
                    dao.updateUser(u.copy(pin = correctPin, defaultName = correctNames?.first ?: u.defaultName, nickname = correctNames?.second ?: u.nickname))
                }
            }
        }
    }

    // RESET PINU pro admina TOM - vola se z ProfileSettings
    suspend fun resetPinsForAdmin() {
        dao.getUserById("kamaradka")?.let { dao.updateUser(it.copy(pin = "2221")) }
        dao.getUserById("tata")?.let { dao.updateUser(it.copy(pin = "3331")) }
        dao.getUserById("admin")?.let { dao.updateUser(it.copy(pin = "2242")) }
    }

    suspend fun resetSinglePin(userId: String, defaultPin: String) {
        dao.getUserById(userId)?.let { dao.updateUser(it.copy(pin = defaultPin)) }
    }

    suspend fun getUserById(userId: String) = dao.getUserById(userId)
    suspend fun getTaskNoteById(id: Long) = dao.getTaskNoteById(id)
    suspend fun getLastTaskNote() = dao.getLastTaskNote()
    suspend fun updateLastTaskRead(userId: String, ts: Long) {
        dao.getUserById(userId)?.let { dao.updateUser(it.copy(lastTaskReadTimestamp = ts)) }
    }
    suspend fun updateLastIdeaRead(userId: String, ts: Long) {
        dao.getUserById(userId)?.let { dao.updateUser(it.copy(lastIdeaReadTimestamp = ts)) }
    }
    suspend fun updateLastCalendarRead(userId: String, ts: Long) {
        dao.getUserById(userId)?.let { dao.updateUser(it.copy(lastCalendarReadTimestamp = ts)) }
    }
    suspend fun updateLastChatRead(userId: String, ts: Long) {
        dao.getUserById(userId)?.let { dao.updateUser(it.copy(lastChatReadTimestamp = ts)) }
    }
    suspend fun updateLastActivityRead(userId: String, ts: Long) {
        dao.getUserById(userId)?.let { dao.updateUser(it.copy(lastActivityReadTimestamp = ts)) }
    }
    suspend fun getIdeaById(id: Long) = dao.getIdeaById(id)
    suspend fun getLastIdea() = dao.getLastIdea()
    suspend fun getCalendarEventById(id: Long) = dao.getCalendarEventById(id)
    suspend fun getLastCalendarEvent() = dao.getLastCalendarEvent()
    suspend fun updateUserFull(user: UserEntity) = dao.updateUser(user)

    suspend fun updateUserProfile(userId: String, nickname: String, avatarEmoji: String, borderHexColor: String, email: String = "", pin: String = "") {
        val user = dao.getUserById(userId)
        if (user != null) {
            dao.updateUser(
                user.copy(
                    nickname = nickname,
                    avatarEmoji = avatarEmoji,
                    borderHexColor = borderHexColor,
                    email = if (email.isNotBlank()) email else user.email,
                    pin = if (pin.isNotBlank()) pin else user.pin
                )
            )
        }
    }

    suspend fun toggleActivityReadStatus(activity: ActivityEntity) {
        dao.updateActivity(activity.copy(isReadByActiveUser = !activity.isReadByActiveUser))
    }

    // UKOL 5: like - LIVE přes Flow + remote sync pro +1/+2/+3 obrys
    suspend fun toggleActivityLike(activity: ActivityEntity, userId: String) {
        val current = activity.likedByIds.split(",").filter { it.isNotBlank() }.toMutableSet()
        if (current.contains(userId)) current.remove(userId) else current.add(userId)
        val newStr = current.joinToString(",")
        dao.updateActivity(activity.copy(likedByIds = newStr))
    }

    suspend fun getActivityById(id: Long): ActivityEntity? {
        return dao.getAllActivitiesList().find { it.id == id }
    }

    suspend fun insertRemoteActivityLike(id: Long, likedByIds: String): Boolean {
        val existing = getActivityById(id) ?: return false
        if (existing.likedByIds != likedByIds) {
            dao.updateActivity(existing.copy(likedByIds = likedByIds))
            return true
        }
        return false
    }

    suspend fun insertRemoteActivities(activities: List<com.example.data.remote.VjmActivityDto>) {
        activities.forEach { dto ->
            if (dto.likedByIds.isNotBlank()) {
                insertRemoteActivityLike(dto.id, dto.likedByIds)
            }
        }
    }

    // Tasks & Notes
    suspend fun addTaskOrNote(item: TaskNoteEntity) {
        dao.insertTaskNote(item)
        insertActivityWithLimit(
            ActivityEntity(
                category = if (item.type == "TASK") "TASK" else "NOTE",
                title = if (item.type == "TASK") "Nový úkol: ${item.title}" else "Nová poznámka: ${item.title}",
                description = item.content.take(60),
                authorId = item.authorId,
                authorName = item.authorName,
                priority = item.priority
            )
        )
    }

    private suspend fun insertActivityWithLimit(activity: ActivityEntity) {
        dao.insertActivity(activity)
        val all = dao.getAllActivitiesList().sortedByDescending { it.timestamp }
        if (all.size > 20) all.drop(20).forEach { dao.deleteActivityById(it.id) }
    }

    suspend fun insertRemoteTask(id: Long, type: String, title: String, content: String, colorHex: String, authorId: String, authorName: String, priority: String, isCompleted: Boolean, createdAt: Long): Boolean {
        val existing = dao.getTaskNoteById(id)
        if (existing == null) {
            dao.insertTaskNote(TaskNoteEntity(id = id, type = type, title = title, content = content, colorHex = colorHex, authorId = authorId, authorName = authorName, priority = priority, isCompleted = isCompleted, createdAt = createdAt))
            insertActivityWithLimit(ActivityEntity(category = if(type=="TASK") "TASK" else "NOTE", title = if(type=="TASK") "Nový úkol: $title" else "Nová poznámka: $title", description = content.take(60), authorId = authorId, authorName = authorName, priority = priority))
            return true
        }
        val updated = existing.copy(
            type = type,
            title = title,
            content = content,
            colorHex = colorHex,
            authorId = authorId,
            authorName = authorName,
            priority = priority,
            isCompleted = isCompleted,
            createdAt = createdAt
        )
        if (updated != existing) {
            dao.updateTaskNote(updated)
            return true
        }
        return false
    }
    suspend fun insertRemoteIdea(id: Long, title: String, description: String, stage: String, authorId: String, authorName: String, priority: String, starsJson: String, crownsJson: String, potentialsJson: String, createdAt: Long): Boolean {
        val existing = dao.getIdeaById(id)
        if (existing == null) {
            dao.insertIdea(IdeaEntity(id = id, title = title, description = description, stage = stage, authorId = authorId, authorName = authorName, priority = priority, starsJson = starsJson, crownsJson = crownsJson, potentialsJson = potentialsJson, createdAt = createdAt))
            insertActivityWithLimit(ActivityEntity(category = "IDEA", title = "Nápad: $title", description = description.take(60), authorId = authorId, authorName = authorName, priority = priority))
            return true
        }
        val updated = existing.copy(
            title = title,
            description = description,
            stage = stage,
            authorId = authorId,
            authorName = authorName,
            priority = priority,
            starsJson = starsJson,
            crownsJson = crownsJson,
            potentialsJson = potentialsJson,
            createdAt = createdAt
        )
        if (updated != existing) {
            dao.updateIdea(updated)
            return true
        }
        return false
    }
       suspend fun insertRemoteCalendar(id: Long, title: String, description: String, colorCategoryHex: String = "#00c43b", startDateTimestamp: Long, endDateTimestamp: Long, authorId: String, authorName: String, priority: String, createdAt: Long): Boolean {
        val existing = dao.getCalendarEventById(id)
        if (existing != null) {
            val updated = existing.copy(
                title = title,
                description = description,
                colorCategoryHex = colorCategoryHex,
                startDateTimestamp = startDateTimestamp,
                endDateTimestamp = endDateTimestamp,
                authorId = authorId,
                authorName = authorName,
                priority = priority,
                createdAt = createdAt
            )
            if (updated != existing) {
                dao.updateCalendarEvent(updated)
                return true
            }
            return false
        }
        dao.insertCalendarEvent(CalendarEventEntity(id = id, title = title, description = description, colorCategoryHex = colorCategoryHex, startDateTimestamp = startDateTimestamp, endDateTimestamp = endDateTimestamp, authorId = authorId, authorName = authorName, priority = priority, createdAt = createdAt, isAllDay = false, location = "", isRecurring = false, recurrenceRule = "", reminderMinutes = 0))
        insertActivityWithLimit(ActivityEntity(category = "CALENDAR", title = "Kalendář: $title", description = description.take(60), authorId = authorId, authorName = authorName, priority = priority))
        return true
    }

    suspend fun reconcileTasks(remoteIds: Set<Long>): Boolean {
        var changed = false
        dao.getAllTasksAndNotesList().forEach { local ->
            if (!remoteIds.contains(local.id)) {
                dao.deleteTaskNote(local.id)
                changed = true
            }
        }
        return changed
    }

    suspend fun reconcileIdeas(remoteIds: Set<Long>): Boolean {
        var changed = false
        dao.getAllIdeasList().forEach { local ->
            if (!remoteIds.contains(local.id)) {
                dao.deleteIdea(local.id)
                changed = true
            }
        }
        return changed
    }

    suspend fun reconcileCalendar(remoteIds: Set<Long>): Boolean {
        var changed = false
        val now = System.currentTimeMillis()
        dao.getAllCalendarEventsList().forEach { local ->
            val isFreshLocalEvent = now - local.createdAt < 10 * 60 * 1000
            if (!remoteIds.contains(local.id) && !isFreshLocalEvent) {
                dao.deleteCalendarEvent(local.id)
                changed = true
            }
        }
        return changed
    }

    suspend fun updateTaskOrNote(item: TaskNoteEntity, editorId: String) {
        val existing = dao.getTaskNoteById(item.id) ?: return
        val newEditCount = if (existing.authorId == editorId) existing.editCount + 1 else existing.editCount
        dao.updateTaskNote(item.copy(editCount = newEditCount))
    }

    suspend fun toggleTaskCompletion(item: TaskNoteEntity) {
        dao.updateTaskNote(item.copy(isCompleted = !item.isCompleted))
    }

    suspend fun requestOrConfirmTaskNoteDeletion(id: Long, userId: String) {
        val existing = dao.getTaskNoteById(id) ?: return
        val currentRequests = parseJsonList(existing.deletionRequestsJson).toMutableSet()
        currentRequests.add(userId)

        if (currentRequests.size >= 3) {
            dao.deleteTaskNote(id)
        } else {
            val updatedJson = stringListToJson(currentRequests.toList())
            dao.updateTaskNote(existing.copy(deletionRequestsJson = updatedJson))
        }
    }

    suspend fun deleteTaskNoteDirect(id: Long) {
        dao.deleteTaskNote(id)
        // server mazani resi ViewModel pres apiService.deleteTask - zde jen local
    }

    fun getCommentsForTaskNote(taskNoteId: Long): Flow<List<TaskNoteCommentEntity>> =
        dao.getCommentsForTaskNote(taskNoteId)

    suspend fun addTaskNoteComment(taskNoteId: Long, authorId: String, authorName: String, text: String) {
        dao.insertTaskNoteComment(
            TaskNoteCommentEntity(
                taskNoteId = taskNoteId,
                authorId = authorId,
                authorName = authorName,
                text = text
            )
        )
        insertActivityWithLimit(
            ActivityEntity(
                category = "TASK",
                title = "Nový komentář : ÚKOL",
                description = text.take(100),
                authorId = authorId,
                authorName = authorName
            )
        )
    }

    suspend fun switchTaskNoteType(id: Long, newType: String) {
        val existing = dao.getTaskNoteById(id) ?: return
        dao.updateTaskNote(existing.copy(type = newType))
    }

    // Ideas & Projects
    suspend fun addIdea(
        title: String,
        description: String,
        initialPotential: Int = 2,
        authorId: String,
        authorName: String,
        priority: String = "NONE"
    ) {
        val potentialsMap = mutableMapOf(authorId to initialPotential.toString())
        val idea = IdeaEntity(
            title = title,
            description = description,
            stage = "LIST",
            potentialsJson = stringMapToJson(potentialsMap),
            priority = priority,
            authorId = authorId,
            authorName = authorName
        )
        dao.insertIdea(idea)
        insertActivityWithLimit(
            ActivityEntity(
                category = "IDEA",
                title = "Nový nápad: $title",
                description = description.take(60),
                authorId = authorId,
                authorName = authorName,
                priority = priority
            )
        )
    }

    suspend fun updateIdea(idea: IdeaEntity) {
        dao.updateIdea(idea)
    }

    suspend fun setUserPotentialForIdea(ideaId: Long, userId: String, potential: Int) {
        val idea = dao.getIdeaById(ideaId) ?: return
        val map = jsonToStringMap(idea.potentialsJson)
        map[userId] = potential.toString()
        dao.updateIdea(idea.copy(potentialsJson = stringMapToJson(map)))
    }

    suspend fun toggleStarOnIdea(ideaId: Long, userId: String) {
        val idea = dao.getIdeaById(ideaId) ?: return
        // Author cannot star their own idea!
        if (idea.authorId == userId) return

        val currentStars = parseJsonList(idea.starsJson).toMutableSet()
        if (currentStars.contains(userId)) {
            currentStars.remove(userId)
        } else {
            currentStars.add(userId)
        }

        val updatedStarsJson = stringListToJson(currentStars.toList())
        var newStage = idea.stage
        if (currentStars.size >= 2 && idea.stage == "LIST") {
            newStage = "REALIZACE_DRAFT"
            dao.insertActivity(
                ActivityEntity(
                    category = "IDEA",
                    title = "Nápad posunut do Předrealizace: ${idea.title}",
                    description = "2/2 hvězdičky - nyní 0/3 potvrzení pro postup do Realizace",
                    authorId = userId,
                    authorName = "Tým"
                )
            )
        }

        val resetApprovalsOnMove = if (newStage != idea.stage) "[]" else idea.approvalsJson
        val resetStatusOnMove = if (newStage != idea.stage) "NONE" else idea.approvalStatus
        dao.updateIdea(idea.copy(starsJson = updatedStarsJson, stage = newStage, approvalsJson = resetApprovalsOnMove, approvalStatus = resetStatusOnMove))
    }

    suspend fun toggleCrownOnIdea(ideaId: Long, userId: String) {
        val idea = dao.getIdeaById(ideaId) ?: return
        val currentCrowns = parseJsonList(idea.crownsJson).toMutableSet()
        if (currentCrowns.contains(userId)) {
            currentCrowns.remove(userId)
        } else {
            currentCrowns.add(userId)
        }

        val updatedCrownsJson = stringListToJson(currentCrowns.toList())
        var newStage = idea.stage
        // 3 crowns needed to advance to final Realizace!
        if (currentCrowns.size >= 3 && idea.stage == "REALIZACE_DRAFT") {
            newStage = "REALIZACE_FINAL"
            dao.insertActivity(
                ActivityEntity(
                    category = "IDEA",
                    title = "👑 3/3 Korunky! Nápad v REALIZACI: ${idea.title}",
                    description = "Všichni 3 členové schválili korunkou!",
                    authorId = userId,
                    authorName = "Tým"
                )
            )
        }

        dao.updateIdea(idea.copy(crownsJson = updatedCrownsJson, stage = newStage))
    }

    suspend fun addInfoToIdea(ideaId: Long, infoText: String) {
        val idea = dao.getIdeaById(ideaId) ?: return
        val list = parseJsonList(idea.infoListJson).toMutableList()
        if (infoText.isNotBlank()) {
            list.add(infoText)
            dao.updateIdea(idea.copy(infoListJson = stringListToJson(list)))
        }
    }

    suspend fun attachFileToIdea(ideaId: Long, fileUri: String) {
        val idea = dao.getIdeaById(ideaId) ?: return
        val list = parseJsonList(idea.attachmentsJson).toMutableList()
        list.add(fileUri)
        dao.updateIdea(idea.copy(attachmentsJson = stringListToJson(list)))
    }

    suspend fun submitIdeaProposal(ideaId: Long, name: String, desc: String, photosJson: String, authorId: String, authorName: String) {
        val idea = dao.getIdeaById(ideaId) ?: return
        dao.updateIdea(
            idea.copy(
                proposalName = name,
                proposalDesc = desc,
                proposalImagesJson = photosJson,
                approvalStatus = "PROPOSED",
                approvalsJson = stringListToJson(listOf(authorId))
            )
        )
        insertActivityWithLimit(
            ActivityEntity(
                category = "IDEA",
                title = "⚠️ Návrh projektu k potvrzení: $name",
                description = desc.take(60),
                authorId = authorId,
                authorName = authorName
            )
        )
    }

    suspend fun respondToProposal(ideaId: Long, userId: String, isApproved: Boolean, reason: String = "") {
        val idea = dao.getIdeaById(ideaId) ?: return
        val currentApprovals = parseJsonList(idea.approvalsJson).toMutableSet()
        val currentRejectionsMap = jsonToStringMap(idea.rejectionsJson)
        val approvalNotesMap = jsonToStringMap(idea.approvalNotesJson)

        approvalNotesMap[userId] = reason.ifEmpty { if (isApproved) "Potvrzeno" else "Zamítnuto" }

        if (isApproved) {
            currentApprovals.add(userId)
            currentRejectionsMap.remove(userId)
        } else {
            currentApprovals.remove(userId)
            currentRejectionsMap[userId] = reason.ifEmpty { "Není schváleno" }
        }

        val newApprovalStatus = when {
            currentApprovals.size >= 3 -> "CONFIRMED"
            currentRejectionsMap.isNotEmpty() -> "REJECTED"
            else -> "PROPOSED"
        }

        val newStage = if (newApprovalStatus == "CONFIRMED") "REALIZACE_FINAL" else idea.stage

        dao.updateIdea(
            idea.copy(
                approvalsJson = stringListToJson(currentApprovals.toList()),
                rejectionsJson = stringMapToJson(currentRejectionsMap),
                approvalNotesJson = stringMapToJson(approvalNotesMap),
                approvalStatus = newApprovalStatus,
                stage = newStage
            )
        )
    }

    fun getCommentsForIdea(ideaId: Long): Flow<List<ProjectCommentEntity>> = dao.getCommentsForIdea(ideaId)

    suspend fun addProjectComment(ideaId: Long, text: String, imageUri: String?, authorId: String, authorName: String) {
        dao.insertProjectComment(
            ProjectCommentEntity(
                ideaId = ideaId,
                authorId = authorId,
                authorName = authorName,
                text = text,
                imageUri = imageUri
            )
        )
        insertActivityWithLimit(
            ActivityEntity(
                category = "IDEA",
                title = "Nový komentář : NÁPAD",
                description = text.take(100),
                authorId = authorId,
                authorName = authorName
            )
        )
    }

    // Chat & Secret Chat - LIVE - autor nikdy nedostane notifikaci, priorita live
    suspend fun sendSecretChatMessage(senderId: String, senderName: String, content: String, priority: String = "NONE") {
        val msg = ChatMessageEntity(
            senderId = senderId,
            senderName = senderName,
            recipientId = "SECRET",
            content = content,
            priority = priority
        )
        dao.insertChatMessage(msg)
    }

    suspend fun insertRemoteSecretMessage(dto: com.example.data.remote.VjmSecretMessageDto): Boolean {
        val exists = try {
            dao.getAllSecretChatMessagesList().any {
                it.content == dto.text && it.senderName == dto.senderName && kotlin.math.abs(it.timestamp - dto.timestamp) < 3000
            }
        } catch (_: Exception) { false }
        if (exists) return false
        if (dto.senderId == "tata") return false // secret nikdy pro tátu
        val msg = ChatMessageEntity(
            senderId = dto.senderId,
            senderName = dto.senderName,
            recipientId = "SECRET",
            content = dto.text,
            timestamp = dto.timestamp,
            readByJson = "[]"
        )
        dao.insertChatMessage(msg)
        return true
    }

    // Chat
    suspend fun sendChatMessage(
        senderId: String,
        senderName: String,
        content: String,
        imageUri: String? = null,
        attachmentDataUri: String? = null,
        attachmentName: String? = null,
        attachmentMimeType: String? = null,
        priority: String = "NONE"
    ) {
        val readBy = stringListToJson(listOf(senderId))
        val msg = ChatMessageEntity(
            senderId = senderId,
            senderName = senderName,
            recipientId = "GROUP",
            content = content,
            imageUri = imageUri,
            attachmentDataUri = attachmentDataUri,
            attachmentName = attachmentName,
            attachmentMimeType = attachmentMimeType,
            priority = priority,
            readByJson = readBy
        )
        dao.insertChatMessage(msg)
        insertActivityWithLimit(
            ActivityEntity(
                category = "CHAT",
                title = "Zpráva od $senderName",
                description = content.ifEmpty { "[Obrázek]" }.take(50),
                authorId = senderId,
                authorName = senderName,
                priority = priority
            )
        )
    }

    suspend fun insertRemoteChatMessage(
        senderName: String,
        content: String,
        imageUri: String?,
        attachmentDataUri: String?,
        attachmentName: String?,
        attachmentMimeType: String?,
        reactionEmoji: String?,
        timestamp: Long
    ): Boolean {
        val existing = dao.getAllChatMessagesList().find {
            it.content == content &&
                it.senderName == senderName &&
                it.imageUri == imageUri &&
                it.attachmentDataUri == attachmentDataUri &&
                it.attachmentName == attachmentName &&
                it.attachmentMimeType == attachmentMimeType &&
                kotlin.math.abs(it.timestamp - timestamp) < 2000
        }
        if (existing != null) {
            if (existing.reactionEmoji != reactionEmoji) {
                dao.updateChatMessage(existing.copy(reactionEmoji = reactionEmoji))
                return true
            }
            return false
        }
        val msg = ChatMessageEntity(
            senderId = senderName,
            senderName = senderName,
            recipientId = "GROUP",
            content = content,
            imageUri = imageUri,
            attachmentDataUri = attachmentDataUri,
            attachmentName = attachmentName,
            attachmentMimeType = attachmentMimeType,
            reactionEmoji = reactionEmoji,
            timestamp = timestamp,
            readByJson = "[]"
        )
        dao.insertChatMessage(msg)
        insertActivityWithLimit(ActivityEntity(category = "CHAT", title = "Zpráva od $senderName", description = content.ifEmpty { "[Obrázek]" }.take(50), authorId = senderName, authorName = senderName))
        return true
    }

    suspend fun clearChatHistory() {
        dao.deleteAllChatMessages()
    }

    suspend fun clearAllCoreData() {
        dao.deleteAllTaskNotes()
        dao.deleteAllIdeas()
        dao.deleteAllCalendarEvents()
        dao.deleteAllChatMessagesIncludingSecret()
        dao.deleteAllActivities()
    }

    suspend fun markChatMessageRead(message: ChatMessageEntity, userId: String) {
        val readList = parseJsonList(message.readByJson).toMutableSet()
        if (!readList.contains(userId)) {
            readList.add(userId)
            dao.updateChatMessage(message.copy(readByJson = stringListToJson(readList.toList())))
        }
    }

    suspend fun setMessageReaction(message: ChatMessageEntity, emoji: String?) {
        dao.updateChatMessage(message.copy(reactionEmoji = emoji))
    }

    // Calendar
    suspend fun addCalendarEvent(event: CalendarEventEntity) {
        dao.insertCalendarEvent(event)
        val actDesc = if (event.isRecurring && event.recurrenceRule != "Žádné") "Opakování: ${event.recurrenceRule} - ${event.description.take(60)}" else event.description.take(60).ifEmpty { event.title.take(60) }
        insertActivityWithLimit(
            ActivityEntity(
                category = "CALENDAR",
                title = "Kalendář: ${event.title}",
                description = actDesc,
                authorId = event.authorId,
                authorName = event.authorName,
                priority = event.priority
            )
        )
    }

    // pro opakování - bez spamování aktivit (bod 15)
    suspend fun addCalendarEventSilently(event: CalendarEventEntity) {
        dao.insertCalendarEvent(event)
    }

    suspend fun updateCalendarEvent(event: CalendarEventEntity, editorId: String) {
        val existing = dao.getCalendarEventById(event.id) ?: return
        val newEditCount = if (existing.authorId == editorId) existing.editCount + 1 else existing.editCount
        dao.updateCalendarEvent(event.copy(editCount = newEditCount))
    }

    suspend fun deleteCalendarEvent(id: Long) {
        dao.deleteCalendarEvent(id)
    }

    fun getCommentsForCalendarEvent(eventId: Long): Flow<List<CalendarCommentEntity>> =
        dao.getCommentsForCalendarEvent(eventId)

    suspend fun addCalendarComment(eventId: Long, authorId: String, authorName: String, text: String) {
        dao.insertCalendarComment(
            CalendarCommentEntity(
                eventId = eventId,
                authorId = authorId,
                authorName = authorName,
                text = text
            )
        )
        insertActivityWithLimit(
            ActivityEntity(
                category = "CALENDAR",
                title = "Nový komentář : KALENDÁŘ",
                description = text.take(100),
                authorId = authorId,
                authorName = authorName
            )
        )
    }

    suspend fun getLastCalendarComment() = dao.getLastCalendarComment()
    suspend fun getLastTaskNoteComment() = dao.getLastTaskNoteComment()
    suspend fun getLastProjectComment() = dao.getLastProjectComment()

    suspend fun insertRemoteCalendarComment(dto: com.example.data.remote.VjmCalendarCommentDto): Boolean {
        if (dao.getCalendarCommentById(dto.id) != null) return false
        val exists = try {
            dao.getAllCalendarCommentsList().any { it.eventId == dto.eventId && it.authorId == dto.authorId && it.text == dto.text && kotlin.math.abs(it.timestamp - dto.timestamp) < 3000 }
        } catch (_: Exception) { false }
        if (exists) return false
        dao.insertCalendarComment(
            CalendarCommentEntity(
                id = dto.id,
                eventId = dto.eventId,
                authorId = dto.authorId,
                authorName = dto.authorName,
                text = dto.text,
                timestamp = dto.timestamp
            )
        )
        insertActivityWithLimit(
            ActivityEntity(
                category = "CALENDAR",
                title = "Nový komentář : KALENDÁŘ",
                description = dto.text.take(100),
                authorId = dto.authorId,
                authorName = dto.authorName,
                timestamp = dto.timestamp
            )
        )
        return true
    }

    suspend fun insertRemoteTaskComment(dto: com.example.data.remote.VjmTaskCommentDto): Boolean {
        if (dao.getTaskNoteCommentById(dto.id) != null) return false
        val exists = dao.getAllTaskNoteCommentsList().any { it.taskNoteId == dto.taskNoteId && it.authorId == dto.authorId && it.text == dto.text && kotlin.math.abs(it.timestamp - dto.timestamp) < 3000 }
        if (exists) return false
        dao.insertTaskNoteComment(
            TaskNoteCommentEntity(
                id = dto.id,
                taskNoteId = dto.taskNoteId,
                authorId = dto.authorId,
                authorName = dto.authorName,
                text = dto.text,
                timestamp = dto.timestamp
            )
        )
        insertActivityWithLimit(
            ActivityEntity(
                category = "TASK",
                title = "Nový komentář : ÚKOL",
                description = dto.text.take(100),
                authorId = dto.authorId,
                authorName = dto.authorName,
                timestamp = dto.timestamp
            )
        )
        return true
    }

    suspend fun insertRemoteProjectComment(dto: com.example.data.remote.VjmProjectCommentDto): Boolean {
        if (dao.getProjectCommentById(dto.id) != null) return false
        val exists = dao.getAllProjectCommentsList().any { it.ideaId == dto.ideaId && it.authorId == dto.authorId && it.text == dto.text && kotlin.math.abs(it.timestamp - dto.timestamp) < 3000 }
        if (exists) return false
        dao.insertProjectComment(
            ProjectCommentEntity(
                id = dto.id,
                ideaId = dto.ideaId,
                authorId = dto.authorId,
                authorName = dto.authorName,
                text = dto.text,
                imageUri = dto.imageUri,
                timestamp = dto.timestamp
            )
        )
        insertActivityWithLimit(
            ActivityEntity(
                category = "IDEA",
                title = "Nový komentář : NÁPAD",
                description = dto.text.take(100),
                authorId = dto.authorId,
                authorName = dto.authorName,
                timestamp = dto.timestamp
            )
        )
        return true
    }



    suspend fun setUserOnline(userId: String, online: Boolean) {
        val user = dao.getUserById(userId)?: return
        dao.updateUser(user.copy(
            isOnline = online,
            lastSeenTimestamp = if(!online) System.currentTimeMillis() else user.lastSeenTimestamp
        ))
    }

    suspend fun updateUserLastSeen(userId: String, time: Long) {
        dao.getUserById(userId)?.let { dao.updateUser(it.copy(lastSeenTimestamp = time)) }
    }
}
