package pt.ualg.miaugenda.data.remote

import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import pt.ualg.miaugenda.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private var authInterceptor: AuthInterceptor? = null
    private var retrofitInstance: Retrofit? = null
    private var authApiInstance: AuthApi? = null
    private var shiftApiInstance: ShiftApi? = null
    private var attendanceApiInstance: AttendanceApi? = null
    private var userApiInstance: UserApi? = null
    private var weekAssignmentApiInstance: WeekAssignmentApi? = null
    private var requestApiInstance: RequestApi? = null
    private var availabilityApiInstance: AvailabilityApi? = null
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private fun createOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .protocols(listOf(Protocol.HTTP_1_1))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        authInterceptor?.let { builder.addInterceptor(it) }

        builder.addInterceptor(loggingInterceptor)

        return builder.build()
    }

    private fun getRetrofit(): Retrofit {
        if (retrofitInstance == null) {
            retrofitInstance = Retrofit.Builder()
                .baseUrl(BuildConfig.API_BASE_URL)
                .client(createOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofitInstance!!
    }

    fun setAuthInterceptor(interceptor: AuthInterceptor) {
        authInterceptor = interceptor
        // Reset instances para recriar com novo interceptor
        retrofitInstance = null
        authApiInstance = null
        shiftApiInstance = null
        attendanceApiInstance = null
        userApiInstance = null
        weekAssignmentApiInstance = null
        requestApiInstance = null
        availabilityApiInstance = null
    }

    val authApi: AuthApi
        get() {
            if (authApiInstance == null) {
                authApiInstance = getRetrofit().create(AuthApi::class.java)
            }
            return authApiInstance!!
        }

    val shiftApi: ShiftApi
        get() {
            if (shiftApiInstance == null) {
                shiftApiInstance = getRetrofit().create(ShiftApi::class.java)
            }
            return shiftApiInstance!!
        }

    val attendanceApi: AttendanceApi
        get() {
            if (attendanceApiInstance == null) {
                attendanceApiInstance = getRetrofit().create(AttendanceApi::class.java)
            }
            return attendanceApiInstance!!
        }

    val userApi: UserApi
        get() {
            if (userApiInstance == null) {
                userApiInstance = getRetrofit().create(UserApi::class.java)
            }
            return userApiInstance!!
        }

    val weekAssignmentApi: WeekAssignmentApi
        get() {
            if (weekAssignmentApiInstance == null) {
                weekAssignmentApiInstance = getRetrofit().create(WeekAssignmentApi::class.java)
            }
            return weekAssignmentApiInstance!!
        }

    val requestApi: RequestApi
        get() {
            if (requestApiInstance == null) {
                requestApiInstance = getRetrofit().create(RequestApi::class.java)
            }
            return requestApiInstance!!
        }

    val availabilityApi: AvailabilityApi
        get() {
            if (availabilityApiInstance == null) {
                availabilityApiInstance = getRetrofit().create(AvailabilityApi::class.java)
            }
            return availabilityApiInstance!!
        }
}
