package pt.ualg.miaugenda.ui.screen.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pt.ualg.miaugenda.data.model.UpdateProfileRequest
import pt.ualg.miaugenda.data.model.User
import pt.ualg.miaugenda.data.remote.RetrofitClient
import pt.ualg.miaugenda.data.local.TokenManager

data class ProfileUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val user: User? = null,
    val error: String? = null,
    val successMessage: String? = null
)

class ProfileViewModel(
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                successMessage = null
            )

            try {
                val response = RetrofitClient.userApi.getMe()

                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = response.body()
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Erro ao carregar perfil (${response.code()})"
                    )
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun updateProfile(
        name: String,
        contact: String,
        password: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSaving = true,
                error = null,
                successMessage = null
            )

            try {
                val request = UpdateProfileRequest(
                    name = name,
                    contact = contact.ifBlank { null },
                    password = password.ifBlank { null }
                )

                val response = RetrofitClient.userApi.updateMe(request)

                if (response.isSuccessful) {
                    val updatedUser = response.body()

                    // 🔥 ATUALIZA TOKEN MANAGER (FIX DO TEU BUG)
                    updatedUser?.let {
                        tokenManager.updateUserData(
                            name = it.name,
                            contact = it.contact,
                            category = it.category
                        )
                    }

                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        user = updatedUser,
                        successMessage = "Perfil atualizado com sucesso"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = "Erro ao atualizar perfil (${response.code()})"
                    )
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = e.message
                )
            }
        }
    }
}