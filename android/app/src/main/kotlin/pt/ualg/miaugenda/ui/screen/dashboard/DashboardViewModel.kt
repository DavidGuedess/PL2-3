package pt.ualg.miaugenda.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import pt.ualg.miaugenda.data.model.Shift
import pt.ualg.miaugenda.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.IsoFields

data class DashboardUiState(
    val nextShift: Shift? = null,
    val isLoading: Boolean = true,
    val error: Boolean = false
)

class DashboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    init {
        loadNextShift()
    }

    fun loadNextShift() {
        viewModelScope.launch {
            _uiState.value = DashboardUiState(isLoading = true)
            try {
                val today = LocalDate.now()
                val week = "${today.year}-W${
                    today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR).toString().padStart(2, '0')
                }"
                val response = RetrofitClient.shiftApi.getShifts(week = week)
                if (response.isSuccessful) {
                    val nextShift = response.body()
                        ?.filter { LocalDate.parse(it.date.substring(0, 10)) >= today }
                        ?.minByOrNull { it.date }
                    _uiState.value = DashboardUiState(nextShift = nextShift, isLoading = false)
                } else {
                    _uiState.value = DashboardUiState(isLoading = false, error = true)
                }
            } catch (e: Exception) {
                _uiState.value = DashboardUiState(isLoading = false, error = true)
            }
        }
    }
}
