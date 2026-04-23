package pt.ualg.miaugenda.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import pt.ualg.miaugenda.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private var authInterceptor: AuthInterceptor? = null
    private var retrofitInstance: Retrofit? = null
    private var authApiInstance: AuthApi? = null
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private fun createOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
        
        authInterceptor?.let { builder.addInterceptor(it) }
        
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
    }

    val authApi: AuthApi
        get() {
            if (authApiInstance == null) {
                authApiInstance = getRetrofit().create(AuthApi::class.java)
            }
            return authApiInstance!!
        }
}
