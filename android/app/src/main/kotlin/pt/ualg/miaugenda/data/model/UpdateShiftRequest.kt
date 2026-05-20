package pt.ualg.miaugenda.data.model

data class UpdateShiftRequest(
    val shiftTypeId: Int? = null,
    val date: String? = null,
    val published: Boolean? = null
)
