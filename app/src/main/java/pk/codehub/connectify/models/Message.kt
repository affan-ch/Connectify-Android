package pk.codehub.connectify.models
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: Long,
    val phoneNumber: String,
    val contactName: String,
    val content: String,
    val contentType: String = "text/plain", // SMS is usually plain text
    val sender: String,
    val status: String?,
    val isRead: Int,
    val simSlot: Int?,
    val threadId: Long,
    val timestamp: Long
)
