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
import pt.ualg.miaugenda.data.model.CreateWeekAssignmentRequest
import pt.ualg.miaugenda.data.model.Shift
import pt.ualg.miaugenda.data.model.Availability
import pt.ualg.miaugenda.data.model.TimeOffRequest
import pt.ualg.miaugenda.data.model.resolvedStartTime
import pt.ualg.miaugenda.data.model.resolvedEndTime
import pt.ualg.miaugenda.data.model.ShiftType
import pt.ualg.miaugenda.data.model.UpdateShiftRequest
import pt.ualg.miaugenda.data.model.User
import pt.ualg.miaugenda.data.model.WeekAssignment
import pt.ualg.miaugenda.data.remote.RetrofitClient
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class SchedulerUiState(
    val shifts: List<Shift> = emptyList(),
    val users: List<User> = emptyList(),
    val shiftTypes: List<ShiftType> = emptyList(),
    val weekAssignments: List<WeekAssignment> = emptyList(),
    val timeOffRequests: List<TimeOffRequest> = emptyList(),
    val availabilities: List<Availability> = emptyList(),
    val statusFilter: String? = null,
    val categoryFilter: String? = null,
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

                val weekEndStr = weekStart.plusDays(6).format(DateTimeFormatter.ISO_LOCAL_DATE)

                if (userRole == "EMPLOYEE") {
                    // Funcionarios so veem os seus proprios turnos
                    val shiftsD  = async { RetrofitClient.shiftApi.getMyShifts(week = weekStr) }
                    val stD      = async { RetrofitClient.shiftApi.getShiftTypes() }
                    val torD     = async { RetrofitClient.requestApi.getTimeOffRequests() }
                    val availD   = async { RetrofitClient.availabilityApi.getAvailabilities(startDate = weekStr, endDate = weekEndStr) }

                    val sR   = shiftsD.await()
                    val stR  = stD.await()
                    val torR = torD.await()
                    val avR  = availD.await()

                    if (sR.isSuccessful && stR.isSuccessful) {
                        _uiState.value = _uiState.value.copy(
                            shifts          = sR.body().orEmpty(),
                            users           = emptyList(),
                            shiftTypes      = stR.body().orEmpty(),
                            timeOffRequests = if (torR.isSuccessful) torR.body().orEmpty() else emptyList(),
                            availabilities  = if (avR.isSuccessful) avR.body().orEmpty() else emptyList(),
                            isLoading       = false
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Erro ao carregar dados (${sR.code()})"
                        )
                    }
                } else {
                    // ADMIN e MANAGER veem todos os turnos e utilizadores
                    val shiftsD = async { RetrofitClient.shiftApi.getShifts(week = weekStr) }
                    val usersD  = async { RetrofitClient.userApi.getUsers() }
                    val stD     = async { RetrofitClient.shiftApi.getShiftTypes() }
                    val waD     = async { RetrofitClient.weekAssignmentApi.getWeekAssignments(week = weekStr) }
                    val torD    = async { RetrofitClient.requestApi.getTimeOffRequests() }
                    val availD  = async { RetrofitClient.availabilityApi.getAvailabilities(startDate = weekStr, endDate = weekEndStr) }

                    val sR   = shiftsD.await()
                    val uR   = usersD.await()
                    val stR  = stD.await()
                    val waR  = waD.await()
                    val torR = torD.await()
                    val avR  = availD.await()

                    if (sR.isSuccessful && uR.isSuccessful && stR.isSuccessful) {
                        _uiState.value = _uiState.value.copy(
                            shifts          = sR.body().orEmpty(),
                            users           = uR.body().orEmpty(),
                            shiftTypes      = stR.body().orEmpty(),
                            weekAssignments = waR.body().orEmpty(),
                            timeOffRequests = if (torR.isSuccessful) torR.body().orEmpty() else emptyList(),
                            availabilities  = if (avR.isSuccessful) avR.body().orEmpty() else emptyList(),
                            isLoading       = false
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

    private fun parseApiError(bodyStr: String?, fallback: String): String {
        return try {
            val json = org.json.JSONObject(bodyStr ?: "")
            val errors = json.optJSONArray("errors")
            if (errors != null && errors.length() > 0) {
                val first = errors.getJSONObject(0)
                val path = first.optString("path", "")
                val msg = first.optString("message", fallback)
                if (path.isNotEmpty()) "[$path] $msg" else msg
            } else {
                json.optString("message", fallback)
            }
        } catch (_: Exception) { fallback }
    }

    fun createShift(userId: Int, date: String, startTime: String, endTime: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val r = RetrofitClient.shiftApi.createShift(
                    CreateShiftRequest(userId = userId, date = date, startTime = startTime, endTime = endTime)
                )
                if (r.isSuccessful) {
                    loadWeek()
                    onResult(true, "Turno criado como rascunho")
                } else {
                    val body = r.errorBody()?.string()
                    onResult(false, when (r.code()) {
                        409 -> parseApiError(body, "Conflito de horario com turno existente")
                        403 -> "Sem permissao para criar turnos"
                        else -> parseApiError(body, "Erro ao criar turno (${r.code()})")
                    })
                }
            } catch (e: Exception) {
                onResult(false, "Sem ligacao ao servidor")
            }
        }
    }

    fun updateShift(shiftId: Int, date: String? = null, startTime: String? = null, endTime: String? = null, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val r = RetrofitClient.shiftApi.updateShift(
                    shiftId, UpdateShiftRequest(startTime = startTime, endTime = endTime, date = date)
                )
                if (r.isSuccessful) {
                    loadWeek()
                    onResult(true, "Turno atualizado com sucesso")
                } else {
                    val body = r.errorBody()?.string()
                    onResult(false, when (r.code()) {
                        409 -> parseApiError(body, "Conflito de horario com turno existente")
                        404 -> "Turno nao encontrado"
                        403 -> "Sem permissao para editar turnos"
                        else -> parseApiError(body, "Erro ao atualizar turno (${r.code()})")
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
                    onResult(false, "Erro ao publicar turno (${r.code()})")
                }
            } catch (e: Exception) {
                onResult(false, "Sem ligacao ao servidor")
            }
        }
    }

    fun publishMultipleShifts(shiftIds: List<Int>, onResult: (okCount: Int, failCount: Int) -> Unit) {
        viewModelScope.launch {
            try {
                val results = shiftIds.map { id ->
                    async {
                        RetrofitClient.shiftApi.updateShift(id, UpdateShiftRequest(published = true))
                    }
                }.awaitAll()

                val ok   = results.count { it.isSuccessful }
                val fail = results.count { !it.isSuccessful }

                loadWeek()
                onResult(ok, fail)
            } catch (e: Exception) {
                onResult(0, shiftIds.size)
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

    fun addUserToWeek(userId: Int, weekStr: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val r = RetrofitClient.weekAssignmentApi.createWeekAssignment(
                    CreateWeekAssignmentRequest(userId = userId, weekStart = weekStr)
                )
                if (r.isSuccessful) {
                    // Refetch weekAssignments para garantir estado atualizado
                    refreshWeekAssignments(weekStr)
                    onResult(true)
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun removeUserFromWeek(assignmentId: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val r = RetrofitClient.weekAssignmentApi.deleteWeekAssignment(assignmentId)
                if (r.isSuccessful || r.code() == 204) {
                    val removedAssignment = _uiState.value.weekAssignments.find { it.id == assignmentId }
                    val newAssignments = _uiState.value.weekAssignments.filter { it.id != assignmentId }
                    val newShifts = if (removedAssignment != null)
                        _uiState.value.shifts.filter { it.userId != removedAssignment.userId }
                    else
                        _uiState.value.shifts
                    _uiState.value = _uiState.value.copy(
                        weekAssignments = newAssignments,
                        shifts = newShifts
                    )
                    onResult(true)
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    private suspend fun refreshWeekAssignments(weekStr: String) {
        try {
            val waR = RetrofitClient.weekAssignmentApi.getWeekAssignments(week = weekStr)
            if (waR.isSuccessful) {
                _uiState.value = _uiState.value.copy(weekAssignments = waR.body().orEmpty())
            }
        } catch (_: Exception) { }
    }

    fun copyWeek(targetWeek: LocalDate, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val sourceShifts = _uiState.value.shifts
            if (sourceShifts.isEmpty()) {
                onResult(false, "Nao ha turnos para copiar nesta semana")
                return@launch
            }
            try {
                var created = 0; var conflicts = 0
                sourceShifts.forEach { shift ->
                    val sourceDate = LocalDate.parse(shift.date.substring(0, 10))
                    val dayOffset = java.time.temporal.ChronoUnit.DAYS.between(currentWeek, sourceDate)
                    val targetDate = targetWeek.plusDays(dayOffset).format(DateTimeFormatter.ISO_LOCAL_DATE)
                    val r = RetrofitClient.shiftApi.createShift(
                        CreateShiftRequest(
                            userId = shift.userId,
                            date = targetDate,
                            startTime = shift.resolvedStartTime(),
                            endTime = shift.resolvedEndTime(),
                            shiftTypeId = shift.shiftTypeId
                        )
                    )
                    if (r.isSuccessful) created++ else if (r.code() == 409) conflicts++
                }
                onResult(true, if (conflicts == 0) "$created turnos copiados" else "$created copiados, $conflicts com conflito")
            } catch (e: Exception) {
                onResult(false, "Sem ligacao ao servidor")
            }
        }
    }

    fun setFilters(status: String?, category: String?) {
        _uiState.value = _uiState.value.copy(statusFilter = status, categoryFilter = category)
    }

    fun getCurrentWeek(): LocalDate = currentWeek
    fun previousWeek() = loadWeek(currentWeek.minusWeeks(1))
    fun nextWeek()     = loadWeek(currentWeek.plusWeeks(1))
}
