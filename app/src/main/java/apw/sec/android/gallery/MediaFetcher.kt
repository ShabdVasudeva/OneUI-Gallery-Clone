package apw.sec.android.gallery

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore

class MediaFetcher(private val context: Context) {

    fun fetchMediaFiles(): List<MediaFile> {
        val mediaFiles = mutableListOf<MediaFile>()
        val contentResolver = context.contentResolver
        mediaFiles.addAll(fetchMediaFromFolder(contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "DCIM"))
        mediaFiles.addAll(fetchMediaFromFolder(contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "Pictures/Screenshots"))
        mediaFiles.addAll(fetchMediaFromFolder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "DCIM"))

        return mediaFiles
    }

    fun fetchMediaFromFolder(
        contentResolver: ContentResolver,
        mediaUri: Uri,
        folderName: String
    ): List<MediaFile> {
        val mediaFiles = mutableListOf<MediaFile>()
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATA
        )

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

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val name = it.getString(nameColumn)
                val contentUri = Uri.withAppendedPath(mediaUri, id.toString())
                val type = if (mediaUri == MediaStore.Images.Media.EXTERNAL_CONTENT_URI) "Image" else "Video"
                mediaFiles.add(MediaFile(contentUri.toString(), name, type, null))
            }
        }

        return mediaFiles
    }
}