package pt.ualg.miaugenda

import android.content.Context
import android.util.Log
import pt.ualg.miaugenda.data.local.TokenManager
import pt.ualg.miaugenda.data.remote.AuthInterceptor
import pt.ualg.miaugenda.data.remote.RetrofitClient

object MiauGendaApp {
    private const val TAG = "MiauGendaApp"

    @Volatile
    private var tokenManager: TokenManager? = null

    @Synchronized
    fun initialize(context: Context) {
        if (tokenManager != null) return
        try {
            Log.d(TAG, "Criando TokenManager...")
            val tm = TokenManager(context.applicationContext)
            tokenManager = tm
            Log.d(TAG, "Configurando AuthInterceptor no Retrofit...")
            RetrofitClient.setAuthInterceptor(AuthInterceptor(tm))
            Log.d(TAG, "Inicialização completa")
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao inicializar MiauGendaApp", e)
            throw e
        }
    }

    fun getTokenManager(context: Context): TokenManager {
        return tokenManager ?: run {
            initialize(context)
            tokenManager!!
        }
    }
}
