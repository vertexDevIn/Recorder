package `in`.vertexdev.mobile.call_rec.models

data class StatusItem(

    val id: String,
    val name: String,
    val status_date: String?,
    val status_file: String?,
    val status_roles: String,
    val status: String,
    val created_at: String,
    val updated_at: String,
    val admin_id: String,
    val remarks: String?,
    val country: String?,
    val status_option: String?,
    val color: String?,
    val source_icon: String?,
    val total_count: Int,
    val automation_id: String,
    val tat_days: String?,
    val sort_order: String?,
    val country_name: String?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StatusItem) return false
        return id == other.id // Compare by id
    }

    override fun hashCode(): Int {
        return id.hashCode() // Use id for hash code
    }

    override fun toString(): String {
        return country_name ?: name // Fallback to name if country_name is null
    }
}