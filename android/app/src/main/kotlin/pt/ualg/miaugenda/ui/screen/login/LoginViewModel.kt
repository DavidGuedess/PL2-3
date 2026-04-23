package pt.ualg.miaugenda.ui.screen.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pt.ualg.miaugenda.data.local.TokenManager
import pt.ualg.miaugenda.data.repository.AuthRepository

data class LoginUiState(
    val employeeNumber: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoginSuccessful: Boolean = false
)

class LoginViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val tokenManager: TokenManager
) : ViewModel() {

    companion object {
        private const val TAG = "LoginViewModel"
    }

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmployeeNumberChange(value: String) {
        _uiState.value = _uiState.value.copy(
            employeeNumber = value,
            errorMessage = null
        )
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(
            password = value,
            errorMessage = null
        )
    }

    fun login() {
        val currentState = _uiState.value
        
        if (currentState.employeeNumber.isBlank() || currentState.password.isBlank()) {
            _uiState.value = currentState.copy(
                errorMessage = "Por favor, preencha todos os campos"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true, errorMessage = null)
            Log.d(TAG, "A fazer login com employeeNumber=${currentState.employeeNumber}")

            try {
                val result = authRepository.login(
                    currentState.employeeNumber,
                    currentState.password
                )
                
                result.fold(
                    onSuccess = { response ->
                        try {
                            tokenManager.saveTokens(
                                accessToken = response.accessToken,
                                refreshToken = response.refreshToken,
                                userId = response.user.id,
                                userName = response.user.name,
                                userEmail = response.user.email,
                                userRole = response.user.role,
                                userCategory = response.user.category,
                                employeeNumber = response.user.employeeNumber
                            )
                            
                            _uiState.value = currentState.copy(
                                isLoading = false,
                                isLoginSuccessful = true,
                                errorMessage = null
                            )
                        } catch (e: Exception) {
                            _uiState.value = currentState.copy(
                                isLoading = false,
                                errorMessage = "Erro ao guardar dados: ${e.message}"
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.value = currentState.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Erro desconhecido"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = currentState.copy(
                    isLoading = false,
                    errorMessage = "Erro inesperado: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
