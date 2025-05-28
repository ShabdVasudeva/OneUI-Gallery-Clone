package apw.sec.android.gallery

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.*
import android.view.*
import androidx.viewpager2.widget.ViewPager2
import android.graphics.*
import android.text.*
import android.net.*
import java.io.*
import android.provider.MediaStore
import android.content.*
import android.content.res.ColorStateList
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import apw.sec.android.gallery.databinding.*
import apw.sec.android.gallery.data.MediaHub
import androidx.core.graphics.toColorInt

class ViewActivity: AppCompatActivity() {
    private var _binding: ActivityViewBinding? = null
    private val binding get() = _binding!!
    private lateinit var imageList: MutableList<MediaFile>
    private lateinit var adapter: ImagePagerAdapter
    private lateinit var filmstripAdapter: FilmstripAdapter
    private var startPosition: Int = 0
    private var key: String? = ""
    private var isUIVisible = true
    
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        _binding = ActivityViewBinding.inflate(getLayoutInflater())
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener{
            onBackPressed()
        }
        binding.toolbar.setTitleTextColor(Color.WHITE)
        binding.toolbar.setNavigationIconTint(Color.WHITE)
        key = intent.getStringExtra("media_key")
        startPosition = intent.getIntExtra("position", 0)
        imageList = MediaHub.get(key!!) as MutableList<MediaFile>
        window.navigationBarColor = "#000000".toColorInt()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        window.statusBarColor = "#000000".toColorInt()
        val currentPosition = binding.viewPager.currentItem
        supportActionBar!!.title = getImageDate(applicationContext, imageList[currentPosition].uri.toUri()) ?: imageList[startPosition].name
        supportActionBar!!.subtitle = getImageTime(applicationContext, imageList[currentPosition].uri.toUri()) ?: ""
        binding.toolbar.setSubtitleTextColor(Color.GRAY)
        binding.viewPager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback(){
            override fun onPageSelected(pos: Int){
                super.onPageSelected(pos)
                supportActionBar!!.title = getImageDate(applicationContext, imageList[pos].uri.toUri()) ?: imageList[pos].name
                supportActionBar!!.subtitle = getImageTime(applicationContext, imageList[pos].uri.toUri()) ?: ""
            }
        })
        adapter = ImagePagerAdapter(this@ViewActivity, imageList){toggleUIVisibility()}
        binding.viewPager.adapter = adapter
        binding.viewPager.setCurrentItem(startPosition, false)
        filmstripAdapter = FilmstripAdapter(imageList){ position ->
            binding.viewPager.setCurrentItem(position, true)
        }
        binding.filmStripRecyclerView.layoutManager = LinearLayoutManager(this@ViewActivity, LinearLayoutManager.HORIZONTAL, false)
        binding.filmStripRecyclerView.adapter = filmstripAdapter
        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(binding.filmStripRecyclerView)
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback(){
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                supportActionBar?.title = imageList[position].name
                filmstripAdapter.setSelectedPosition(position)
                binding.filmStripRecyclerView.post{
                    binding.filmStripRecyclerView.smoothScrollToPosition(position)
                }
            }
        })
        binding.bottomBar.itemTextColor = ColorStateList.valueOf(Color.WHITE)
        binding.bottomBar.itemIconTintList = ColorStateList.valueOf(Color.WHITE)
        binding.bottomBar.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.share -> {
                    val currentPosition = binding.viewPager.currentItem
                    val currentUri = imageList[currentPosition].uri.toUri()
                    shareImage(this, currentUri)
                    true
                }
                R.id.edit -> {
                    val pos = binding.viewPager.currentItem
                    editImage(this, imageList[pos].uri.toUri())
                    true
                }
                R.id.delete ->{
                    val pos = binding.viewPager.currentItem
                    deleteImageFromUri(this, imageList[pos].uri.toUri(), pos)
                    true
                }
                R.id.info ->{
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
                val nextIndex = if (pos < imageList.size) pos else imageList.lastIndex
                binding.viewPager.setCurrentItem(nextIndex, false)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Please Allow All files access permission first", Toast.LENGTH_SHORT).show()
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            context.startActivity(intent)
            false
        }
    }
    
    fun editImage(context: Context, imageUri: Uri) {
        try {
            var intent =  Intent(Intent.ACTION_EDIT)
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
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeStream(contentResolver.openInputStream(imageUri), null, options)
        val resolution = "${options.outWidth} x ${options.outHeight}"
        val fileSize = filePath?.let { File(it).length() } ?: 0L
        val fileSizeMB = String.format("%.2f MB", fileSize / (1024.0 * 1024.0))
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Image Info")
        val message = "📂 <b>Folder:</b> $folderName<br>📄 <b>File Path:</b> $filePath<br>🖼 <b>Resolution:</b> $resolution<br>📏 <b>File Size:</b> $fileSizeMB"
        builder.setMessage(Html.fromHtml(message, Html.FROM_HTML_MODE_LEGACY))
        builder.setPositiveButton("OK", null)
        builder.show()
    }

    fun getImageTime(context: Context, uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATE_TAKEN, MediaStore.Images.Media.DATE_MODIFIED)

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val dateTakenIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
                val dateModifiedIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)

                val dateTaken = if (dateTakenIndex != -1) cursor.getLong(dateTakenIndex) else 0L
                val dateModified = if (dateModifiedIndex != -1) cursor.getLong(dateModifiedIndex) else 0L

                val date = if (dateTaken != 0L) dateTaken else dateModified
                return java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                    .format(java.util.Date(date))
            }
        }
        return null
    }

    fun getImageDate(context: Context, uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATE_TAKEN, MediaStore.Images.Media.DATE_MODIFIED)
        val resolver = context.contentResolver

        resolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val dateTakenIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val dateModifiedIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)

                val dateTaken = cursor.getLong(dateTakenIndex)
                val dateModified = cursor.getLong(dateModifiedIndex)

                val timestamp = if (dateTaken > 0) dateTaken else dateModified
                val imageDate = java.util.Date(timestamp)

                val now = java.util.Calendar.getInstance().time

                val dateFormat = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())
                val imageDay = dateFormat.format(imageDate)
                val currentDay = dateFormat.format(now)

                return if (imageDay == currentDay) {
                    "Today"
                } else {
                    java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault()).format(imageDate)
                }
            }
        }
        return null
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
        if (isUIVisible) {
            binding.toolbar.visibility = View.GONE
            binding.bottomBar.visibility = View.GONE
        } else {
            binding.toolbar.visibility = View.VISIBLE
            binding.bottomBar.visibility = View.VISIBLE
        }
        isUIVisible = !isUIVisible
    }
    
    fun shareImage(context: Context, imageUri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
    }
    
    override fun onDestroy(){
        super.onDestroy()
        _binding = null
        key?.let { 
            MediaHub.remove(it)
        }
    }
}