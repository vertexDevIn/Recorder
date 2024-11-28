package `in`.vertexdev.mobile.call_rec.models

import com.google.gson.annotations.SerializedName


data class LeadCategory(
    val name: String,
    val id: String,
    val color: String?,
    @SerializedName("total_count") val totalCount: String,
    val count: String
)