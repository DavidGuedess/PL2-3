package pt.ualg.miaugenda.data.model

data class CreateShiftRequest(
    val userId: Int,
    val date: String,
    val startTime: String,
    val endTime: String,
    val shiftTypeId: Int? = null
)
