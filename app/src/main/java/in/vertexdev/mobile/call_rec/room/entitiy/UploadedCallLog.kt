package `in`.vertexdev.mobile.call_rec.room.entitiy

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "uploaded_call_logs")
data class UploadedCallLog(
    @PrimaryKey val id: String, // Use the call log ID as the primary key
    val name: String,
    val logsUploaded:Boolean = false,
    val fileUploaded: Boolean = false,
    val ignoreThis:Boolean = false,
    val callStarted:Long,// Store the timestamp in milliseconds since Unix epoch
    val callEndTimeStamp: Long,// Store the timestamp in milliseconds since Unix epoch
    val callDuration:String,
    val callType:String,
    val isLead:String,
    val cleanedNumber:String,
    val number:String,
    val leadId:String,
    val fileUploadAttempts:Int =0,
    val lastAttemptError:String = ""

    )