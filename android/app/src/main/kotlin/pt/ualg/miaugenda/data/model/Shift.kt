package pt.ualg.miaugenda.data.model

data class ShiftUser(
    val id: Int,
    val name: String,
    val employeeNumber: String,
    val role: String,
    val category: String = ""
)

data class ShiftType(
    val id: Int,
    val name: String,
    val startTime: String,
    val endTime: String
)

data class Shift(
    val id: Int,
    val userId: Int,
    val shiftTypeId: Int? = null,
    val date: String,
    val startTime: String? = null,
    val endTime: String? = null,
    val published: Boolean = false,
    val user: ShiftUser? = null,
    val shiftType: ShiftType? = null
)

fun Shift.resolvedStartTime(): String = startTime ?: shiftType?.startTime ?: "00:00"
fun Shift.resolvedEndTime(): String   = endTime   ?: shiftType?.endTime   ?: "00:00"
fun Shift.resolvedName(): String      = shiftType?.name ?: "${resolvedStartTime()}-${resolvedEndTime()}"
