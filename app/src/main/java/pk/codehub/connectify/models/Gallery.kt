package pk.codehub.connectify.models
import kotlinx.serialization.Serializable

@Serializable
data class Gallery(
    val id: Long? = null,
    val mediaId: String,
    val fileName: String,
    val filePath: String,
    val mediaType: String? = null, // "image" or "video"
    val mimeType: String,
    val size: Long,
    val width: Int? = null,
    val height: Int? = null,
    val duration: Int? = null, // in milliseconds (for video)
    val dateTaken: Long,
    val dateModified: Long? = null,
    val isFavorite: Int? = null,
    val synced: Int? = null,
    val thumbnailBase64: String? = null, // Base64 encoded thumbnail
)
