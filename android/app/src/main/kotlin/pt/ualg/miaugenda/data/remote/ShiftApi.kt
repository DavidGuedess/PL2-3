package pt.ualg.miaugenda.data.remote

import pt.ualg.miaugenda.data.model.Shift
import pt.ualg.miaugenda.data.model.ShiftType
import pt.ualg.miaugenda.data.model.CreateShiftRequest
import pt.ualg.miaugenda.data.model.UpdateShiftRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ShiftApi {

    @GET("/shifts")
    suspend fun getShifts(
        @Query("week") week: String? = null,
        @Query("month") month: String? = null
    ): Response<List<Shift>>

    @GET("/shifts/me")
    suspend fun getMyShifts(
        @Query("week") week: String? = null,
        @Query("month") month: String? = null
    ): Response<List<Shift>>

    @POST("/shifts")
    suspend fun createShift(
        @Body request: CreateShiftRequest
    ): Response<Shift>

    @PATCH("/shifts/{id}")
    suspend fun updateShift(
        @Path("id") id: Int,
        @Body request: UpdateShiftRequest
    ): Response<Shift>

    @DELETE("/shifts/{id}")
    suspend fun deleteShift(
        @Path("id") id: Int
    ): Response<Unit>

    @GET("/shift-types")
    suspend fun getShiftTypes(): Response<List<ShiftType>>
}
