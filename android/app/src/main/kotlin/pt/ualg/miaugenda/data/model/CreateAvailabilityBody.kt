package pt.ualg.miaugenda.data.model

data class CreateAvailabilityBody(
    val date: String,
    val type: String,
    val note: String?
)
