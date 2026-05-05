package pt.ualg.miaugenda.ui.screen.attendancehistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pt.ualg.miaugenda.data.model.AttendanceRecord
import pt.ualg.miaugenda.data.remote.RetrofitClient
import java.time.Duration
import java.time.LocalDateTime
import java.time.Instant
import java.time.ZoneId

class AttendanceHistoryViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AttendanceHistoryUiState())
    val uiState: StateFlow<AttendanceHistoryUiState> = _uiState

    private val attendanceApi = RetrofitClient.attendanceApi

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            try {
                val response = attendanceApi.getMyHistory()

                if (response.isSuccessful) {
                    val records = response.body().orEmpty()

                    val dailyAttendances = buildDailyAttendances(records)

                    _uiState.value = AttendanceHistoryUiState(
                        records = records,
                        filteredRecords = records,
                        dailyAttendances = dailyAttendances
                    )
                } else if (response.code() == 401) {
                    _uiState.value = AttendanceHistoryUiState(
                        error = "Utilizador não autorizado"
                    )
                } else {
                    _uiState.value = AttendanceHistoryUiState(
                        error = "Erro ao carregar histórico de presenças"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = AttendanceHistoryUiState(
                    error = "Erro: ${e.message}"
                )
            }
        }
    }

    fun setFromDate(date: String) {
        _uiState.value = _uiState.value.copy(fromDate = date)
        applyFilters()
    }

    fun setToDate(date: String) {
        _uiState.value = _uiState.value.copy(toDate = date)
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value

        val from = state.fromDate
        val to = state.toDate

        val filtered = state.records.filter { record ->
            val date = record.timestamp.substring(0, 10)

            val afterFrom = from == null || date >= from
            val beforeTo = to == null || date <= to

            afterFrom && beforeTo
        }

        _uiState.value = state.copy(
            filteredRecords = filtered,
            dailyAttendances = buildDailyAttendances(filtered)
        )
    }

    private fun buildDailyAttendances(records: List<AttendanceRecord>): List<DailyAttendance> {
        return records
            .sortedByDescending { it.timestamp }
            .groupBy { it.timestamp.substring(0, 10) }
            .map { entry ->
                val date = entry.key
                val dayRecords = entry.value.sortedBy { it.timestamp }

                DailyAttendance(
                    date = date,
                    records = dayRecords,
                    totalMinutes = calculateTotalMinutes(dayRecords),
                    hasOpenRecord = hasOpenRecord(dayRecords)
                )
            }
            .sortedByDescending { it.date }
    }

    private fun calculateTotalMinutes(records: List<AttendanceRecord>): Long {
        var totalMinutes = 0L
        var lastIn: AttendanceRecord? = null

        for (record in records.sortedBy { it.timestamp }) {
            if (record.type == "IN") {
                lastIn = record
            } else if (record.type == "OUT" && lastIn != null) {
                totalMinutes += Duration.between(
                    parseTimestamp(lastIn!!.timestamp),
                    parseTimestamp(record.timestamp)
                ).toMinutes()

                lastIn = null
            }
        }

        return totalMinutes
    }

    private fun hasOpenRecord(records: List<AttendanceRecord>): Boolean {
        var lastIn: AttendanceRecord? = null

        for (record in records.sortedBy { it.timestamp }) {
            if (record.type == "IN") {
                lastIn = record
            } else if (record.type == "OUT" && lastIn != null) {
                lastIn = null
            }
        }

        return lastIn != null
    }

    private fun parseTimestamp(timestamp: String): LocalDateTime {
        return Instant.parse(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
    }
}