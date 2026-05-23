package pt.ualg.miaugenda.data.remote

import pt.ualg.miaugenda.data.model.AttendanceRecord
import pt.ualg.miaugenda.data.model.CreateAttendanceBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AttendanceApi {
    @POST("/attendance")
    suspend fun register(
        @Body body: CreateAttendanceBody
    ): Response<AttendanceRecord>

    @GET("/attendance/me")
    suspend fun getMyHistory(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): Response<List<AttendanceRecord>>

    @GET("/attendance")
    suspend fun getAttendance(
        @Query("userId") userId: Int? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): Response<List<AttendanceRecord>>
}