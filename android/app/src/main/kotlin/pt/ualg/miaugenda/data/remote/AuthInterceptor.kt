package pt.ualg.miaugenda.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import pt.ualg.miaugenda.data.local.TokenManager

class AuthInterceptor(
    private val tokenManager: TokenManager
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        val token = tokenManager.getAccessToken()
        
        val newRequest = if (token != null && !originalRequest.url.encodedPath.contains("/auth/")) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }
        
        return chain.proceed(newRequest)
    }
}
