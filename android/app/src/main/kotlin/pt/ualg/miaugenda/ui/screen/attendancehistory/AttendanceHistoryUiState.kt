package pt.ualg.miaugenda.ui.screen.attendancehistory

import pt.ualg.miaugenda.data.model.AttendanceRecord

data class AttendanceHistoryUiState(
    val isLoading: Boolean = false,
    val records: List<AttendanceRecord> = emptyList(),
    val filteredRecords: List<AttendanceRecord> = emptyList(),
    val dailyAttendances: List<DailyAttendance> = emptyList(),
    val error: String? = null,
    val fromDate: String? = null,
    val toDate: String? = null
)