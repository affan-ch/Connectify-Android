package pk.codehub.connectify.models

import kotlinx.serialization.Serializable

@Serializable
data class Notification(
    val id: Int,
    val key: String,
    val appName: String,
    val packageName: String,
    val title: String?,
    val content: String?,
    val postTime: Long,
    val actions: List<NotificationAction>,
    val isGroup: Boolean,
    val groupKey: String?
)