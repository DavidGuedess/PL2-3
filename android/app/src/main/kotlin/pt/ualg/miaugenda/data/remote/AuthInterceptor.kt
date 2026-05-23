package pt.ualg.miaugenda.data.remote

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import pt.ualg.miaugenda.AppState
import pt.ualg.miaugenda.BuildConfig
import pt.ualg.miaugenda.data.local.TokenManager

class AuthInterceptor(
    private val tokenManager: TokenManager
) : Interceptor {

    private val refreshClient = OkHttpClient.Builder().build()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = tokenManager.getAccessToken()

        val authenticatedRequest = if (token != null && !originalRequest.url.encodedPath.contains("/auth/")) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        val response = chain.proceed(authenticatedRequest)

        if (response.code == 401 && !originalRequest.url.encodedPath.contains("/auth/")) {
            response.close()
            val newToken = tryRefreshTokens(token)
            if (newToken != null) {
                val retryRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer $newToken")
                    .build()
                return chain.proceed(retryRequest)
            } else {
                AppState.signalTokenExpired()
                return chain.proceed(originalRequest)
            }
        }

        return response
    }

    @Synchronized
    private fun tryRefreshTokens(expiredToken: String?): String? {
        // Double-check: another thread may have already refreshed
        val currentToken = tokenManager.getAccessToken()
        if (currentToken != null && currentToken != expiredToken) {
            return currentToken
        }

        val refreshToken = tokenManager.getRefreshToken() ?: run {
            tokenManager.clearTokens()
            return null
        }

        return try {
            val json = JSONObject().put("refreshToken", refreshToken).toString()
            val body = json.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(BuildConfig.API_BASE_URL + "/api/auth/refresh")
                .post(body)
                .build()

            refreshClient.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) {
                    val respBody = resp.body?.string() ?: return null
                    val jsonResp = JSONObject(respBody)
                    val newAccessToken = jsonResp.optString("accessToken").takeIf { it.isNotEmpty() }
                        ?: run { tokenManager.clearTokens(); return null }
                    val newRefreshToken = jsonResp.optString("refreshToken").takeIf { it.isNotEmpty() }
                        ?: newAccessToken
                    tokenManager.updateTokens(newAccessToken, newRefreshToken)
                    newAccessToken
                } else {
                    tokenManager.clearTokens()
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
