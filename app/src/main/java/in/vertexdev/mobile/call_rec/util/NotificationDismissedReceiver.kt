package `in`.vertexdev.mobile.call_rec.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import `in`.vertexdev.mobile.call_rec.services.SyncDataService2

class NotificationDismissedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Check if the service is still running and re-show the notification
        val serviceIntent = Intent(context, SyncDataService2::class.java)
        serviceIntent.action = SyncDataService2.Actions.START.toString()
        context.startService(serviceIntent)
    }
}