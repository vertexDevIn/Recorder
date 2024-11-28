package `in`.vertexdev.mobile.call_rec.room.entitiy

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audio_files")
data class AudioFile(
    @PrimaryKey val id: String,  // Use a unique ID for the audio file


)