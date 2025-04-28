package pk.codehub.connectify.models

import kotlinx.serialization.Serializable


@Serializable
data class NotificationAction(
    val title: String?,
    val index: Int,
    val isReplyable: Boolean
)
