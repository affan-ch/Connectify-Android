package pk.codehub.connectify.services

import android.app.RemoteInput
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pk.codehub.connectify.models.Notification
import pk.codehub.connectify.models.NotificationAction
import pk.codehub.connectify.viewmodels.WebRTCViewModel
import javax.inject.Inject

@AndroidEntryPoint
class NotificationService : NotificationListenerService() {

    @Inject
    lateinit var webRTCViewModel: WebRTCViewModel

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            val payload = createPayloadFromSBN(sbn)
            val jsonString = Json.encodeToString(payload)

            Handler(Looper.getMainLooper()).post {
                try {
                    webRTCViewModel.sendMessage(jsonString, "Notification:Posted")
                } catch (e: Exception) {
                    Log.e("NotificationService", "Failed to send posted notification", e)
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationService", "Failed to process posted notification", e)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        try {
            val dismissedPayload = mapOf(
                "id" to sbn.id,
                "key" to sbn.key
            )

            val json = Json.encodeToString(dismissedPayload)

            Handler(Looper.getMainLooper()).post {
                try {
                    webRTCViewModel.sendMessage(json, "Notification:Removed")
                } catch (e: Exception) {
                    Log.e("NotificationService", "Failed to send removed notification", e)
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationService", "Failed to process removed notification", e)
        }
    }

    fun sendAllActiveNotifications() {
        try {
            val notifications = activeNotifications?.map { createPayloadFromSBN(it) } ?: emptyList()
            val json = Json.encodeToString(notifications)

            Handler(Looper.getMainLooper()).post {
                try {
                    webRTCViewModel.sendMessage(json, "Notification:AllActive")
                } catch (e: Exception) {
                    Log.e("NotificationService", "Failed to send all active notifications", e)
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationService", "Failed to serialize active notifications", e)
        }
    }

    fun clearNotificationByKey(key: String) {
        try {
            cancelNotification(key)
        } catch (e: Exception) {
            Log.e("NotificationService", "Failed to cancel notification with key $key", e)
        }
    }

    fun performNotificationAction(id: Int, actionIndex: Int, replyText: String? = null) {
        try {
            val sbn = activeNotifications?.firstOrNull { it.id == id } ?: run {
                Log.w("NotificationService", "Notification with id $id not found")
                return
            }

            val action = sbn.notification.actions?.getOrNull(actionIndex) ?: run {
                Log.w("NotificationService", "Action index $actionIndex not found for notification id $id")
                return
            }

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
            Log.e("NotificationService", "Failed to perform action on notification $id", e)
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
            Log.w("NotificationService", "App name not found for package: $packageName")
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
