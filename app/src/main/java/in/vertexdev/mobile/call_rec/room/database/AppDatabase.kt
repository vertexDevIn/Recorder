package `in`.vertexdev.mobile.call_rec.room.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import `in`.vertexdev.mobile.call_rec.room.dao.UploadedCallLogDao
import `in`.vertexdev.mobile.call_rec.room.entitiy.AudioFile
import `in`.vertexdev.mobile.call_rec.room.entitiy.UploadedCallLog

@Database(entities = [UploadedCallLog::class,AudioFile::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun uploadedCallLogDao(): UploadedCallLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .fallbackToDestructiveMigration() // Enables destructive migration
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
