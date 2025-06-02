package pk.codehub.connectify.models

import kotlinx.serialization.Serializable

@Serializable
data class AppIcon(
    val appName: String,
    val packageName: String,
    val packageVersion: String,
    val appIconBase64: String
)