package pt.ualg.miaugenda.data.model

data class AttendanceRecord(
    val id: Int,
    val userId: Int,
    val type: String,
    val timestamp: String,
    val note: String? = null
)