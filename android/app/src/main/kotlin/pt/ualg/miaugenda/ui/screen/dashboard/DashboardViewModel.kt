package pt.ualg.miaugenda.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pt.ualg.miaugenda.data.model.Shift
import pt.ualg.miaugenda.data.remote.RetrofitClient
import java.time.LocalDate

data class DashboardUiState(
    val shifts: List<Shift> = emptyList(),
    val isLoading: Boolean = true,
    val error: Boolean = false,
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
                val response = RetrofitClient.shiftApi.getMyShifts(week = weekStart.toString())
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        shifts    = response.body().orEmpty(),
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = true)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = true)
            }
        }
    }

    // Carrega o estado de ponto do dia de hoje
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
            } catch (_: Exception) { }
        }
    }

    fun clockIn(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val r = RetrofitClient.attendanceApi.register(mapOf("type" to "IN"))
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

    fun clockOut(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val r = RetrofitClient.attendanceApi.register(mapOf("type" to "OUT"))
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
}