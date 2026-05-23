package pt.ualg.miaugenda.data.model

data class ActiveEmployee(
    val userId: Int,
    val name: String,
    val employeeNumber: String,
    val clockedInSince: String,
    val shiftStart: String? = null,
    val shiftEnd: String? = null
)
