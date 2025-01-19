package pk.codehub.connectify.models

import kotlinx.serialization.Serializable

@Serializable
data class Packet(
    val type: String,
    val content: String,
    val timestamp: String,
    val sender: String
)
