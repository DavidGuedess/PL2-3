package com.miaugenda.data.api

import com.miaugenda.data.model.AttendanceRecord
import com.miaugenda.data.model.LoginRequest
import com.miaugenda.data.model.LoginResponse
import com.miaugenda.data.model.Shift
import com.miaugenda.data.model.ShiftType
import com.miaugenda.data.model.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AuthService {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/auth/logout")
    suspend fun logout(): Response<Unit>
}

interface UserService {
    @GET("users/me")
    suspend fun getMe(): Response<User>

    @PATCH("users/me")
    suspend fun updateMe(@Body body: Map<String, String>): Response<User>
}

interface ShiftService {
    @GET("shift-types")
    suspend fun getShiftTypes(): Response<List<ShiftType>>

    @POST("shifts")
    suspend fun createShift(@Body body: Map<String, Any>): Response<Shift>

    @GET("shifts")
    suspend fun getShifts(
        @Query("week") week: String? = null,
        @Query("month") month: String? = null
    ): Response<List<Shift>>
}

interface AttendanceService {
    @POST("attendance")
    suspend fun register(@Body body: Map<String, String>): Response<AttendanceRecord>

    @GET("attendance/me")
    suspend fun getMyHistory(): Response<List<AttendanceRecord>>
}
