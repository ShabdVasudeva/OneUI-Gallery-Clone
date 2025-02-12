package apw.sec.android.gallery.securenv

import android.os.Parcel
import android.os.Parcelable
import android.net.Uri

data class ImageItem(
    val imagePath: String
) : Parcelable{

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(imagePath)
    }

    override fun describeContents(): Int = 0
    
    companion object CREATOR : Parcelable.Creator<ImageItem> {
        
        override fun createFromParcel(parcel: Parcel): ImageItem {
            return ImageItem(parcel)
        }

        override fun newArray(size: Int): Array<ImageItem?> {
            return arrayOfNulls(size)
        }
    }
}