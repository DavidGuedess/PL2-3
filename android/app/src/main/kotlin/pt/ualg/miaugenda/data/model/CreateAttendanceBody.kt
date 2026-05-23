package pt.ualg.miaugenda.data.model

data class CreateAttendanceBody(
    val type: String,
    val note: String? = null
)
