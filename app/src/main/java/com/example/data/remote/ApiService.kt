package com.example.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Part
import okhttp3.MultipartBody
import com.squareup.moshi.JsonClass

object ApiConfig {
    const val BASE_URL = "https://vjmchat-production.up.railway.app/"
    const val WS_URL = "wss://vjmchat-production.up.railway.app/ws"
}

@JsonClass(generateAdapter = true)
data class VjmMessageDto(
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
@JsonClass(generateAdapter = true)
data class MessageReactionUpdateDto(
    val sender: String,
    val text: String,
    val timestamp: Long,
    val reactionEmoji: String? = null
)
@JsonClass(generateAdapter = true)
data class VjmActivityDto(val id: Long = 0, val user: String, val text: String, val timestamp: Long = System.currentTimeMillis(), val likedByIds: String = "")
@JsonClass(generateAdapter = true)
data class VjmActivityLikeDto(val id: Long, val likedByIds: String)
@JsonClass(generateAdapter = true)
data class VjmTaskDto(val id: Long = 0, val type: String = "TASK", val title: String, val content: String = "", val colorHex: String = "#1E293B", val authorId: String = "", val authorName: String = "", val priority: String = "NONE", val isCompleted: Boolean = false, val createdAt: Long = System.currentTimeMillis())
@JsonClass(generateAdapter = true)
data class VjmIdeaDto(val id: Long = 0, val title: String, val description: String = "", val stage: String = "LIST", val authorId: String = "", val authorName: String = "", val priority: String = "NONE", val starsJson: String = "[]", val crownsJson: String = "[]", val potentialsJson: String = "{}", val createdAt: Long = System.currentTimeMillis())
@JsonClass(generateAdapter = true)
data class VjmCalendarDto(val id: Long = 0, val title: String, val description: String = "", val colorCategoryHex: String = "#00c43b", val startDateTimestamp: Long, val endDateTimestamp: Long, val authorId: String = "", val authorName: String = "", val priority: String = "NONE", val createdAt: Long = System.currentTimeMillis())
@JsonClass(generateAdapter = true)
data class VjmUserDto(val id: String, val pin: String, val nickname: String = "", val defaultName: String = "", val isOnline: Boolean = false)
@JsonClass(generateAdapter = true)
data class VjmSecretMessageDto(val id: Long = 0, val senderId: String, val senderName: String, val text: String, val timestamp: Long = System.currentTimeMillis(), val priority: String = "NONE")

@JsonClass(generateAdapter = true)
data class VjmCalendarCommentDto(val id: Long = 0, val eventId: Long, val authorId: String, val authorName: String, val text: String, val timestamp: Long = System.currentTimeMillis())

@JsonClass(generateAdapter = true)
data class VjmTaskCommentDto(val id: Long = 0, val taskNoteId: Long, val authorId: String, val authorName: String, val text: String, val timestamp: Long = System.currentTimeMillis())

@JsonClass(generateAdapter = true)
data class VjmProjectCommentDto(val id: Long = 0, val ideaId: Long, val authorId: String, val authorName: String, val text: String, val imageUri: String? = null, val timestamp: Long = System.currentTimeMillis())

@JsonClass(generateAdapter = true)
data class UploadResponseDto(val url: String, val fileName: String, val mimeType: String)

interface ApiService {
    @GET("api/messages") suspend fun getMessages(): List<VjmMessageDto>
    @POST("api/messages") suspend fun sendMessage(@Body msg: VjmMessageDto): VjmMessageDto
    @POST("api/messages/reaction") suspend fun updateMessageReaction(@Body update: MessageReactionUpdateDto): Map<String, Boolean>
    @Multipart
    @POST("api/uploads") suspend fun uploadChatFile(@Part file: MultipartBody.Part): UploadResponseDto
    @POST("api/messages/clear") suspend fun clearMessages(): Map<String, Boolean>
    @GET("api/activities") suspend fun getActivities(): List<VjmActivityDto>
    @POST("api/activities") suspend fun sendActivity(@Body act: VjmActivityDto): VjmActivityDto
    @POST("api/activities/like") suspend fun sendActivityLike(@Body dto: VjmActivityLikeDto): Map<String, Boolean>
    @GET("api/activities/likes") suspend fun getActivityLikes(): List<VjmActivityLikeDto>
    @GET("api/tasks") suspend fun getTasks(): List<VjmTaskDto>
    @POST("api/tasks") suspend fun sendTask(@Body task: VjmTaskDto): VjmTaskDto
    @POST("api/tasks/delete/{id}") suspend fun deleteTask(@Path("id") id: Long): Map<String, Boolean>
    @GET("api/ideas") suspend fun getIdeas(): List<VjmIdeaDto>
    @POST("api/ideas") suspend fun sendIdea(@Body idea: VjmIdeaDto): VjmIdeaDto
    @POST("api/ideas/delete/{id}") suspend fun deleteIdea(@Path("id") id: Long): Map<String, Boolean>
    @GET("api/calendar") suspend fun getCalendar(): List<VjmCalendarDto>
    @POST("api/calendar") suspend fun sendCalendar(@Body cal: VjmCalendarDto): VjmCalendarDto
    @POST("api/calendar/delete/{id}") suspend fun deleteCalendar(@Path("id") id: Long): Map<String, Boolean>
    @GET("api/users") suspend fun getUsers(): List<VjmUserDto>
    @POST("api/users/pin") suspend fun updateUserPin(@Body user: VjmUserDto): Map<String, Boolean>
    @POST("api/users/online") suspend fun updateUserOnline(@Body user: VjmUserDto): Map<String, Boolean>
    // SECRET CHAT - LIVE
    @GET("api/secret-messages") suspend fun getSecretMessages(): List<VjmSecretMessageDto>
    @POST("api/secret-messages") suspend fun sendSecretMessage(@Body msg: VjmSecretMessageDto): VjmSecretMessageDto

    // CALENDAR COMMENTS - LIVE VARIANTA 2
    @GET("api/calendar-comments") suspend fun getCalendarComments(): List<VjmCalendarCommentDto>
    @POST("api/calendar-comments") suspend fun sendCalendarComment(@Body comment: VjmCalendarCommentDto): VjmCalendarCommentDto

    @GET("api/task-comments") suspend fun getTaskComments(): List<VjmTaskCommentDto>
    @POST("api/task-comments") suspend fun sendTaskComment(@Body comment: VjmTaskCommentDto): VjmTaskCommentDto

    @GET("api/project-comments") suspend fun getProjectComments(): List<VjmProjectCommentDto>
    @POST("api/project-comments") suspend fun sendProjectComment(@Body comment: VjmProjectCommentDto): VjmProjectCommentDto
}