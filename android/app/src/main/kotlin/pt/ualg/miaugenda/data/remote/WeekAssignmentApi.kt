package pt.ualg.miaugenda.data.remote

import pt.ualg.miaugenda.data.model.CreateWeekAssignmentRequest
import pt.ualg.miaugenda.data.model.WeekAssignment
import retrofit2.Response
import retrofit2.http.*

interface WeekAssignmentApi {
    @GET("/week-assignments")
    suspend fun getWeekAssignments(@Query("week") week: String): Response<List<WeekAssignment>>

    @POST("/week-assignments")
    suspend fun createWeekAssignment(@Body request: CreateWeekAssignmentRequest): Response<WeekAssignment>

    @DELETE("/week-assignments/{id}")
    suspend fun deleteWeekAssignment(@Path("id") id: Int): Response<Unit>
}
