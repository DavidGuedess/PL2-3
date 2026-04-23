package pt.ualg.miaugenda.data.model

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
    val shiftType: ShiftType
)
