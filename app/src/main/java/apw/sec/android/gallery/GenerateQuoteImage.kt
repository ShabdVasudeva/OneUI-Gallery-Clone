package apw.sec.android.gallery

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import android.text.*
import apw.sec.android.gallery.data.*

class GenerateQuoteImage {

    fun createQuoteImage(context: Context, hexColor: String, fontPath: String) {
        val randomQuote = RandomQuotes(context).getRandomQuote()
        val quoteText = splitIntoLines(randomQuote.quote, 15)
        val width = 1920
        val height = 1080
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val backgroundColor = try {
            Color.parseColor(hexColor)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            Color.GRAY
        }
        val textColor = getTextColor(hexColor)
        val paintBackground = Paint().apply {
            color = backgroundColor
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintBackground)
        val typefaceStyle = Typeface.createFromAsset(context.assets, fontPath)
        val paintText = TextPaint().apply {
            color = textColor
            textSize = 60f
            textAlign = Paint.Align.CENTER
            typeface = typefaceStyle
            isAntiAlias = true
        }
        val textWidth = (width * 0.8).toInt()
        val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(quoteText, 0, quoteText.length, paintText, textWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(1.2f, 1.2f)
                .setIncludePad(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(quoteText, paintText, textWidth, Layout.Alignment.ALIGN_CENTER, 1.2f, 1.2f, true)
        }
        val textX = width / 2.2f
        val textY = (height / 2f) - (staticLayout.height / 2f)
        canvas.save()
        canvas.translate(textX, textY)
        staticLayout.draw(canvas)
        canvas.restore()

        val paintAuthor = Paint(paintText).apply {
            textSize = 40f
            textAlign = Paint.Align.CENTER
        }
        val authorY = textY + staticLayout.height + 100f
        canvas.drawText("~ ${randomQuote.writer}", width / 2f, authorY, paintAuthor)
        saveImageToDownloads(context, bitmap)
    }

    fun splitIntoLines(text: String, maxWords: Int): String {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var line = StringBuilder()
        for (word in words) {
            if (line.split(" ").size < maxWords) {
                if (line.isNotEmpty()) line.append(" ")
                line.append(word)
            } else {
                lines.add(line.toString())
                line = StringBuilder(word)
            }
        }
        if (line.isNotEmpty()) lines.add(line.toString())

        return lines.joinToString("\n")
    }
    fun saveImageToDownloads(context: Context, bitmap: Bitmap) {
        val fileName = "quote_${System.currentTimeMillis()}.png"
        val fos: OutputStream?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
            val contentResolver = context.contentResolver
            val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            fos = imageUri?.let { contentResolver.openOutputStream(it) }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            fos = FileOutputStream(file)
        }
        fos?.use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
    fun getTextColor(backgroundColorHex: String): Int {
        val backgroundColor = Color.parseColor(backgroundColorHex)
        val red = Color.red(backgroundColor)
        val green = Color.green(backgroundColor)
        val blue = Color.blue(backgroundColor)
        val luminance = (0.299 * red + 0.587 * green + 0.114 * blue)
        return if (luminance > 128) Color.BLACK else Color.WHITE
    }
}