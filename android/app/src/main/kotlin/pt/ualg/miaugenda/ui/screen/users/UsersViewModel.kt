package pt.ualg.miaugenda.ui.screen.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pt.ualg.miaugenda.data.model.CreateUserRequest
import pt.ualg.miaugenda.data.model.User
import pt.ualg.miaugenda.data.remote.RetrofitClient

data class UsersUiState(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val showCreateDialog: Boolean = false,
    val pendingDeactivation: User? = null
)

class UsersViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UsersUiState())
    val uiState: StateFlow<UsersUiState> = _uiState

    private val userApi = RetrofitClient.userApi

    init {
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = userApi.getUsers()
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        users = response.body().orEmpty(),
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Erro ao carregar utilizadores (${response.code()})"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Erro desconhecido"
                )
            }
        }
    }

    fun openCreateDialog() {
        _uiState.value = _uiState.value.copy(
            showCreateDialog = true,
            error = null,
            successMessage = null
        )
    }

    fun closeCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = false)
    }

    fun createUser(request: CreateUserRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)
            try {
                val response = userApi.createUser(request)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        showCreateDialog = false,
                        successMessage = "Utilizador criado com sucesso"
                    )
                    loadUsers()
                } else {
                    val msg = when (response.code()) {
                        400 -> "Dados invalidos"
                        403 -> "Sem permissao (requer ADMIN)"
                        409 -> "Email ou numero de funcionario ja existe"
                        else -> "Erro ao criar utilizador (${response.code()})"
                    }
                    _uiState.value = _uiState.value.copy(isSubmitting = false, error = msg)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    error = e.message ?: "Erro desconhecido"
                )
            }
        }
    }

    fun requestDeactivate(user: User) {
        _uiState.value = _uiState.value.copy(pendingDeactivation = user)
    }

    fun cancelDeactivate() {
        _uiState.value = _uiState.value.copy(pendingDeactivation = null)
    }

    fun confirmDeactivate() {
        val target = _uiState.value.pendingDeactivation ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)
            try {
                val response = userApi.deactivateUser(target.id)
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        pendingDeactivation = null,
                        successMessage = "Utilizador desativado"
                    )
                    loadUsers()
                } else {
                    val msg = when (response.code()) {
                        403 -> "Sem permissao (requer ADMIN)"
                        404 -> "Utilizador nao encontrado"
                        409 -> "Utilizador ja esta desativado"
                        else -> "Erro ao desativar (${response.code()})"
                    }
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        pendingDeactivation = null,
                        error = msg
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    pendingDeactivation = null,
                    error = e.message ?: "Erro desconhecido"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }
}
