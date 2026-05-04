package pt.ualg.miaugenda.data.model

data class ShiftUser(
    val id: Int,
    val name: String,
    val employeeNumber: String,
    val role: String
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
    val shiftTypeId: Int,
    val date: String,
    val user: ShiftUser? = null,
    val shiftType: ShiftType
)