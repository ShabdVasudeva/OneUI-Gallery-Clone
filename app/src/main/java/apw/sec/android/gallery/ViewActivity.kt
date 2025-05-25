package apw.sec.android.gallery

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.*
import android.view.*
import androidx.viewpager2.widget.ViewPager2
import android.util.Log
import android.graphics.*
import android.text.*
import android.provider.Settings
import android.Manifest
import android.net.*
import java.io.*
import android.app.*
import android.provider.MediaStore
import android.content.*
import androidx.lifecycle.*
import android.content.res.ColorStateList
import apw.sec.android.gallery.databinding.*
import apw.sec.android.gallery.data.MediaHub
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlin.collections.emptyList

class ViewActivity: AppCompatActivity() {
    private var _binding: ActivityViewBinding? = null
    private val binding get() = _binding!!
    private lateinit var imageList: List<MediaFile>
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
        /* @Suppress("UNCHECKED_CAST")
        imageList = intent.getSerializableExtra("imageList") as ArrayList<MediaFile>  */
        key = intent.getStringExtra("media_key")
        startPosition = intent.getIntExtra("position", 0)
        imageList = MediaHub.get(key!!) ?: emptyList()
        getSupportActionBar()!!.title = imageList[startPosition].name
        window.navigationBarColor = Color.parseColor("#000000")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        window.statusBarColor = Color.parseColor("#000000")
        val currentPosition = binding.viewPager.currentItem
        binding.viewPager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback(){
            override fun onPageSelected(pos: Int){
                super.onPageSelected(pos)
                getSupportActionBar()!!.title = imageList[pos].name
            }
        })
        val adapter = ImagePagerAdapter(this@ViewActivity, imageList){toggleUIVisibility()}
        binding.viewPager.adapter = adapter
        binding.viewPager.setCurrentItem(startPosition, false)
        binding.bottomBar.itemTextColor = ColorStateList.valueOf(Color.WHITE)
        binding.bottomBar.itemIconTintList = ColorStateList.valueOf(Color.WHITE)
        binding.bottomBar.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.share -> {
                    val currentPosition = binding.viewPager.currentItem
                    val currentUri = Uri.parse(imageList[currentPosition].uri)
                    shareImage(this, currentUri)
                    true
                }
                R.id.edit -> {
                    val pos = binding.viewPager.currentItem
                    editImage(this, Uri.parse(imageList[pos].uri))
                    true
                }
                R.id.delete ->{
                    val pos = binding.viewPager.currentItem
                    deleteImageFromUri(this, Uri.parse(imageList[pos].uri))
                    true
                }
                R.id.info ->{
                    val pos = binding.viewPager.currentItem
                    showImageInfoDialog(this, Uri.parse(imageList[pos].uri))
                    true
                }
                else -> false
            }
        }
    }

    fun deleteImageFromUri(context: Context, imageUri: Uri): Boolean {
        return try {
            val contentResolver: ContentResolver = context.contentResolver
            val rowsDeleted = contentResolver.delete(imageUri, null, null)
            rowsDeleted > 0
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Please Allow All files access permission first", Toast.LENGTH_SHORT).show()
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