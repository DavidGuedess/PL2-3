package pt.ualg.miaugenda.data.remote

import pt.ualg.miaugenda.data.model.Channel
import pt.ualg.miaugenda.data.model.ChannelMessage
import pt.ualg.miaugenda.data.model.CreateChannelRequest
import pt.ualg.miaugenda.data.model.SendMessageRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface MessagingApi {

    @GET("channels")
    suspend fun getChannels(): Response<List<Channel>>

    @POST("channels")
    suspend fun createChannel(
        @Body request: CreateChannelRequest
    ): Response<Channel>

    @GET("channels/{id}/messages")
    suspend fun getMessages(
        @Path("id") channelId: Int,
        @Query("limit") limit: Int = 50,
        @Query("before") before: Int? = null
    ): Response<List<ChannelMessage>>

    @POST("channels/{id}/messages")
    suspend fun sendMessage(
        @Path("id") channelId: Int,
        @Body request: SendMessageRequest
    ): Response<ChannelMessage>

    @DELETE("channels/{id}")
    suspend fun deleteChannel(@Path("id") id: Int): Response<Unit>
}
