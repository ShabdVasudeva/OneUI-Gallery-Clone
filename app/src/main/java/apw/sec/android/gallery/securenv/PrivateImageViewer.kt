package apw.sec.android.gallery.securenv

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.*
import android.graphics.*
import androidx.recyclerview.widget.*
import com.google.android.material.elevation.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import apw.sec.android.gallery.*
import android.provider.MediaStore
import android.net.Uri
import apw.sec.android.gallery.securenv.*
import apw.sec.android.gallery.databinding.LayoutPrivateImageViewerBinding

class PrivateImageViewer: AppCompatActivity(){
    
    private var _binding: LayoutPrivateImageViewerBinding? = null
    private val binding get() = _binding!!
    
    companion object {
        private const val WINDOW_FLAGS = WindowManager.LayoutParams.FLAG_SECURE
        private lateinit var imageList: List<ImageItem>
        private var startPosition: Int = 0
        private var isUIVisible = true
        private lateinit var database: PrivateSafeDatabase
        private var key: String? = ""
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);
        _binding = LayoutPrivateImageViewerBinding.inflate(getLayoutInflater())
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener{
            onBackPressed()
        }
        binding.toolbar.setNavigationIconTint(Color.WHITE)
        binding.toolbar.setTitleTextColor(Color.WHITE)
        binding.bottomBar2.itemTextColor = ColorStateList.valueOf(Color.WHITE)
        binding.bottomBar2.itemIconTintList = ColorStateList.valueOf(Color.WHITE)
        window.setFlags(
            WINDOW_FLAGS,
            WINDOW_FLAGS
        )
        database = PrivateSafeDatabase(this)
        window.navigationBarColor = Color.parseColor("#000000")
        window.statusBarColor = Color.parseColor("#000000")
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        key = intent.getStringExtra("media_key")
        imageList = PrivateMediaHub.get(key!!)!!
        startPosition = intent.getIntExtra("position", 0)
        val adapter = PrivatePagerAdapter(this, imageList){toggleUIVisibility()}
        binding.viewPager.adapter = adapter
        binding.viewPager.setCurrentItem(startPosition, false)
        binding.bottomBar2.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.share -> {
                    val pos = binding.viewPager.currentItem
                    ImageUtils.shareImage(this, imageList[pos].imagePath)
                    true
                }
                R.id.delete ->{
                    val pos = binding.viewPager.currentItem
                    database.deleteImagePath(imageList[pos].imagePath)
                    ImageUtils.deletePath(imageList[pos].imagePath)
                    Toast.makeText(this, "Deleted successfuly changes may occur on next open", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.save ->{
                    val pos = binding.viewPager.currentItem
                    ImageUtils.saveToLocal(this, imageList[pos].imagePath)
                    true
                }
                else -> false
            }
        }
    }
    
    private fun toggleUIVisibility() {
        if (isUIVisible) {
            binding.toolbar.visibility = View.GONE
            binding.bottomBar2.visibility = View.GONE
        } else {
            binding.toolbar.visibility = View.VISIBLE
            binding.bottomBar2.visibility = View.VISIBLE
        }
        isUIVisible = !isUIVisible
    }
    
    override fun onDestroy(){
        super.onDestroy()
        _binding = null
        PrivateMediaHub.remove(key!!)
        key = null
    }
}
