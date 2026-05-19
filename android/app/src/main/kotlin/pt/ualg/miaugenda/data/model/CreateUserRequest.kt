package pt.ualg.miaugenda.data.model

data class CreateUserRequest(
    val name: String,
    val email: String,
    val employeeNumber: String,
    val role: String,
    val category: String,
    val password: String
)
