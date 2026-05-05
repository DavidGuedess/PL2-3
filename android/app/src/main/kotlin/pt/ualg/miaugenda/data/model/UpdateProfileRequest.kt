package pt.ualg.miaugenda.data.model

data class UpdateProfileRequest(
    val name: String,
    val contact: String?,
    val password: String?
)