package pt.ualg.miaugenda.ui.screen.myschedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pt.ualg.miaugenda.data.model.Shift
import pt.ualg.miaugenda.data.remote.RetrofitClient
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class MyScheduleUiState(
    val weekStart: LocalDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
    val shifts: List<Shift> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class MyScheduleViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MyScheduleUiState())
    val uiState: StateFlow<MyScheduleUiState> = _uiState

    init {
        loadShiftsForWeek(_uiState.value.weekStart)
    }

    fun previousWeek() {
        val newStart = _uiState.value.weekStart.minusWeeks(1)
        _uiState.value = _uiState.value.copy(weekStart = newStart)
        loadShiftsForWeek(newStart)
    }

    fun nextWeek() {
        val newStart = _uiState.value.weekStart.plusWeeks(1)
        _uiState.value = _uiState.value.copy(weekStart = newStart)
        loadShiftsForWeek(newStart)
    }

    fun refresh() {
        loadShiftsForWeek(_uiState.value.weekStart)
    }

    private fun loadShiftsForWeek(weekStart: LocalDate) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = RetrofitClient.shiftApi.getMyOwnShifts(week = weekStart.toString())
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        shifts = response.body().orEmpty(),
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Erro ao carregar turnos (${response.code()})"
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
}
