package `in`.vertexdev.mobile.call_rec.models

data class Employee(
    val profile_pic: String?, // Nullable since it can be null
    val mname: String?, // Nullable since it can be null
    val designation: String?, // Nullable since it can be null
    val branch: String?, // Nullable since it can be null
    val fname: String,
    val lname: String?,
    val mobile: String,
    val email: String,
    val id: String,
    val type: String,
    val calendly: String?, // Nullable since it can be null
    val status: String
){
    override fun toString(): String {
        return fname +" " +  lname // This will be displayed in the dropdown
    }
}