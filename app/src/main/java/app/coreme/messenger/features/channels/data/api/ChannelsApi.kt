package app.coreme.messenger.features.channels.data.api

import app.coreme.messenger.features.channels.data.dto.ChannelDto
import app.coreme.messenger.features.channels.data.dto.ChannelPostDto
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ChannelsApi {
    @GET("api/channels")
    suspend fun getChannels(): List<ChannelDto>

    @GET("api/channels/{channelId}/posts")
    suspend fun getChannelPosts(@Path("channelId") channelId: String): List<ChannelPostDto>

    @POST("api/channels/{channelId}/subscribe")
    suspend fun subscribe(@Path("channelId") channelId: String)

    @DELETE("api/channels/{channelId}/subscribe")
    suspend fun unsubscribe(@Path("channelId") channelId: String)
}
