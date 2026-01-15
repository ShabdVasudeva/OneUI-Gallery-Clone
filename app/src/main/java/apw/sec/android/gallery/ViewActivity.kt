package apw.sec.android.gallery
import android.content.*
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.net.*
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.text.*
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.*
import androidx.viewpager2.widget.ViewPager2
import apw.sec.android.gallery.data.MediaHub
import apw.sec.android.gallery.databinding.*
import java.io.*
import apw.sec.android.gallery.R
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import java.io.FileOutputStream

class ViewActivity : AppCompatActivity() {
    private var _binding: ActivityViewBinding? = null
    private val binding
        get() = _binding!!
    private lateinit var imageList: MutableList<MediaFile>
    private lateinit var adapter: ImagePagerAdapter
    private lateinit var filmstripAdapter: FilmstripAdapter
    private var startPosition: Int = 0
    private var key: String? = ""
    private var isUIVisible = true
    private var initialTouchY = 0f
    private val SWIPE_THRESHOLD = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Remove toolbar setup
        binding.fabBack.setOnClickListener { finishWithAnimation() }

        binding.root.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialTouchY = event.y
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val deltaY = event.y - initialTouchY
                    if (deltaY > SWIPE_THRESHOLD) {
                        finishWithAnimation()
                    }
                    true
                }
                else -> false
            }
        }

        key = intent.getStringExtra("media_key")
        val sharedPreferences =
            getSharedPreferences("apw_gallery_preferences", Context.MODE_PRIVATE)
        val isEnabled = sharedPreferences.getBoolean("ENABLE_FILMSTRIP", true)
        if (isEnabled) {
            binding.filmStripRecyclerView.visibility = View.VISIBLE
        } else {
            binding.filmStripRecyclerView.visibility = View.GONE
        }
        startPosition = intent.getIntExtra("position", 0)
        imageList = MediaHub.get(key!!) as MutableList<MediaFile>

        // Ensure layout goes behind status and nav bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Apply window insets to FAB instead of toolbar
        ViewCompat.setOnApplyWindowInsetsListener(binding.fabBack) { view, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val params = view.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = topInset + 16 // 16dp base margin + status bar height
            view.layoutParams = params
            insets
        }

        // Set black background for system bars
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        // Disable light icons (use light-on-dark)
        val controller = WindowInsetsControllerCompat(window, binding.root)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false

        adapter = ImagePagerAdapter(this@ViewActivity, imageList) { toggleUIVisibility() }
        binding.viewPager.adapter = adapter
        binding.viewPager.setCurrentItem(startPosition, false)

        filmstripAdapter =
            FilmstripAdapter(imageList) { position ->
                binding.viewPager.setCurrentItem(position, true)
            }
        binding.filmStripRecyclerView.layoutManager =
            LinearLayoutManager(this@ViewActivity, LinearLayoutManager.HORIZONTAL, false)
        binding.filmStripRecyclerView.adapter = filmstripAdapter

        filmstripAdapter.setSelectedPosition(startPosition)

        binding.filmStripRecyclerView.post {
            val recyclerWidth = binding.filmStripRecyclerView.width

            val itemWidthPx = resources.displayMetrics.density * 100  // your FilmImageView width
            val sidePadding = (recyclerWidth / 2f - itemWidthPx / 2f).toInt()

            binding.filmStripRecyclerView.setPadding(
                sidePadding,
                binding.filmStripRecyclerView.paddingTop,
                sidePadding,
                binding.filmStripRecyclerView.paddingBottom
            )

            binding.filmStripRecyclerView.clipToPadding = false
        }

        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(binding.filmStripRecyclerView)

        binding.viewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    filmstripAdapter.setSelectedPosition(position)
                    binding.filmStripRecyclerView.smoothScrollToPosition(position)
                    updatePlayButtonVisibility(position)
                }
            }
        )

        // Set play button click listener
        binding.playButton.setOnClickListener {
            val currentPosition = binding.viewPager.currentItem
            val mediaFile = imageList[currentPosition]
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(mediaFile.uri), "video/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        }

        // Update play button visibility for initial position
        updatePlayButtonVisibility(startPosition)

        binding.bottomBar.seslSetGroupDividerEnabled(true)

        binding.bottomBar.itemTextColor = ColorStateList.valueOf(Color.WHITE)
        binding.bottomBar.itemIconTintList = ColorStateList.valueOf(Color.WHITE)
        binding.bottomBar.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.share -> {
                    val current = binding.viewPager.currentItem
                    val currentUri = imageList[current].uri.toUri()
                    shareImage(this, currentUri)
                    true
                }
                R.id.edit -> {
                    val pos = binding.viewPager.currentItem
                    editImage(this, imageList[pos].uri.toUri())
                    true
                }
                R.id.favourite -> {
                    Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.delete -> {
                    val builder = AlertDialog.Builder(this@ViewActivity)
                    builder.setTitle("Delete this photo?")
                    builder.setMessage(
                        "This photo will be deleted from this device.\nAlso can not be accessed or restored from gallery."
                    )
                    builder.setNegativeButton("Cancel", null)
                    builder.setPositiveButton("Delete") { _, _ ->
                        val pos = binding.viewPager.currentItem
                        deleteImageFromUri(this@ViewActivity, imageList[pos].uri.toUri(), pos)
                    }
                    builder.show()
                    true
                }
                R.id.info -> {
                    val pos = binding.viewPager.currentItem
                    showImageInfoDialog(this, imageList[pos].uri.toUri())
                    true
                }
                R.id.rename -> {
                    val pos = binding.viewPager.currentItem
                    showRenameDialog(imageList[pos], pos)
                    true
                }
                R.id.setaswallpaper -> {
                    val pos = binding.viewPager.currentItem
                    setAsWallpaper(imageList[pos].uri.toUri())
                    true
                }
                R.id.print -> {
                    val pos = binding.viewPager.currentItem
                    print(imageList[pos].uri.toUri())
                    true
                }
                else -> false
            }
        }
    }

    private fun updatePlayButtonVisibility(position: Int) {
        val mediaFile = imageList[position]
        val isVideo = isVideoFile(mediaFile.uri.toUri())
        if (isVideo && isUIVisible) {
            binding.playButton.visibility = View.VISIBLE
        } else {
            binding.playButton.visibility = View.GONE
        }
    }

    private fun isVideoFile(uri: Uri): Boolean {
        val mimeType = contentResolver.getType(uri)
        return mimeType?.startsWith("video/") == true
    }

    fun print(imageUri: Uri) {
        try {
            val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
            val jobName = "Photo Print"

            // Use PrintHelper for better image printing support
            val printHelper = androidx.print.PrintHelper(this).apply {
                scaleMode = androidx.print.PrintHelper.SCALE_MODE_FIT
            }

            try {
                val bitmap = contentResolver.openInputStream(imageUri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }

                if (bitmap != null) {
                    printHelper.printBitmap(jobName, bitmap)
                } else {
                    Toast.makeText(this, "Unable to load image", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error loading image for print", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Unable to print image", Toast.LENGTH_SHORT).show()
        }
    }

    fun setAsWallpaper(imageUri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_ATTACH_DATA).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                setDataAndType(imageUri, "image/*")
                putExtra("mimeType", "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Set as wallpaper"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Unable to set wallpaper", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteImageFromUri(context: Context, imageUri: Uri, pos: Int): Boolean {
        return try {
            val contentResolver: ContentResolver = context.contentResolver
            val rowsDeleted = contentResolver.delete(imageUri, null, null)

            if (rowsDeleted > 0) {
                imageList.removeAt(pos)
                filmstripAdapter.notifyItemRemoved(pos)
                adapter.notifyItemRemoved(pos)
                if (imageList.isEmpty()) {
                    setResult(
                        RESULT_OK,
                        Intent().apply {
                            putExtra("deleted_position", pos)
                            putExtra("media_key", key)
                        }
                    )
                    finish()
                } else {
                    setResult(
                        RESULT_OK,
                        Intent().apply {
                            putExtra("deleted_position", pos)
                            putExtra("media_key", key)
                        }
                    )
                    val nextIndex = if (pos < imageList.size) pos else imageList.lastIndex
                    binding.viewPager.setCurrentItem(nextIndex, false)
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                context,
                "Please Allow All files access permission first",
                Toast.LENGTH_SHORT
            )
                .show()
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            context.startActivity(intent)
            false
        }
    }

    fun editImage(context: Context, imageUri: Uri) {
        try {
            var intent = Intent(Intent.ACTION_EDIT)
            intent.setDataAndType(imageUri, "image/*")
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(intent, null))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "No editor found!", Toast.LENGTH_SHORT).show()
        }
    }

    fun showImageInfoDialog(context: Context, imageUri: Uri) {
        val contentResolver = context.contentResolver
        val filePath: String? = getFilePathFromUri(context, imageUri)
        val folderName = filePath?.substringBeforeLast("/") ?: "Unknown"
        val fileName = filePath?.substringAfterLast("/") ?: "Unknown"
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeStream(contentResolver.openInputStream(imageUri), null, options)
        val resolution = "${options.outWidth} x ${options.outHeight}"
        val fileSize = filePath?.let { File(it).length() } ?: 0L
        val fileSizeMB = String.format("%.2f MB", fileSize / (1024.0 * 1024.0))

        // Get date taken and date modified
        var dateTaken: String? = null
        var dateModified: String? = null
        contentResolver.query(imageUri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val dateTakenIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
                val dateModifiedIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)
                if (dateTakenIndex != -1) {
                    val dt = cursor.getLong(dateTakenIndex)
                    if (dt > 0)
                        dateTaken =
                            java.text.SimpleDateFormat(
                                "dd MMM yyyy, hh:mm a",
                                java.util.Locale.getDefault()
                            )
                                .format(java.util.Date(dt))
                }
                if (dateModifiedIndex != -1) {
                    val dm = cursor.getLong(dateModifiedIndex)
                    if (dm > 0)
                        dateModified =
                            java.text.SimpleDateFormat(
                                "dd MMM yyyy, hh:mm a",
                                java.util.Locale.getDefault()
                            )
                                .format(java.util.Date(dm * 1000))
                }
            }
        }

        // Mime type
        val mimeType = contentResolver.getType(imageUri) ?: "Unknown"

        // Build info message
        val message = buildString {
            append("📂 <b>Folder:</b> $folderName<br>")
            append("📄 <b>File Name:</b> $fileName<br>")
            append("🖼 <b>Resolution:</b> $resolution<br>")
            append("📏 <b>File Size:</b> $fileSizeMB<br>")
            append("📑 <b>MIME Type:</b> $mimeType<br>")
            append("🕒 <b>Date Taken:</b> ${dateTaken ?: "Unknown"}<br>")
            append("📝 <b>Date Modified:</b> ${dateModified ?: "Unknown"}<br>")
            append("📁 <b>File Path:</b> $filePath")
        }

        val builder = AlertDialog.Builder(context)
        builder.setTitle("Image Info")
        builder.setMessage(Html.fromHtml(message, Html.FROM_HTML_MODE_LEGACY))
        builder.setPositiveButton("OK", null)
        builder.show()
    }

    fun getImageTime(context: Context, uri: Uri): String? {
        val contentResolver = context.contentResolver
        var dateTaken: Long? = null
        var dateModified: Long? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val dateTakenIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
                val dateModifiedIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)
                if (dateTakenIndex != -1) {
                    val dt = cursor.getLong(dateTakenIndex)
                    if (dt > 0) dateTaken = dt
                }
                if (dateModifiedIndex != -1) {
                    val dm = cursor.getLong(dateModifiedIndex)
                    if (dm > 0) dateModified = dm * 1000 // DATE_MODIFIED is in seconds
                }
            }
        }
        val date = dateTaken ?: dateModified ?: return null
        return java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
            .format(java.util.Date(date))
    }

    fun getImageDate(context: Context, uri: Uri): String {
        val contentResolver = context.contentResolver
        var dateTaken: Long? = null
        var dateModified: Long? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val dateTakenIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
                val dateModifiedIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)
                if (dateTakenIndex != -1) {
                    val dt = cursor.getLong(dateTakenIndex)
                    if (dt > 0) dateTaken = dt
                }
                if (dateModifiedIndex != -1) {
                    val dm = cursor.getLong(dateModifiedIndex)
                    if (dm > 0) dateModified = dm * 1000 // DATE_MODIFIED is in seconds
                }
            }
        }
        val timestamp = dateTaken ?: dateModified ?: System.currentTimeMillis()
        val imageDate = java.util.Date(timestamp)
        val now = java.util.Calendar.getInstance().time

        val dateFormat = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())
        val imageDay = dateFormat.format(imageDate)
        val currentDay = dateFormat.format(now)

        return if (imageDay == currentDay) {
            "Today"
        } else {
            java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                .format(imageDate)
        }
    }

    fun getFilePathFromUri(context: Context, uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = context.contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                return it.getString(columnIndex)
            }
        }
        return null
    }

    fun shareImage(context: Context, imageUri: Uri) {
        val shareIntent =
            Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
    }

    private fun showRenameDialog(media: MediaFile, position: Int) {
        val view = layoutInflater.inflate(R.layout.alert_dialog, null)
        val input = view.findViewById<EditText>(R.id.input)

        val displayName = getDisplayName(media.uri.toUri())
        val dotIndex = displayName.lastIndexOf('.')

        val oldName =
            if (dotIndex > 0) displayName.substring(0, dotIndex) else displayName

        val extension =
            if (dotIndex > 0) displayName.substring(dotIndex + 1) else ""

        input.setText(oldName)
        input.hint = "Enter name"

        val dialog = AlertDialog.Builder(this)
            .setTitle("Rename")
            .setView(view)
            .setPositiveButton("Rename", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            // SELECT TEXT AFTER WINDOW ATTACH
            input.post {
                input.requestFocus()
                input.setSelection(0, input.text.length)

                val imm =
                    getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
            }

            val positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positive.isEnabled = oldName.isNotBlank()

            input.addTextChangedListener {
                positive.isEnabled = it.toString().trim().isNotEmpty()
            }

            positive.setOnClickListener {
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    val finalName =
                        if (extension.isNotEmpty()) "$newName.$extension" else newName

                    renameMedia(media.uri.toUri(), finalName, position)
                    dialog.dismiss()
                }
            }
        }

        dialog.show()

        dialog.window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
        )
    }


    private fun getDisplayName(uri: Uri): String {
        contentResolver.query(
            uri,
            arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(0)
            }
        }
        return "File"
    }

    private fun renameMedia(uri: Uri, newFileName: String, position: Int) {
        try {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, newFileName)
            }

            val rows = contentResolver.update(uri, values, null, null)

            if (rows > 0) {
                Toast.makeText(this, "Renamed successfully", Toast.LENGTH_SHORT).show()

                // Refresh media list
                imageList[position].name = newFileName
                adapter.notifyItemChanged(position)
                filmstripAdapter.notifyItemChanged(position)
            } else {
                Toast.makeText(this, "Rename failed", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            Toast.makeText(
                this,
                "Allow file access permission to rename",
                Toast.LENGTH_LONG
            ).show()
            startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Rename error", Toast.LENGTH_SHORT).show()
        }
    }


    private fun toggleUIVisibility() {
        val sharedPreferences =
            getSharedPreferences("apw_gallery_preferences", Context.MODE_PRIVATE)
        val isFilmstripEnabled = sharedPreferences.getBoolean("ENABLE_FILMSTRIP", true)

        val controller = WindowInsetsControllerCompat(window, binding.root)

        if (isUIVisible) {
            // Hide app UI elements
            binding.fabBack.visibility = View.GONE
            binding.bottomBar.visibility = View.GONE
            binding.filmStripRecyclerView.visibility = View.GONE
            binding.playButton.visibility = View.GONE

            // Hide system bars
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            isUIVisible = false
        } else {
            // Show app UI elements
            binding.fabBack.visibility = View.VISIBLE
            binding.bottomBar.visibility = View.VISIBLE
            if (isFilmstripEnabled) {
                binding.filmStripRecyclerView.visibility = View.VISIBLE
            }

            isUIVisible = true

            // Show play button if current item is a video
            val currentPosition = binding.viewPager.currentItem
            updatePlayButtonVisibility(currentPosition)

            // Show system bars
            controller.show(WindowInsetsCompat.Type.systemBars())

            // Ensure icons stay light on dark background
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        _binding = null
        key?.let { MediaHub.remove(it) }
    }

    private fun finishWithAnimation() {
        val currentItem = binding.viewPager.currentItem
        binding.viewPager.setCurrentItem(currentItem - 1, true)  // Animate to previous item
        binding.viewPager.postDelayed({
            finish()
        }, 200) // Delay finish slightly to show animation
    }
}