package apw.sec.android.gallery

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore

class Albums(private val context: Context) {

    fun fetchAlbums(): List<MediaFile> {
        val mediaFiles = mutableListOf<MediaFile>()
        val contentResolver = context.contentResolver
        val mediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME
        )

        val cursor: Cursor? = contentResolver.query(
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

                mediaFiles.add(MediaFile(contentUri.toString(), name, "Image", folderName))
            }
        }

        return mediaFiles
    }
}