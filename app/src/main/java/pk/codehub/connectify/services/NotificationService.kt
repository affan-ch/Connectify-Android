package pk.codehub.connectify.services

import android.app.RemoteInput
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Base64
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pk.codehub.connectify.models.NotificationAction
import pk.codehub.connectify.models.Notification
import pk.codehub.connectify.viewmodels.WebRTCViewModel
import java.io.ByteArrayOutputStream
import javax.inject.Inject


@AndroidEntryPoint
class NotificationService : NotificationListenerService() {

    @Inject
    lateinit var webRTCViewModel: WebRTCViewModel

//    override fun onCreate() {
//        super.onCreate()
//        // Start observing state even before listener is connected
//        Handler(Looper.getMainLooper()).post {
//            webRTCViewModel.state.observeForever { newState ->
//                    // When WebRTC channel opens, push all active notifications
//                    sendAllActiveNotifications()
//
//            }
//        }
//    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val payload = createPayloadFromSBN(sbn)
        val jsonString = Json.encodeToString(payload)
        webRTCViewModel.sendMessage(jsonString, "Notification:Posted")
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        val notificationId = sbn.id
        val notificationKey = sbn.key

        // Log or send via WebRTC
        val dismissedPayload = mapOf(
            "id" to notificationId,
            "key" to notificationKey
        )

        val json = Json.encodeToString(dismissedPayload)

        // Send to peer
        webRTCViewModel.sendMessage(json, "Notification:Removed")
    }

    private fun sendAllActiveNotifications() {
        val notifications = activeNotifications?.map { createPayloadFromSBN(it) } ?: emptyList()

        // Serialize to JSON array
        val json = Json.encodeToString(notifications)

        // Send to peer
        webRTCViewModel.sendMessage(json, "Notification:AllActive")
    }

    fun clearNotificationByKey(key: String) {
        try {
            cancelNotification(key)
        } catch (e: Exception) {
            Log.e("NotificationService", "Failed to cancel notification", e)
        }
    }

    fun performNotificationAction(id: Int, actionIndex: Int, replyText: String? = null) {
        val sbn = activeNotifications?.firstOrNull { it.id == id } ?: return
        val action = sbn.notification.actions?.getOrNull(actionIndex) ?: return

        try {
            if (replyText != null && action.remoteInputs != null) {
                val fillInIntent = Intent().apply {
                    val bundle = Bundle()
                    action.remoteInputs.forEach {
                        bundle.putCharSequence(it.resultKey, replyText)
                    }
                    RemoteInput.addResultsToIntent(action.remoteInputs, this, bundle)
                }
                action.actionIntent.send(this, 0, fillInIntent)
            } else {
                action.actionIntent.send()
            }
        } catch (e: Exception) {
            Log.e("NotificationService", "Failed to perform action", e)
        }
    }

    private fun createPayloadFromSBN(sbn: StatusBarNotification): Notification {
        val notification = sbn.notification
        val extras = notification.extras
        val packageName = sbn.packageName
        val postTime = sbn.postTime
        val id = sbn.id
        val key = sbn.key
        val isGroup = sbn.isGroup
        val groupKey = sbn.groupKey

        val title = extras.getCharSequence("android.title")?.toString()
        val content = extras.getCharSequence("android.text")?.toString()
            ?: extras.getCharSequence("android.bigText")?.toString()
            ?: extras.getCharSequence("android.summaryText")?.toString()
            ?: "Content Unavailable"

        val appName = try {
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }

        val actions = notification.actions?.mapIndexed { index, action ->
            NotificationAction(
                title = action.title?.toString(),
                index = index,
                isReplyable = action.remoteInputs != null
            )
        } ?: emptyList()

        return Notification(
            id = id,
            key = key,
            appName = appName,
            packageName = packageName,
            title = title,
            content = content,
            postTime = postTime,
            actions = actions,
            isGroup = isGroup,
            groupKey = groupKey
        )
    }


}
