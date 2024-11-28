package `in`.vertexdev.mobile.call_rec.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import `in`.vertexdev.mobile.call_rec.room.entitiy.AudioFile
import `in`.vertexdev.mobile.call_rec.room.entitiy.UploadedCallLog
import kotlinx.coroutines.flow.Flow

class MainDao {
}

@Dao
interface UploadedCallLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(callLog: UploadedCallLog)



    @Query("SELECT * FROM uploaded_call_logs WHERE ignoreThis = 0 AND fileUploadAttempts < 5  ORDER BY callStarted DESC")
    fun getAllUploadedCallLogs(): Flow<List<UploadedCallLog>>

    @Query("SELECT * FROM uploaded_call_logs WHERE id = :id LIMIT 1")
    suspend fun getUploadedCallLogById(id: String): UploadedCallLog?


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudioFile(audioFile: AudioFile)

    @Query("SELECT * FROM audio_files WHERE id = :fileId LIMIT 1")
    suspend fun getAudioFileById(fileId: String): AudioFile?

    @Query("SELECT * FROM audio_files")
    suspend fun getAllAudioFiles(): List<AudioFile>

    @Query("SELECT * FROM uploaded_call_logs WHERE ignoreThis = 0 AND fileUploaded = 0")
    fun observePendingUploads(): List<UploadedCallLog>


    @Query("UPDATE uploaded_call_logs SET fileUploadAttempts = :attempts, lastAttemptError = :error WHERE id = :id")
    suspend fun updateFileUploadAttemptsAndError(id: String, attempts: Int, error: String)


    @Query(
        """
        UPDATE uploaded_call_logs 
        SET fileUploaded = :fileUploaded, 
            fileUploadAttempts = :fileUploadAttempts, 
            lastAttemptError = :lastAttemptError 
        WHERE id = :id
    """
    )

    suspend fun updateFileUploadStatus(
        id: String,
        fileUploaded: Boolean,
        fileUploadAttempts: Int,
        lastAttemptError: String
    )

    @Query("""
    SELECT * FROM uploaded_call_logs 
    WHERE CAST(callDuration AS INTEGER) BETWEEN (:fileDurationSeconds - 2) AND (:fileDurationSeconds + 2)
    AND name LIKE '%' || :name || '%'
    ORDER BY ABS(callStarted - :timestamp) ASC 
    LIMIT 1
""")
    suspend fun findClosestMatchingCallLogName(
        timestamp: Long,
        fileDurationSeconds: Int,
        name: String
    ): UploadedCallLog?


    @Query("""
    SELECT * FROM uploaded_call_logs 
    WHERE CAST(callDuration AS INTEGER) BETWEEN (:fileDurationSeconds - 2) AND (:fileDurationSeconds + 2)
    AND REPLACE(number, '+', '') LIKE '%' || REPLACE(:number, '+', '') || '%'
    ORDER BY ABS(callStarted - :timestamp) ASC 
    LIMIT 1
""")
    suspend fun findClosestMatchingCallLogNumber(
        timestamp: Long,
        fileDurationSeconds: Int,
        number: String
    ): UploadedCallLog?

}

