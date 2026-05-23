package pt.ualg.miaugenda.data.model

data class CreateTimeOffRequestBody(
    val startDate: String,
    val endDate: String,
    val allDay: Boolean,
    val reason: String?
)
