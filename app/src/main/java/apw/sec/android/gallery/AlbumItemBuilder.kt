    package apw.sec.android.gallery

    import apw.sec.android.gallery.MediaFile

    object AlbumItemBuilder {

        // Albums that should appear first in CUSTOM_ORDER
        private val PRIORITY_ALBUMS = listOf(
            "Recent",
            "Camera",
            "Screenshots",
            "Download",
            "Videos",
            "WhatsApp Images"
        )

        private val PINNED_ALBUMS = listOf(
            "Recent",
            "Camera",
            "Videos"
        )


        fun build(
            mediaFiles: List<MediaFile>,
            sortType: AlbumSortType
        ): List<AlbumItem> {

            val result = mutableListOf<AlbumItem>()
            val usedFolders = mutableSetOf<String>()

            // Build GROUP albums
            val groupedFiles = mediaFiles
                .filter { !it.groupName.isNullOrBlank() }
                .groupBy { it.groupName!! }

            groupedFiles.forEach { (groupName, files) ->

                val albums = files
                    .groupBy { it.folderName ?: "Unknown" }
                    .map { (folderName, media) ->
                        usedFolders.add(folderName)

                        val sortedMedia = media.sortedByDescending { it.dateAdded ?: 0L }
                        AlbumItem.Album(folderName, sortedMedia)
                    }

                    .sortedBy { it.folderName.lowercase() }

                if (albums.isNotEmpty()) {
                    result.add(AlbumItem.Group(groupName, albums))
                }
            }

            // Build UNGROUPED albums
            val ungroupedAlbums = mediaFiles
                .filter { it.folderName !in usedFolders }
                .groupBy { it.folderName ?: "Unknown" }
                .map { (folderName, media) ->
                    val sortedMedia = media.sortedByDescending { it.dateAdded ?: 0L }
                    AlbumItem.Album(folderName, sortedMedia)
                }


            result.addAll(ungroupedAlbums)

            return result.sortedWith(globalComparator(sortType))
        }

        // Sorting logic
        private fun globalComparator(sortType: AlbumSortType): Comparator<AlbumItem> {

            val normalComparator: Comparator<AlbumItem> = when (sortType) {

                AlbumSortType.CUSTOM -> Comparator { a, b ->
                    val nameA = a.displayName()
                    val nameB = b.displayName()

                    val indexA = PRIORITY_ALBUMS.indexOf(nameA)
                    val indexB = PRIORITY_ALBUMS.indexOf(nameB)

                    when {
                        indexA != -1 && indexB != -1 -> indexA - indexB
                        indexA != -1 -> -1
                        indexB != -1 -> 1
                        else -> nameA.compareTo(nameB, ignoreCase = true)
                    }
                }

                AlbumSortType.NAME_ASC ->
                    compareBy { it.displayName().lowercase() }

                AlbumSortType.NAME_DESC ->
                    compareByDescending { it.displayName().lowercase() }

                AlbumSortType.COUNT_ASC ->
                    compareBy { it.itemCount() }

                AlbumSortType.COUNT_DESC ->
                    compareByDescending { it.itemCount() }
            }

            return Comparator { a, b ->

                val nameA = a.displayName()
                val nameB = b.displayName()

                val pinnedIndexA = PINNED_ALBUMS.indexOfFirst { it.equals(nameA, true) }
                val pinnedIndexB = PINNED_ALBUMS.indexOfFirst { it.equals(nameB, true) }

                when {
                    pinnedIndexA != -1 && pinnedIndexB != -1 ->
                        pinnedIndexA - pinnedIndexB

                    pinnedIndexA != -1 -> -1
                    pinnedIndexB != -1 -> 1
                    else -> normalComparator.compare(a, b)
                }
            }
        }


        private fun AlbumItem.displayName(): String =
            when (this) {
                is AlbumItem.Album -> folderName
                is AlbumItem.Group -> groupName
            }

        private fun AlbumItem.itemCount(): Int =
            when (this) {
                is AlbumItem.Album -> media.size
                is AlbumItem.Group -> albums.sumOf { it.media.size }
            }
    }
