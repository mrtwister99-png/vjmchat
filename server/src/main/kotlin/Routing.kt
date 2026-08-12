package com.example

import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.origin
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Collections

@Serializable
data class VjmActivity(val id: Long = 0, val user: String, val text: String, val timestamp: Long = System.currentTimeMillis())
@Serializable
data class VjmMessage(
    val id: Long = 0,
    val sender: String,
    val text: String,
    val imageUri: String? = null,
    val attachmentDataUri: String? = null,
    val attachmentName: String? = null,
    val attachmentMimeType: String? = null,
    val reactionEmoji: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
@Serializable
data class MessageReactionUpdate(
    val sender: String,
    val text: String,
    val timestamp: Long,
    val reactionEmoji: String? = null
)
@Serializable
data class VjmTask(val id: Long = 0, val type: String = "TASK", val title: String, val content: String = "", val colorHex: String = "#1E293B", val authorId: String = "", val authorName: String = "", val priority: String = "NONE", val isCompleted: Boolean = false, val createdAt: Long = System.currentTimeMillis())
@Serializable
data class VjmIdea(val id: Long = 0, val title: String, val description: String = "", val stage: String = "LIST", val authorId: String = "", val authorName: String = "", val priority: String = "NONE", val starsJson: String = "[]", val crownsJson: String = "[]", val potentialsJson: String = "{}", val createdAt: Long = System.currentTimeMillis())
@Serializable
data class VjmCalendar(val id: Long = 0, val title: String, val description: String = "", val startDateTimestamp: Long, val endDateTimestamp: Long, val authorId: String = "", val authorName: String = "", val priority: String = "NONE", val createdAt: Long = System.currentTimeMillis())
@Serializable
data class VjmUser(val id: String, val pin: String, val nickname: String = "", val defaultName: String = "", val isOnline: Boolean = false)
@Serializable
data class VjmSecretMessage(val id: Long = 0, val senderId: String, val senderName: String, val text: String, val timestamp: Long = System.currentTimeMillis())
@Serializable
data class VjmCalendarComment(val id: Long = 0, val eventId: Long, val authorId: String, val authorName: String, val text: String, val timestamp: Long = System.currentTimeMillis())
@Serializable
data class VjmTaskComment(val id: Long = 0, val taskNoteId: Long, val authorId: String, val authorName: String, val text: String, val timestamp: Long = System.currentTimeMillis())
@Serializable
data class VjmProjectComment(val id: Long = 0, val ideaId: Long, val authorId: String, val authorName: String, val text: String, val imageUri: String? = null, val timestamp: Long = System.currentTimeMillis())
@Serializable
data class UploadResponse(val url: String, val fileName: String, val mimeType: String)

@Serializable
data class ServerState(
    val activities: List<VjmActivity> = emptyList(),
    val messages: List<VjmMessage> = emptyList(),
    val tasks: List<VjmTask> = emptyList(),
    val ideas: List<VjmIdea> = emptyList(),
    val calendars: List<VjmCalendar> = emptyList(),
    val secretMessages: List<VjmSecretMessage> = emptyList(),
    val calendarComments: List<VjmCalendarComment> = emptyList(),
    val taskComments: List<VjmTaskComment> = emptyList(),
    val projectComments: List<VjmProjectComment> = emptyList(),
    val users: List<VjmUser> = defaultUsers()
)

private val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    encodeDefaults = true
}

private val stateDir = File("server-data").apply { mkdirs() }
private val stateFile = File(stateDir, "state.json")
private val uploadDir = File("uploads").apply { mkdirs() }
private val initialState = loadServerState()

