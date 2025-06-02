package pk.codehub.connectify.models
import kotlinx.serialization.Serializable

@Serializable
data class CallLogEntry(
    val phoneNumber: String,
    val contactName: String,
    val callType: String,
    val duration: Int?,
    val simSlot: Int?,
    val isRead: Int?,
    val isNew: Int?,
    val timestamp: Long,
)