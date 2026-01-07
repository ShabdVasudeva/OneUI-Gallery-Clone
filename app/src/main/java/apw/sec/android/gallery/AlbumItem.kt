package apw.sec.android.gallery

import apw.sec.android.gallery.MediaFile

sealed class AlbumItem {

    data class Album(
        val folderName: String,
        val media: List<MediaFile>
    ) : AlbumItem()

    data class Group(
        val groupName: String,
        val albums: List<Album>
    ) : AlbumItem()
}
