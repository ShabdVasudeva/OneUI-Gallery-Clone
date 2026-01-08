package apw.sec.android.gallery;

import java.io.Serializable

data class MediaFile(
    val uri: String,
    var name: String,
    val type: String,
    var folderName: String? = null,
    var groupName: String? = null,
    val dateAdded: Long? = null,
    val duration: Long? = null
) : Serializable {
    fun isVideo(): Boolean {
        return name.lowercase().endsWith(".mp4")
    }
}
