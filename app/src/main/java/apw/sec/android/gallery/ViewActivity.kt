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
import androidx.recyclerview.widget.*
import androidx.viewpager2.widget.ViewPager2
import apw.sec.android.gallery.data.MediaHub
import apw.sec.android.gallery.databinding.*
import java.io.*
import apw.sec.android.gallery.R

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
        _binding = ActivityViewBinding.inflate(getLayoutInflater())
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finishWithAnimation() }
        binding.toolbar.setTitleTextColor(Color.WHITE)
        binding.toolbar.setNavigationIconTint(Color.WHITE)

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
        window.navigationBarColor = "#000000".toColorInt()
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        window.statusBarColor = "#000000".toColorInt()
        val currentPosition = binding.viewPager.currentItem
        supportActionBar!!.title = getImageDate(applicationContext, imageList[currentPosition].uri.toUri())
        supportActionBar!!.subtitle = getImageTime(applicationContext, imageList[currentPosition].uri.toUri()) ?: ""
        binding.toolbar.setSubtitleTextColor(Color.GRAY)
        binding.viewPager.registerOnPageChangeCallback(
                object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(pos: Int) {
                        super.onPageSelected(pos)
                        supportActionBar!!.title =
                                getImageDate(applicationContext, imageList[pos].uri.toUri())
                        supportActionBar!!.subtitle =
                                getImageTime(applicationContext, imageList[pos].uri.toUri()) ?: ""
                    }
                }
        )
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
        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(binding.filmStripRecyclerView)
        binding.viewPager.registerOnPageChangeCallback(
                object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        super.onPageSelected(position)
                        supportActionBar?.title = getImageDate(
                            this@ViewActivity,
                            imageList[position].uri.toUri()
                        )
                        filmstripAdapter.setSelectedPosition(position)
                        binding.filmStripRecyclerView.post {
                            binding.filmStripRecyclerView.smoothScrollToPosition(position)
                        }
                    }
                }
        )
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
                R.id.delete -> {
                    var builder = AlertDialog.Builder(this@ViewActivity)
                    builder.setTitle("Delete this photo?")
                    builder.setMessage(
                            "This photo will be deleted from this device.\nAlso can not be acessed or restored from gallery."
                    )
                    builder.setNegativeButton("Cancel", null)
                    builder.setPositiveButton(
                            "Delete",
                            object : DialogInterface.OnClickListener {
                                override fun onClick(p0: DialogInterface?, p1: Int) {
                                    val pos = binding.viewPager.currentItem
                                    deleteImageFromUri(
                                            this@ViewActivity,
                                            imageList[pos].uri.toUri(),
                                            pos
                                    )
                                }
                            }
                    )
                    builder.show()
                    true
                }
                R.id.info -> {
                    val pos = binding.viewPager.currentItem
                    showImageInfoDialog(this, imageList[pos].uri.toUri())
                    true
                }
                else -> false
            }
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

    private fun toggleUIVisibility() {
        val sharedPreferences = getSharedPreferences("apw_gallery_prefs", Context.MODE_PRIVATE)
        if (isUIVisible) {
            binding.toolbar.visibility = View.GONE
            binding.bottomBar.visibility = View.GONE
            binding.filmStripRecyclerView.visibility = View.GONE
        } else {
            binding.toolbar.visibility = View.VISIBLE
            binding.bottomBar.visibility = View.VISIBLE
            binding.filmStripRecyclerView.visibility = View.VISIBLE
        }
        isUIVisible = !isUIVisible
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
            overridePendingTransition(R.anim.slide_out_up, R.anim.slide_in_down)
        }, 200) // Delay finish slightly to show animation
    }
}