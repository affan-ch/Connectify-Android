package pk.codehub.connectify.services

import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pk.codehub.connectify.models.Notification
import pk.codehub.connectify.viewmodels.WebRTCViewModel
import javax.inject.Inject


@AndroidEntryPoint
class NotificationService : NotificationListenerService() {

    @Inject
    lateinit var webRTCViewModel: WebRTCViewModel

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val notification = sbn.notification
        val extras = notification.extras
        val packageName = sbn.packageName
        val logo = sbn.notification.smallIcon
        val title = extras.getString("android.title")
        val content = extras.getString("android.text")
        val postTime = sbn.postTime

        val packageManager = packageManager
        var appName: String

        try {
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            appName = packageManager.getApplicationLabel(applicationInfo).toString()

        } catch (e: PackageManager.NameNotFoundException) {
            appName = packageName
        }

        Log.d("Notification received:", "$appName ($packageName), $logo, $title, $content, $postTime, $extras")
        val notificationObject = Notification(appName, packageName, title.toString(), content.toString(), postTime)

        webRTCViewModel.sendMessage(Json.encodeToString(notificationObject), "notification")
    }
}
