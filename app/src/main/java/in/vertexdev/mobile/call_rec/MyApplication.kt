package `in`.vertexdev.mobile.call_rec

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

import dagger.hilt.android.HiltAndroidApp



@HiltAndroidApp
class MyApplication: Application(){

    override fun onCreate() {
        super.onCreate()
        val channelFileUploader = NotificationChannel(
            "file_uploader",
            "File Sync",
            NotificationManager.IMPORTANCE_HIGH
        )
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channelFileUploader)


    }



}