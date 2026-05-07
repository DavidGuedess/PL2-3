package pt.ualg.miaugenda.ui.screen.attendancemonitor

import pt.ualg.miaugenda.data.model.AttendanceRecord
import pt.ualg.miaugenda.data.model.User
import pt.ualg.miaugenda.ui.screen.attendancehistory.DailyAttendance

data class AttendanceMonitorUiState(
    val isLoadingUsers: Boolean = false,
    val isLoadingAttendance: Boolean = false,
    val users: List<User> = emptyList(),
    val selectedUser: User? = null,
    val records: List<AttendanceRecord> = emptyList(),
    val dailyAttendances: List<DailyAttendance> = emptyList(),
    val fromDate: String? = null,
    val toDate: String? = null,
    val error: String? = null
)
