package pk.codehub.connectify.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import pk.codehub.connectify.services.ForegroundService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            // Start Foreground Service
            context.startForegroundService(Intent(context, ForegroundService::class.java))

            // Accessibility service starts manually via user enabling it in settings
        }
    }
}
