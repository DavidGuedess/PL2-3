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

        android.util.Log.d("AUTH_DEBUG", "URL: ${originalRequest.url}")
        android.util.Log.d("AUTH_DEBUG", "Token exists: ${token != null}")
        android.util.Log.d("AUTH_DEBUG", "Token preview: ${token?.take(20)}")

        val newRequest = if (token != null && !originalRequest.url.encodedPath.contains("/auth/")) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        android.util.Log.d("AUTH_DEBUG", "Authorization sent: ${newRequest.header("Authorization") != null}")

        return chain.proceed(newRequest)
    }
}
