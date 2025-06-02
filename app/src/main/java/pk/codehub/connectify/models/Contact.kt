package pk.codehub.connectify.models
import kotlinx.serialization.Serializable

@Serializable
data class Contact(
    val firstName: String,
    val lastName: String? = null,
    val phoneNumber: String,
    val email: String? = null,
    val company: String? = null,
    val dob: String? = null,
    val address: String? = null,
    val notes: String? = null,
    val photoBase64: String? = null,
)