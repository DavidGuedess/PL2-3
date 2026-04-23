package pt.ualg.miaugenda.data.repository

import android.util.Log
import pt.ualg.miaugenda.data.model.LoginRequest
import pt.ualg.miaugenda.data.model.LoginResponse
import pt.ualg.miaugenda.data.remote.RetrofitClient

class AuthRepository {
    companion object {
        private const val TAG = "AuthRepository"
    }

    private val authApi by lazy { RetrofitClient.authApi }

    suspend fun login(employeeNumber: String, password: String): Result<LoginResponse> {
        return try {
            Log.d(TAG, "Iniciando login para: $employeeNumber")
            val response = authApi.login(LoginRequest(employeeNumber, password))
            Log.d(TAG, "Resposta recebida: HTTP ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "Login bem-sucedido")
                Result.success(response.body()!!)
            } else {
                val errorMsg = when (response.code()) {
                    401 -> "Credenciais inválidas"
                    400 -> "Dados inválidos"
                    500 -> "Erro no servidor. Verifique se a base de dados está configurada."
                    else -> "Erro ao fazer login: HTTP ${response.code()}"
                }
                Log.w(TAG, "Login falhou: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exceção durante login", e)
            val msg = when (e) {
                is java.net.ConnectException -> "Não foi possível conectar ao servidor (verifique o backend)"
                is java.net.SocketTimeoutException -> "Tempo de espera excedido"
                is java.net.UnknownHostException -> "Host desconhecido (verifique a URL)"
                else -> "Erro de conexão: ${e.message ?: e.javaClass.simpleName}"
            }
            Result.failure(Exception(msg))
        }
    }
}
