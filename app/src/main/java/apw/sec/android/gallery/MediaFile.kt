package apw.sec.android.gallery

import android.os.Parcel
import android.os.Parcelable
import android.net.Uri

data class MediaFile(
    val uri: Uri,
    val name: String,
    val type: String,
    val folderName: String?
) : Parcelable {

    fun isVideo(): Boolean{
        return name.toLowerCase().endsWith(".mp4")
    }
    
    constructor(parcel: Parcel) : this(
        parcel.readParcelable(Uri::class.java.classLoader) ?: Uri.EMPTY,
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(uri, flags)
        parcel.writeString(name)
        parcel.writeString(type)
        parcel.writeString(folderName)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<MediaFile> {
        override fun createFromParcel(parcel: Parcel): MediaFile {
            return MediaFile(parcel)
        }

        override fun newArray(size: Int): Array<MediaFile?> {
            return arrayOfNulls(size)
        }
    }
}