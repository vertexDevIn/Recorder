package `in`.vertexdev.mobile.call_rec.models

import com.google.gson.annotations.SerializedName

data class UserResponse(
    val id: String,
    val fname: String,
    val lname: String,
    val type: String,
    val branch: String,
    val mobile: String,
    val email: String,
    val public_api_key: String,
    @SerializedName("key_authenticated") val keyAuthenticated: Int,
    @SerializedName("custom_role") val customRole: String,
    @SerializedName("data_access") val dataAccess: String,
    @SerializedName("main_branch") val mainBranch: String,
    @SerializedName("onetime_token") val oneTimeToken: String?,
)