package pt.ualg.miaugenda.data.model

data class CreateShiftSwapRequestBody(
    val requesterShiftId: Int,
    val targetShiftId: Int,
    val reason: String?
)
