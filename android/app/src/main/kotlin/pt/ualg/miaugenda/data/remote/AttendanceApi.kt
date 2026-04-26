package pt.ualg.miaugenda.data.remote

import pt.ualg.miaugenda.data.model.AttendanceRecord
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AttendanceApi {
    @POST("/attendance")
    suspend fun register(
        @Body body: Map<String, String>
    ): Response<AttendanceRecord>

    @GET("/attendance/me")
    suspend fun getMyHistory(): Response<List<AttendanceRecord>>
}