private val activities = Collections.synchronizedList(initialState.activities.toMutableList())
private val messages = Collections.synchronizedList(initialState.messages.toMutableList())
private val tasks = Collections.synchronizedList(initialState.tasks.toMutableList())
private val ideas = Collections.synchronizedList(initialState.ideas.toMutableList())
private val calendars = Collections.synchronizedList(initialState.calendars.toMutableList())
private val secretMessages = Collections.synchronizedList(initialState.secretMessages.toMutableList())
private val calendarComments = Collections.synchronizedList(initialState.calendarComments.toMutableList())
private val taskComments = Collections.synchronizedList(initialState.taskComments.toMutableList())
private val projectComments = Collections.synchronizedList(initialState.projectComments.toMutableList())
private val users = Collections.synchronizedList(
    mergeUsersWithDefaults(initialState.users).toMutableList()
)
private val wsSessions = Collections.synchronizedSet(mutableSetOf<DefaultWebSocketServerSession>())

private fun defaultUsers(): List<VjmUser> = listOf(
    VjmUser(id = "admin", pin = "2242", nickname = "Tom", defaultName = "Tom", isOnline = false),
    VjmUser(id = "kamaradka", pin = "2221", nickname = "Adélka", defaultName = "Adélka", isOnline = false),
    VjmUser(id = "tata", pin = "3331", nickname = "Michal", defaultName = "Michal", isOnline = false)
)

private fun mergeUsersWithDefaults(savedUsers: List<VjmUser>): List<VjmUser> {
    val byId = savedUsers.associateBy { it.id }.toMutableMap()
    defaultUsers().forEach { fallback ->
        val existing = byId[fallback.id]
        byId[fallback.id] = if (existing == null) fallback else fallback.copy(
            pin = existing.pin,
            nickname = existing.nickname.ifBlank { fallback.nickname },
            defaultName = existing.defaultName.ifBlank { fallback.defaultName },
            isOnline = existing.isOnline
        )
    }
    return byId.values.sortedBy { it.id }
}

private fun loadServerState(): ServerState {
    if (!stateFile.exists()) return ServerState()
    return try {
        json.decodeFromString<ServerState>(stateFile.readText())
    } catch (_: Exception) {
        ServerState()
    }
}

