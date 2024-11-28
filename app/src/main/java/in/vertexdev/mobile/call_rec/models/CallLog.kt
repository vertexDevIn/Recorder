package `in`.vertexdev.mobile.call_rec.models

data class CallLog(
    var name:String = "",
    var dateTime: String = "",
    var duration: Int = 0,
    val phoneNumber: String = "",
    val rawType: Int= 0,
    val timestamp: Long = 0L,
    val type: String = ""
)
