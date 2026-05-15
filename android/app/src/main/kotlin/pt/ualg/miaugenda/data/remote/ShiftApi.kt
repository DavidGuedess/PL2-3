package pt.ualg.miaugenda.data.remote

import pt.ualg.miaugenda.data.model.Shift
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ShiftApi {
    @GET("/shifts")
    suspend fun getMyShifts(
        @Query("week") week: String? = null
    ): Response<List<Shift>>

    @GET("/shifts/me")
    suspend fun getMyOwnShifts(
        @Query("week") week: String? = null
    ): Response<List<Shift>>
}