@Synchronized
private fun persistServerState() {
    val snapshot = ServerState(
        activities = synchronized(activities) { activities.toList() },
        messages = synchronized(messages) { messages.toList() },
        tasks = synchronized(tasks) { tasks.toList() },
        ideas = synchronized(ideas) { ideas.toList() },
        calendars = synchronized(calendars) { calendars.toList() },
        secretMessages = synchronized(secretMessages) { secretMessages.toList() },
        calendarComments = synchronized(calendarComments) { calendarComments.toList() },
        taskComments = synchronized(taskComments) { taskComments.toList() },
        projectComments = synchronized(projectComments) { projectComments.toList() },
        users = synchronized(users) { users.toList() }
    )
    stateFile.writeText(json.encodeToString(ServerState.serializer(), snapshot))
}

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("VJM Chat server běží ✅")
        }

        get("/api/activities") { call.respond(activities.takeLast(20).reversed()) }
        post("/api/activities") {
            val req = call.receive<VjmActivity>()
            val act = req.copy(id = System.currentTimeMillis())
            activities.add(act)
            if (activities.size > 20) activities.removeAt(0)
            persistServerState()
            call.respond(act)
        }

        get("/api/messages") { call.respond(messages.takeLast(2000)) }
        get("/api/messages/unread-count") { call.respond(mapOf("count" to messages.size)) }
        post("/api/messages") {
            val req = call.receive<VjmMessage>()
            val msg = req.copy(id = System.currentTimeMillis())
            messages.add(msg)
            if (messages.size > 2000) messages.removeAt(0)
            persistServerState()
            val json = Json.encodeToString(VjmMessage.serializer(), msg)
            wsSessions.forEach { try { it.send(Frame.Text(json)) } catch (_: Exception) {} }
            call.respond(msg)
        }
        post("/api/messages/reaction") {
            val req = call.receive<MessageReactionUpdate>()
            val index = messages.indexOfFirst {
                it.sender == req.sender &&
                    it.text == req.text &&
                    kotlin.math.abs(it.timestamp - req.timestamp) < 2000
            }
            if (index == -1) {
                call.respond(mapOf("ok" to false))
                return@post
            }
            val updated = messages[index].copy(reactionEmoji = req.reactionEmoji)
            messages[index] = updated
            persistServerState()
            val json = Json.encodeToString(VjmMessage.serializer(), updated)
            wsSessions.forEach { try { it.send(Frame.Text(json)) } catch (_: Exception) {} }
            call.respond(mapOf("ok" to true))
        }
        post("/api/uploads") {
            var savedFile: File? = null
            var savedName = "upload.bin"
            var mimeType = "application/octet-stream"

            call.receiveMultipart().forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        val originalName = sanitizeFileName(part.originalFileName ?: "upload.bin")
                        savedName = "${System.currentTimeMillis()}_$originalName"
                        val targetFile = File(uploadDir, savedName)
                        @Suppress("DEPRECATION")
                        val streamProvider = part.streamProvider
                        streamProvider().use { input ->
                            targetFile.outputStream().buffered().use { output ->
                                input.copyTo(output)
                            }
                        }
                        savedFile = targetFile
                        mimeType = part.contentType?.toString() ?: mimeType
                    }
                    else -> Unit
                }
                part.release()
            }

            val file = savedFile
            if (file == null || !file.exists()) {
                call.respond(mapOf("ok" to false, "error" to "upload failed"))
                return@post
            }

            val origin = call.request.origin
            val portPart = if ((origin.scheme == "http" && origin.serverPort == 80) || (origin.scheme == "https" && origin.serverPort == 443)) "" else ":${origin.serverPort}"
            call.respond(
                UploadResponse(
                    url = "${origin.scheme}://${origin.serverHost}$portPart/uploads/$savedName",
                    fileName = file.name.substringAfter('_', file.name),
                    mimeType = mimeType
                )
            )
        }
        get("/uploads/{name}") {
            val name = call.parameters["name"] ?: return@get call.respondText("missing", status = io.ktor.http.HttpStatusCode.BadRequest)
            val target = File(uploadDir, sanitizeFileName(name)).canonicalFile
            if (!target.exists() || !target.path.startsWith(uploadDir.canonicalPath)) {
                call.respondText("not found", status = io.ktor.http.HttpStatusCode.NotFound)
                return@get
            }
            call.respondFile(target)
        }
        post("/api/messages/clear") { messages.clear(); persistServerState(); call.respond(mapOf("ok" to true)) }

        get("/api/tasks") { call.respond(tasks) }
        post("/api/tasks") { val t = call.receive<VjmTask>(); tasks.removeIf { it.id == t.id }; val saved = t.copy(id = if (t.id == 0L) System.currentTimeMillis() else t.id); tasks.add(saved); persistServerState(); call.respond(saved) }
        post("/api/tasks/delete/{id}") { val id = call.parameters["id"]?.toLongOrNull() ?: 0L; tasks.removeIf { it.id == id }; persistServerState(); call.respond(mapOf("ok" to true)) }

        get("/api/ideas") { call.respond(ideas) }
        post("/api/ideas") { val i = call.receive<VjmIdea>(); ideas.removeIf { it.id == i.id }; val saved = i.copy(id = if (i.id == 0L) System.currentTimeMillis() else i.id); ideas.add(saved); persistServerState(); call.respond(saved) }
        post("/api/ideas/delete/{id}") { val id = call.parameters["id"]?.toLongOrNull() ?: 0L; ideas.removeIf { it.id == id }; persistServerState(); call.respond(mapOf("ok" to true)) }

        get("/api/calendar") { call.respond(calendars) }
        post("/api/calendar") { val c = call.receive<VjmCalendar>(); calendars.removeIf { it.id == c.id }; val saved = c.copy(id = if (c.id == 0L) System.currentTimeMillis() else c.id); calendars.add(saved); persistServerState(); call.respond(saved) }
        post("/api/calendar/delete/{id}") { val id = call.parameters["id"]?.toLongOrNull() ?: 0L; calendars.removeIf { it.id == id }; persistServerState(); call.respond(mapOf("ok" to true)) }

        get("/api/calendar-comments") { call.respond(calendarComments.sortedBy { it.timestamp }) }
        post("/api/calendar-comments") {
            val c = call.receive<VjmCalendarComment>()
            val saved = c.copy(id = if (c.id == 0L) System.currentTimeMillis() else c.id)
            calendarComments.removeIf { it.id == saved.id }
            calendarComments.add(saved)
            persistServerState()
            call.respond(saved)
        }

        get("/api/task-comments") { call.respond(taskComments.sortedBy { it.timestamp }) }
        post("/api/task-comments") {
            val c = call.receive<VjmTaskComment>()
            val saved = c.copy(id = if (c.id == 0L) System.currentTimeMillis() else c.id)
            taskComments.removeIf { it.id == saved.id }
            taskComments.add(saved)
            persistServerState()
            call.respond(saved)
        }

        get("/api/project-comments") { call.respond(projectComments.sortedBy { it.timestamp }) }
        post("/api/project-comments") {
            val c = call.receive<VjmProjectComment>()
            val saved = c.copy(id = if (c.id == 0L) System.currentTimeMillis() else c.id)
            projectComments.removeIf { it.id == saved.id }
            projectComments.add(saved)
            persistServerState()
            call.respond(saved)
        }

        get("/api/users") { call.respond(users) }
        post("/api/users/pin") {
            val u = call.receive<VjmUser>()
            val existing = users.find { it.id == u.id }
            if (existing != null) {
                users.remove(existing)
                users.add(
                    existing.copy(
                        pin = u.pin,
                        nickname = if (u.nickname.isNotBlank()) u.nickname else existing.nickname,
                        defaultName = if (u.defaultName.isNotBlank()) u.defaultName else existing.defaultName,
                        isOnline = u.isOnline
                    )
                )
            } else {
                users.add(u)
            }
            persistServerState()
            call.respond(mapOf("ok" to true))
        }

        post("/api/users/online") {
            val u = call.receive<VjmUser>()
            val existing = users.find { it.id == u.id }
            if (existing != null) {
                users.remove(existing)
                users.add(
                    existing.copy(
                        isOnline = u.isOnline,
                        nickname = if (u.nickname.isNotBlank()) u.nickname else existing.nickname,
                        defaultName = if (u.defaultName.isNotBlank()) u.defaultName else existing.defaultName
                    )
                )
            } else {
                users.add(u)
            }
            persistServerState()
            call.respond(mapOf("ok" to true))
        }

        // SECRET CHAT - LIVE, jen pro admin a kamaradka (Tom a Adélka), tata nikdy
        get("/api/secret-messages") { 
            // filtruj jen pokud chceš, ale necháme vše - klient si filtruje podle id
            call.respond(secretMessages.takeLast(500)) 
        }
        post("/api/secret-messages") {
            val req = call.receive<VjmSecretMessage>()
            // tata nesmí posílat secret
            if (req.senderId == "tata") {
                call.respond(mapOf("ok" to false, "error" to "tata not allowed"))
                return@post
            }
            val msg = req.copy(id = System.currentTimeMillis())
            secretMessages.add(msg)
            if (secretMessages.size > 500) secretMessages.removeAt(0)
            persistServerState()
            call.respond(msg)
        }

        webSocket("/ws") {
            wsSessions.add(this)
            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        if (text.equals("bye", ignoreCase = true)) {
                            close(CloseReason(CloseReason.Codes.NORMAL, "Client said BYE"))
                        }
                    }
                }
            } finally { wsSessions.remove(this) }
        }
    }
}

private fun sanitizeFileName(name: String): String =
    name.replace("..", "").replace(Regex("[^A-Za-z0-9._-]"), "_")