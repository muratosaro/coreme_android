package app.coreme.messenger.features.chats.data.api

import app.coreme.messenger.features.chats.data.dto.AddReactionRequest
import app.coreme.messenger.features.chats.data.dto.ChatDto
import app.coreme.messenger.features.chats.data.dto.CreateDirectChatRequest
import app.coreme.messenger.features.chats.data.dto.CreateGroupChatRequest
import app.coreme.messenger.features.chats.data.dto.EditMessageRequest
import app.coreme.messenger.features.chats.data.dto.MemberDto
import app.coreme.messenger.features.chats.data.dto.MessageDto
import app.coreme.messenger.features.chats.data.dto.SendMessageRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatsApi {

    @GET("api/chats")
    suspend fun getChats(): List<ChatDto>

    @GET("api/chats/{id}")
    suspend fun getChatById(@Path("id") id: String): ChatDto

    @POST("api/chats")
    suspend fun createDirectChat(@Body request: CreateDirectChatRequest): ChatDto

    @POST("api/chats/group")
    suspend fun createGroupChat(@Body request: CreateGroupChatRequest): ChatDto

    @GET("api/chats/{id}/messages")
    suspend fun getMessages(
        @Path("id") chatId: String,
        @Query("limit") limit: Int = 50,
        @Query("before") before: String? = null,
    ): List<MessageDto>

    @POST("api/chats/{id}/messages")
    suspend fun sendMessage(
        @Path("id") chatId: String,
        @Body request: SendMessageRequest,
    ): MessageDto

    @PATCH("api/chats/{id}/messages/{msgId}")
    suspend fun editMessage(
        @Path("id") chatId: String,
        @Path("msgId") msgId: String,
        @Body request: EditMessageRequest,
    ): MessageDto

    @DELETE("api/chats/{id}/messages/{msgId}")
    suspend fun deleteMessage(
        @Path("id") chatId: String,
        @Path("msgId") msgId: String,
        @Query("forAll") forAll: Boolean = true,
    )

    @GET("api/chats/{id}/members")
    suspend fun getMembers(@Path("id") chatId: String): List<MemberDto>

    @POST("api/chats/{id}/messages/{msgId}/reactions")
    suspend fun addReaction(
        @Path("id") chatId: String,
        @Path("msgId") msgId: String,
        @Body request: AddReactionRequest,
    )

    @DELETE("api/chats/{id}/messages/{msgId}/reactions/{emoji}")
    suspend fun removeReaction(
        @Path("id") chatId: String,
        @Path("msgId") msgId: String,
        @Path("emoji") emoji: String,
    )
}
