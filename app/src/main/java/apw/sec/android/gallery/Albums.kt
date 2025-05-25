package apw.sec.android.gallery

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore

class Albums(private val context: Context) {

    fun fetchAlbums(): List<MediaFile> {
        val mediaFiles = mutableListOf<MediaFile>()

        // Fetch Images
        mediaFiles.addAll(fetchMedia(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            "Image"
        ))

        mediaFiles.addAll(fetchMedia(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            "Video"
        ))

        return mediaFiles.sortedBy { it.folderName?.lowercase() ?: "" }
    }

    private fun fetchMedia(mediaUri: Uri, type: String): List<MediaFile> {
        val list = mutableListOf<MediaFile>()
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME
        )

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

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val name = it.getString(nameColumn)
                val path = it.getString(dataColumn)
                val folderName = it.getString(bucketColumn) ?: "Unknown"
                val contentUri = Uri.withAppendedPath(mediaUri, id.toString())

                list.add(MediaFile(contentUri.toString(), name, type, folderName))
            }
        }

        return list
    }
}
