package apw.sec.android.gallery.securenv

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import java.io.*
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider

object ImageUtils {

    fun shareImage(context: Context, imagePath: String) {
        val file = File(imagePath)
        if (!file.exists()) {
            return
        }
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
    }

    fun saveBitmapToFile(context: Context, bitmap: Bitmap): String? {
        val fileName = "apwpvtspc_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, fileName)
        return try {
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            file.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun deletePath(filePath: String): Boolean {
        val file = File(filePath)
        return if (file.exists()) file.delete() else false
    }
    
    fun saveToLocal(context: Context, imagePath: String): String? {
        val sourceFile = File(imagePath)
        if (!sourceFile.exists()) return null
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        if (!picturesDir.exists()) {
            picturesDir.mkdirs()
        }
        val destinationFile = File(picturesDir, "apwpvtspc_${System.currentTimeMillis()}.jpg")
        return try {
            FileInputStream(sourceFile).use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }
            val uri = Uri.fromFile(destinationFile)
            @Suppress("DEPRECATION")
            context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
            destinationFile.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}