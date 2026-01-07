package apw.sec.android.gallery

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore

class Albums(private val context: Context) {

    fun fetchAlbums(): List<MediaFile> {
        // Fetch all images and videos first
        val allMedia = mutableListOf<MediaFile>()
        allMedia.addAll(fetchMedia(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "Image"))
        allMedia.addAll(fetchMedia(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "Video"))

        // Create a new list for "Recent" album with folderName = "Recent"
        val recentMedia = allMedia
            .sortedByDescending { it.dateAdded ?: 0 }
            .map { it.copy(folderName = "Recent") }

        // Create a new list for "Videos" album with folderName = "Videos"
        val videosMedia = allMedia
            .filter { it.isVideo() }
            .map { it.copy(folderName = "Videos") }

        // Keep original albums with their original folder names intact
        val realAlbumsMedia = allMedia

        // Combine all media files together for the adapter to group by folderName
        val combinedMedia = mutableListOf<MediaFile>()
        combinedMedia.addAll(recentMedia)
        combinedMedia.addAll(videosMedia)
        combinedMedia.addAll(realAlbumsMedia)

        return combinedMedia
    }

    private fun fetchMedia(mediaUri: Uri, type: String): List<MediaFile> {
        val list = mutableListOf<MediaFile>()
        val isVideo = mediaUri == MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        val projection = if (isVideo) {
            arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.DATA,
                MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
                MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.Video.Media.DURATION
            )
        } else {
            arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.DATA,
                MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
                MediaStore.MediaColumns.DATE_ADDED
            )
        }

        val cursor: Cursor? = context.contentResolver.query(
            mediaUri,
            projection,
            null,
            null,
            "${MediaStore.MediaColumns.DATE_ADDED} DESC"
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameColumn = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
            val bucketColumn = it.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            val dateAddedColumn = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val durationColumn = if (isVideo) it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION) else -1

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val name = it.getString(nameColumn)
                val path = it.getString(dataColumn)
                val folderName = it.getString(bucketColumn) ?: "Unknown"
                val dateAdded = it.getLong(dateAddedColumn)
                val duration = if (isVideo && durationColumn != -1) it.getLong(durationColumn) else null
                val contentUri = Uri.withAppendedPath(mediaUri, id.toString())

                list.add(
                    MediaFile(
                        uri = contentUri.toString(),
                        name = name,
                        type = type,
                        folderName = folderName,
                        groupName = null,
                        dateAdded = dateAdded,
                        duration = duration
                    )
                )
            }
        }

        return list
    }
}