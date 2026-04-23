package pt.ualg.miaugenda.data.remote

import pt.ualg.miaugenda.data.model.LoginRequest
import pt.ualg.miaugenda.data.model.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
}
