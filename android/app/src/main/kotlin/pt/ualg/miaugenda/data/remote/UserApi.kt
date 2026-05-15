package pt.ualg.miaugenda.data.remote

import pt.ualg.miaugenda.data.model.CreateUserRequest
import pt.ualg.miaugenda.data.model.UpdateProfileRequest
import pt.ualg.miaugenda.data.model.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface UserApi {

    @GET("users")
    suspend fun getUsers(): Response<List<User>>

    @GET("users/me")
    suspend fun getMe(): Response<User>

    @PATCH("users/me")
    suspend fun updateMe(
        @Body request: UpdateProfileRequest
    ): Response<User>

    @POST("users")
    suspend fun createUser(
        @Body request: CreateUserRequest
    ): Response<User>

    @PATCH("users/{id}/deactivate")
    suspend fun deactivateUser(
        @Path("id") id: Int
    ): Response<User>
}