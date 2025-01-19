package pk.codehub.connectify.models

import kotlinx.serialization.Serializable

@Serializable
data class Notification(
    val appName: String,
    val packageName: String,
    val title: String,
    val content: String,
    val postTime: Long
)