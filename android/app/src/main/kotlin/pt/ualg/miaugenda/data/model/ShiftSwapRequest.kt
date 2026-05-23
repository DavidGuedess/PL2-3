package pt.ualg.miaugenda.data.model

data class ShiftSwapRequest(
    val id: Int,
    val requesterId: Int,
    val requesterShiftId: Int,
    val targetShiftId: Int,
    val reason: String?,
    val status: String,
    val targetAccepted: Boolean? = null,
    val approvedByName: String? = null,
    val createdAt: String,
    val requester: RequestUser? = null,
    val requesterShift: Shift? = null,
    val targetShift: Shift? = null
)
