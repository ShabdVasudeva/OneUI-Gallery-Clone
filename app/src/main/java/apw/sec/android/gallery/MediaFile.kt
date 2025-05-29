package apw.sec.android.gallery;

import java.io.Serializable

data class MediaFile(
    val uri: String,
    val name: String,
    val type: String,
    var folderName: String? = null
) : Serializable {
    fun isVideo(): Boolean {
        return name.lowercase().endsWith(".mp4")
    }
}
