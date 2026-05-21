package pt.ualg.miaugenda.data.model

data class UpdateShiftRequest(
    val startTime: String? = null,
    val endTime: String? = null,
    val date: String? = null,
    val published: Boolean? = null,
    val shiftTypeId: Int? = null
)
