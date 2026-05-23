package pt.ualg.miaugenda.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pt.ualg.miaugenda.data.model.ActiveEmployee
import pt.ualg.miaugenda.data.model.AttendanceRecord
import pt.ualg.miaugenda.data.model.CreateAttendanceBody
import pt.ualg.miaugenda.data.model.Shift
import pt.ualg.miaugenda.data.model.TimeOffRequest
import pt.ualg.miaugenda.data.model.ShiftSwapRequest
import pt.ualg.miaugenda.data.remote.RetrofitClient
import java.time.LocalDate

data class DashboardUiState(
    val shifts: List<Shift> = emptyList(),
    val attendanceRecords: List<AttendanceRecord> = emptyList(),
    val activeEmployees: List<ActiveEmployee> = emptyList(),
    val timeOffRequests: List<TimeOffRequest> = emptyList(),
    val shiftSwapRequests: List<ShiftSwapRequest> = emptyList(),
    val isLoading: Boolean = true,
    val error: Boolean = false,
    // Total de turnos publicados da semana (todos os funcionários)
    val allWeekShiftsCount: Int = 0,
    // Estado de assiduidade
    val isClocked: Boolean = false,
    val clockedInSince: String? = null   // timestamp ISO do último registo IN
)

class DashboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    init {
        loadTodayAttendance()
    }

    fun loadWeekShifts(weekStart: LocalDate) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = false)
            try {
                val weekEnd = weekStart.plusDays(6)
                val shiftsResp    = RetrofitClient.shiftApi.getMyShifts(week = weekStart.toString())
                val allShiftsResp = RetrofitClient.shiftApi.getShifts(week = weekStart.toString())
                val attendResp    = RetrofitClient.attendanceApi.getMyHistory(
                    from = weekStart.toString(), to = weekEnd.toString()
                )
                if (shiftsResp.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        shifts              = shiftsResp.body().orEmpty(),
                        attendanceRecords   = if (attendResp.isSuccessful) attendResp.body().orEmpty() else emptyList(),
                        allWeekShiftsCount  = if (allShiftsResp.isSuccessful) allShiftsResp.body().orEmpty().count { it.published } else 0,
                        isLoading           = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = true)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = true)
            }
        }
    }

    // Carrega o estado de ponto do dia de hoje + lista de ativos
    fun loadTodayAttendance() {
        viewModelScope.launch {
            try {
                val today = LocalDate.now().toString()
                val r = RetrofitClient.attendanceApi.getMyHistory(from = today, to = today)
                if (r.isSuccessful) {
                    val records = r.body().orEmpty().sortedBy { it.timestamp }
                    val last    = records.lastOrNull()
                    val clocked = last?.type == "IN"
                    _uiState.value = _uiState.value.copy(
                        isClocked      = clocked,
                        clockedInSince = if (clocked) last?.timestamp else null
                    )
                }
                val activeResp = RetrofitClient.attendanceApi.getActiveEmployees()
                if (activeResp.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        activeEmployees = activeResp.body().orEmpty()
                    )
                }
            } catch (_: Exception) { }
        }
    }

    fun clockIn(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val r = RetrofitClient.attendanceApi.register(CreateAttendanceBody(type = "IN"))
                if (r.isSuccessful) {
                    val record = r.body()
                    _uiState.value = _uiState.value.copy(
                        isClocked      = true,
                        clockedInSince = record?.timestamp
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

    fun clockOut(note: String = "", onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val r = RetrofitClient.attendanceApi.register(
                    CreateAttendanceBody(type = "OUT", note = note.ifBlank { null })
                )
                if (r.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isClocked      = false,
                        clockedInSince = null
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

    fun loadMyRequests() {
        viewModelScope.launch {
            try {
                val torResponse = RetrofitClient.requestApi.getTimeOffRequests()
                val ssrResponse = RetrofitClient.requestApi.getShiftSwapRequests()
                _uiState.value = _uiState.value.copy(
                    timeOffRequests   = if (torResponse.isSuccessful) torResponse.body().orEmpty() else _uiState.value.timeOffRequests,
                    shiftSwapRequests = if (ssrResponse.isSuccessful) ssrResponse.body().orEmpty() else _uiState.value.shiftSwapRequests
                )
            } catch (_: Exception) { }
        }
    }
}