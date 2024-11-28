package `in`.vertexdev.mobile.call_rec.models

import android.net.Uri

data class Folder(
    val id: String,
    val name: String,
    val thumbnailUri: Uri?,  // Thumbnail URI
    var itemCount: Int,        // Number of items
    val folderPath: String,
    val folderType:Type,

    )
enum class Type{
    Images,
    Video,
    Audio,
    Document,
    Others
}