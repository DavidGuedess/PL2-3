package pt.ualg.miaugenda.data.model

data class UpdateUserRequest(
    val name: String? = null,
    val email: String? = null,
    val contact: String? = null,
    val role: String? = null,
    val category: String? = null
)
