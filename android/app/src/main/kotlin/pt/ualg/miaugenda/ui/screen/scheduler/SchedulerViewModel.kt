package pt.ualg.miaugenda.ui.screen.scheduler

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pt.ualg.miaugenda.MiauGendaApp
import pt.ualg.miaugenda.data.model.CreateShiftRequest
import pt.ualg.miaugenda.data.model.CreateUserRequest
import pt.ualg.miaugenda.data.model.Shift
import pt.ualg.miaugenda.data.model.ShiftType
import pt.ualg.miaugenda.data.model.UpdateShiftRequest
import pt.ualg.miaugenda.data.model.User
import pt.ualg.miaugenda.data.remote.RetrofitClient
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class SchedulerUiState(
    val shifts: List<Shift> = emptyList(),
    val users: List<User> = emptyList(),
    val shiftTypes: List<ShiftType> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class SchedulerViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = MiauGendaApp.getTokenManager(application)

    val userRole: String = tokenManager.getUserRole() ?: "EMPLOYEE"
    val currentUserId: Int = tokenManager.getUserId()
    val currentUserName: String = tokenManager.getUserName() ?: "Eu"
    val currentUserCategory: String = tokenManager.getUserCategory() ?: ""
    val currentUserEmployeeNumber: String = tokenManager.getEmployeeNumber() ?: ""

    private val _uiState = MutableStateFlow(SchedulerUiState())
    val uiState: StateFlow<SchedulerUiState> = _uiState

    // Normaliza para a 2a feira da semana actual
    private var currentWeek: LocalDate = run {
        val today = LocalDate.now()
        val dow = today.dayOfWeek.value // 1=Seg .. 7=Dom
        today.minusDays((dow - 1).toLong())
    }

    fun loadWeek(weekStart: LocalDate = currentWeek) {
        currentWeek = weekStart
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val weekStr = weekStart.format(DateTimeFormatter.ISO_LOCAL_DATE)

                if (userRole == "EMPLOYEE") {
                    // Funcionarios so veem os seus proprios turnos
                    val shiftsD     = async { RetrofitClient.shiftApi.getMyShifts(week = weekStr) }
                    val shiftTypesD = async { RetrofitClient.shiftApi.getShiftTypes() }

                    val sR  = shiftsD.await()
                    val stR = shiftTypesD.await()

                    if (sR.isSuccessful && stR.isSuccessful) {
                        _uiState.value = _uiState.value.copy(
                            shifts     = sR.body().orEmpty(),
                            users      = emptyList(),
                            shiftTypes = stR.body().orEmpty(),
                            isLoading  = false
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Erro ao carregar dados (${sR.code()})"
                        )
                    }
                } else {
                    // ADMIN e MANAGER veem todos os turnos e utilizadores
                    val shiftsD     = async { RetrofitClient.shiftApi.getShifts(week = weekStr) }
                    val usersD      = async { RetrofitClient.userApi.getUsers() }
                    val shiftTypesD = async { RetrofitClient.shiftApi.getShiftTypes() }

                    val sR  = shiftsD.await()
                    val uR  = usersD.await()
                    val stR = shiftTypesD.await()

                    if (sR.isSuccessful && uR.isSuccessful && stR.isSuccessful) {
                        _uiState.value = _uiState.value.copy(
                            shifts     = sR.body().orEmpty(),
                            users      = uR.body().orEmpty(),
                            shiftTypes = stR.body().orEmpty(),
                            isLoading  = false
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Erro ao carregar dados (${sR.code()})"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Sem ligacao ao servidor"
                )
            }
        }
    }

    fun createShift(userId: Int, shiftTypeId: Int, date: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val r = RetrofitClient.shiftApi.createShift(
                    CreateShiftRequest(userId = userId, shiftTypeId = shiftTypeId, date = date)
                )
                if (r.isSuccessful) {
                    loadWeek()
                    onResult(true, "Turno criado como rascunho")
                } else {
                    onResult(false, when (r.code()) {
                        409 -> "Conflito: funcionario ja tem turno nessa data"
                        403 -> "Sem permissao para criar turnos"
                        else -> "Erro ao criar turno (${r.code()})"
                    })
                }
            } catch (e: Exception) {
                onResult(false, "Sem ligacao ao servidor")
            }
        }
    }

    fun updateShift(shiftId: Int, shiftTypeId: Int? = null, date: String? = null, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val r = RetrofitClient.shiftApi.updateShift(
                    shiftId, UpdateShiftRequest(shiftTypeId = shiftTypeId, date = date)
                )
                if (r.isSuccessful) {
                    loadWeek()
                    onResult(true, "Turno atualizado com sucesso")
                } else {
                    onResult(false, when (r.code()) {
                        409 -> "Conflito: funcionario ja tem turno nessa data"
                        404 -> "Turno nao encontrado"
                        403 -> "Sem permissao para editar turnos"
                        else -> "Erro ao atualizar turno"
                    })
                }
            } catch (e: Exception) {
                onResult(false, "Sem ligacao ao servidor")
            }
        }
    }

    fun publishShift(shiftId: Int, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val r = RetrofitClient.shiftApi.updateShift(
                    shiftId, UpdateShiftRequest(published = true)
                )
                if (r.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        shifts = _uiState.value.shifts.map {
                            if (it.id == shiftId) it.copy(published = true) else it
                        }
                    )
                    onResult(true, "Turno publicado com sucesso")
                } else {
                    onResult(false, "Erro ao publicar turno")
                }
            } catch (e: Exception) {
                onResult(false, "Sem ligacao ao servidor")
            }
        }
    }

    fun publishMultipleShifts(shiftIds: List<Int>, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val results = shiftIds.map { id ->
                    async {
                        RetrofitClient.shiftApi.updateShift(id, UpdateShiftRequest(published = true))
                    }
                }.awaitAll()

                val ok   = results.count { it.isSuccessful }
                val fail = results.count { !it.isSuccessful }

                _uiState.value = _uiState.value.copy(
                    shifts = _uiState.value.shifts.map {
                        if (shiftIds.contains(it.id)) it.copy(published = true) else it
                    }
                )

                if (fail == 0) onResult(true, "$ok turnos publicados com sucesso")
                else           onResult(true, "$ok publicados, $fail com erro")
            } catch (e: Exception) {
                onResult(false, "Sem ligacao ao servidor")
            }
        }
    }

    fun deleteShift(shiftId: Int, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val r = RetrofitClient.shiftApi.deleteShift(shiftId)
                if (r.isSuccessful || r.code() == 204) {
                    _uiState.value = _uiState.value.copy(
                        shifts = _uiState.value.shifts.filter { it.id != shiftId }
                    )
                    onResult(true, "Turno eliminado com sucesso")
                } else {
                    onResult(false, when (r.code()) {
                        404 -> "Turno nao encontrado"
                        403 -> "Sem permissao para eliminar turnos"
                        else -> "Erro ao eliminar turno"
                    })
                }
            } catch (e: Exception) {
                onResult(false, "Sem ligacao ao servidor")
            }
        }
    }

    fun createUser(request: CreateUserRequest, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val r = RetrofitClient.userApi.createUser(request)
                if (r.isSuccessful) {
                    val uR = RetrofitClient.userApi.getUsers()
                    if (uR.isSuccessful) {
                        _uiState.value = _uiState.value.copy(users = uR.body().orEmpty())
                    }
                    onResult(true, "Funcionario criado com sucesso")
                } else {
                    onResult(false, when (r.code()) {
                        409 -> "Email ou numero de funcionario ja existe"
                        403 -> "Sem permissao para criar utilizadores"
                        else -> "Erro ao criar funcionario (${r.code()})"
                    })
                }
            } catch (e: Exception) {
                onResult(false, "Sem ligacao ao servidor")
            }
        }
    }

    fun getCurrentWeek(): LocalDate = currentWeek
    fun previousWeek() = loadWeek(currentWeek.minusWeeks(1))
    fun nextWeek()     = loadWeek(currentWeek.plusWeeks(1))
}
