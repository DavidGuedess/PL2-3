package pt.ualg.miaugenda.data.remote

import pt.ualg.miaugenda.data.model.Availability
import pt.ualg.miaugenda.data.model.CreateAvailabilityBody
import retrofit2.Response
import retrofit2.http.*

interface AvailabilityApi {
    @GET("/availability")
    suspend fun getAvailabilities(
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("userId") userId: Int? = null
    ): Response<List<Availability>>

    @POST("/availability")
    suspend fun createAvailability(@Body body: CreateAvailabilityBody): Response<Availability>

    @DELETE("/availability/{id}")
    suspend fun deleteAvailability(@Path("id") id: Int): Response<Unit>
}
