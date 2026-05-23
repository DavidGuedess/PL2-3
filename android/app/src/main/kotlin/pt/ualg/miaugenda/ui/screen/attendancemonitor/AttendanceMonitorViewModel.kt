package pt.ualg.miaugenda.ui.screen.attendancemonitor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pt.ualg.miaugenda.data.model.AttendanceRecord
import pt.ualg.miaugenda.data.model.User
import pt.ualg.miaugenda.data.remote.RetrofitClient
import pt.ualg.miaugenda.ui.screen.attendancehistory.DailyAttendance
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class AttendanceMonitorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AttendanceMonitorUiState())
    val uiState: StateFlow<AttendanceMonitorUiState> = _uiState

    private val userApi = RetrofitClient.userApi
    private val attendanceApi = RetrofitClient.attendanceApi

    init {
        loadUsers()
    }

    private fun loadUsers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingUsers = true, error = null)
            try {
                val response = userApi.getUsers()
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoadingUsers = false,
                        users = response.body().orEmpty()
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoadingUsers = false,
                        error = "Erro ao carregar utilizadores"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingUsers = false,
                    error = "Erro: ${e.message}"
                )
            }
        }
    }

    fun selectUser(user: User) {
        _uiState.value = _uiState.value.copy(selectedUser = user)
        loadAttendance()
    }

    fun setFromDate(date: String) {
        _uiState.value = _uiState.value.copy(fromDate = date)
        loadAttendance()
    }

    fun setToDate(date: String) {
        _uiState.value = _uiState.value.copy(toDate = date)
        loadAttendance()
    }

    private fun loadAttendance() {
        val state = _uiState.value
        val user = state.selectedUser ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingAttendance = true, error = null)
            try {
                val response = attendanceApi.getAttendance(
                    userId = user.id,
                    from = state.fromDate,
                    to = state.toDate
                )
                if (response.isSuccessful) {
                    val records = response.body().orEmpty()
                    _uiState.value = _uiState.value.copy(
                        isLoadingAttendance = false,
                        records = records,
                        dailyAttendances = buildDailyAttendances(records)
                    )
                } else if (response.code() == 401 || response.code() == 403) {
                    _uiState.value = _uiState.value.copy(
                        isLoadingAttendance = false,
                        error = "Sem permissão para ver estas presenças"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoadingAttendance = false,
                        error = "Erro ao carregar presenças"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingAttendance = false,
                    error = "Erro: ${e.message}"
                )
            }
        }
    }

    private fun buildDailyAttendances(records: List<AttendanceRecord>): List<DailyAttendance> {
        return records
            .groupBy { it.timestamp.substring(0, 10) }
            .map { (date, dayRecords) ->
                val sorted = dayRecords.sortedBy { it.timestamp }
                DailyAttendance(
                    date = date,
                    records = sorted,
                    totalMinutes = calculateTotalMinutes(sorted),
                    hasOpenRecord = hasOpenRecord(sorted)
                )
            }
            .sortedByDescending { it.date }
    }

    private fun calculateTotalMinutes(records: List<AttendanceRecord>): Long {
        var total = 0L
        var lastIn: AttendanceRecord? = null
        for (record in records) {
            if (record.type == "IN") {
                lastIn = record
            } else if (record.type == "OUT" && lastIn != null) {
                total += Duration.between(
                    parseTimestamp(lastIn!!.timestamp),
                    parseTimestamp(record.timestamp)
                ).toMinutes()
                lastIn = null
            }
        }
        return total
    }

    private fun hasOpenRecord(records: List<AttendanceRecord>): Boolean {
        var lastIn: AttendanceRecord? = null
        for (record in records) {
            if (record.type == "IN") lastIn = record
            else if (record.type == "OUT" && lastIn != null) lastIn = null
        }
        return lastIn != null
    }

    private fun parseTimestamp(timestamp: String): LocalDateTime =
        Instant.parse(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime()
}
