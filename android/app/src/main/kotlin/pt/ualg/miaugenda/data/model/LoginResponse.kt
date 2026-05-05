package pt.ualg.miaugenda.data.model

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: User
)

data class User(
    val id: Int,
    val employeeNumber: String,
    val name: String,
    val email: String,
    val contact: String?,
    val role: String,
    val category: String
)