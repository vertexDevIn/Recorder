package `in`.vertexdev.mobile.call_rec.models

data class Lead(
    val id: String,
    val fname: String,
    val lname: String,
    val lead_status: String,
    val branch: String,
    val label: String?,
    val status: String,
    val mobile: String,
    val alt_mobile: String?, //102084
    val admin_id: String,
    val reg_date: String,
    val updated_at: String,
    val source: String?,
    val vendor_id: String,
    val enrollment: String,
    val token_no: String?,
    val token_desk_no: String?,
    val token_status: String?,
    val email: String?,
    val event_checked_in: String?,
    val pre_counselling: String,
    val tat_days: String,
    val total_count: String,
    val student_name: String,
    val student_mobile: String,
    val student_source: String?,
    val student_branch: String,
    val student_enrollment: String,
    val student_status: String,
    val student_label: String,
    val student_duplicate: String,
    val student_email: String?,
    val student_assigned: String,
    val goingto_country: String?,
    val goingto_intake: String?,
    val goingto_id: String,
    val goingto_country_short: String?,
    val attempt_no: String,
    val student_label_name: String,
    val public_student_status: String?,
    val pccid: String,
    val fdate:String?,
    val ftime:String?
)