package pt.ualg.miaugenda.ui.screen.attendancehistory

import pt.ualg.miaugenda.data.model.AttendanceRecord

data class DailyAttendance(
    val date: String,
    val records: List<AttendanceRecord>,
    val totalMinutes: Long,
    val hasOpenRecord: Boolean
)