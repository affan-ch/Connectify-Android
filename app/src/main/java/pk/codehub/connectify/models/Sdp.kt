package pk.codehub.connectify.models

import kotlinx.serialization.Serializable

@Serializable
data class Sdp(
    val type: String,
    val sdp: String
)
