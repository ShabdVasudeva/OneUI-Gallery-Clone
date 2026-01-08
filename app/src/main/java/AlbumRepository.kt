package apw.sec.android.gallery

import apw.sec.android.gallery.AlbumItem

object AlbumRepository {
    var albumItems: List<AlbumItem> = emptyList()
    // groupName -> list of album folder names
    val groups: MutableMap<String, List<String>> = mutableMapOf()
}
