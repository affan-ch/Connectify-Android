package pk.codehub.connectify.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import pk.codehub.connectify.services.ForegroundService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            // Start Foreground Service
            Log.d("BootReceiver", "Starting Foreground Service")
            context.startForegroundService(Intent(context, ForegroundService::class.java))
        }
    }
}
