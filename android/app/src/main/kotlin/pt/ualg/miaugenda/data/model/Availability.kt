package pt.ualg.miaugenda.data.model

data class Availability(
    val id: Int,
    val userId: Int,
    val date: String,
    val type: String, // "PREFERRED" | "UNAVAILABLE"
    val note: String?
)
