package pt.ualg.miaugenda.data.model

data class WeekAssignment(
    val id: Int,
    val userId: Int,
    val weekStart: String,
    val user: User
)

data class CreateWeekAssignmentRequest(
    val userId: Int,
    val weekStart: String
)
