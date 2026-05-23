package pt.ualg.miaugenda.data.remote

import pt.ualg.miaugenda.data.model.TimeOffRequest
import pt.ualg.miaugenda.data.model.ShiftSwapRequest
import pt.ualg.miaugenda.data.model.CreateTimeOffRequestBody
import pt.ualg.miaugenda.data.model.CreateShiftSwapRequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface RequestApi {

    @GET("/time-off-requests")
    suspend fun getTimeOffRequests(): Response<List<TimeOffRequest>>

    @POST("/time-off-requests")
    suspend fun createTimeOffRequest(
        @Body body: CreateTimeOffRequestBody
    ): Response<TimeOffRequest>

    @PATCH("/time-off-requests/{id}/status")
    suspend fun updateTimeOffRequestStatus(
        @Path("id") id: Int,
        @Body body: Map<String, String>
    ): Response<TimeOffRequest>

    @DELETE("/time-off-requests/{id}")
    suspend fun deleteTimeOffRequest(
        @Path("id") id: Int
    ): Response<Unit>

    @GET("/shift-swap-requests")
    suspend fun getShiftSwapRequests(): Response<List<ShiftSwapRequest>>

    @POST("/shift-swap-requests")
    suspend fun createShiftSwapRequest(
        @Body body: CreateShiftSwapRequestBody
    ): Response<ShiftSwapRequest>

    @PATCH("/shift-swap-requests/{id}/status")
    suspend fun updateShiftSwapRequestStatus(
        @Path("id") id: Int,
        @Body body: Map<String, String>
    ): Response<ShiftSwapRequest>

    @PATCH("/shift-swap-requests/{id}/target-response")
    suspend fun respondToSwapRequest(
        @Path("id") id: Int,
        @Body body: Map<String, Boolean>
    ): Response<ShiftSwapRequest>

    @DELETE("/shift-swap-requests/{id}")
    suspend fun deleteShiftSwapRequest(
        @Path("id") id: Int
    ): Response<Unit>
}
