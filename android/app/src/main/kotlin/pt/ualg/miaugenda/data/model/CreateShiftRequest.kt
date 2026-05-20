package pt.ualg.miaugenda.data.model

data class CreateShiftRequest(
    val userId: Int,
    val shiftTypeId: Int,
    val date: String
)
