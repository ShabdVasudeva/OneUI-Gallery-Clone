package apw.sec.android.gallery

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore

class FetchAll(private val context: Context) {

    fun fetchMediaFiles(): List<MediaFile> {
        val mediaFiles = mutableListOf<MediaFile>()
        val contentResolver = context.contentResolver
        mediaFiles.addAll(fetchMedia(contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "Image"))
        mediaFiles.addAll(fetchMedia(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "Video"))
        mediaFiles.addAll(fetchMedia(contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "Pictures"))
        mediaFiles.addAll(fetchMedia(contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "Download"))
        return mediaFiles.sortedByDescending { it.dateAdded ?: 0L }
    }

    private fun fetchMedia(
        contentResolver: ContentResolver,
        mediaUri: Uri,
        folderName: String
    ): List<MediaFile> {
        val mediaFiles = mutableListOf<MediaFile>()
        val isVideo = mediaUri == MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        // Add DURATION and DATE_ADDED to projection
        val projection = if (isVideo) {
            arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.DATA,
                MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.Video.Media.DURATION
            )
        } else {
            arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.DATA,
                MediaStore.MediaColumns.DATE_ADDED
            )
        }

        val selection = "${MediaStore.MediaColumns.DATA} LIKE ?"
        val selectionArgs = arrayOf("%$folderName%")

        val cursor: Cursor? = contentResolver.query(
            mediaUri,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.MediaColumns.DATE_ADDED} DESC"
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameColumn = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val dateAddedColumn = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val durationColumn = if (isVideo) it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION) else -1

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val name = it.getString(nameColumn)
                val dateAdded = it.getLong(dateAddedColumn)
                val duration = if (isVideo && durationColumn != -1) it.getLong(durationColumn) else null
                val contentUri = Uri.withAppendedPath(mediaUri, id.toString())
                val type = if (isVideo) "Video" else "Image"

                mediaFiles.add(
                    MediaFile(
                        uri = contentUri.toString(),
                        name = name,
                        type = type,
                        folderName = null,
                        groupName = null,
                        dateAdded = dateAdded,
                        duration = duration
                    )
                )
            }
        }

        return mediaFiles
    }
}