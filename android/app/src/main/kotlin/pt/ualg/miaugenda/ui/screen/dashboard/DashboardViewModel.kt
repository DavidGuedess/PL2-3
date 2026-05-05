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
    val error: Boolean = false
)

class DashboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    fun loadWeekShifts(weekStart: LocalDate) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = false
            )

            try {
                val response = RetrofitClient.shiftApi.getMyShifts(
                    week = weekStart.toString()
                )

                if (response.isSuccessful) {
                    _uiState.value = DashboardUiState(
                        shifts = response.body().orEmpty(),
                        isLoading = false
                    )
                } else {
                    _uiState.value = DashboardUiState(
                        isLoading = false,
                        error = true
                    )
                }
            } catch (e: Exception) {
                _uiState.value = DashboardUiState(
                    isLoading = false,
                    error = true
                )
            }
        }
    }
}