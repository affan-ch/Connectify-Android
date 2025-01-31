package pk.codehub.connectify.models

import kotlinx.serialization.Serializable

@Serializable
data class Offer(
    val offer: String,
    val deviceId: String
)
