package pt.ualg.miaugenda.data.model

data class RequestUser(
    val id: Int,
    val name: String,
    val employeeNumber: String
)

data class TimeOffRequest(
    val id: Int,
    val userId: Int,
    val startDate: String,
    val endDate: String,
    val allDay: Boolean,
    val reason: String?,
    val status: String,
    val approvedByName: String? = null,
    val createdAt: String,
    val user: RequestUser? = null
)
