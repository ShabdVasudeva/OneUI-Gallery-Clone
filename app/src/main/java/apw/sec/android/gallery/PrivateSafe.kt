package apw.sec.android.gallery

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.*
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.recyclerview.widget.*
import com.google.android.material.elevation.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import androidx.preference.*
import android.provider.MediaStore
import apw.sec.android.gallery.securenv.*
import apw.sec.android.gallery.databinding.ActivitySafeBinding

class PrivateSafe: AppCompatActivity() {
    
    private var _binding: ActivitySafeBinding? = null
    private val binding get() = _binding!!
    private lateinit var database: PrivateSafeDatabase
    companion object {
        private const val REQUEST_CODE_AUTHENTICATION = 1001
        private const val REQUEST_IMAGE = 1002
        private const val WINDOW_FLAG = WindowManager.LayoutParams.FLAG_SECURE
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivitySafeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        authenticateWithKeyguard()
        binding.toolbar.setNavigationButtonAsBack()
        window.setFlags(
            WINDOW_FLAG,
            WINDOW_FLAG
        )
        database = PrivateSafeDatabase(this@PrivateSafe)
        val images = database.getAllImagePaths()
        val adapter = ImageAdapter(images){ _ ->
            
        }
        binding.recyclerView.layoutManager = GridLayoutManager(this@PrivateSafe, 4)
        binding.recyclerView.adapter = adapter
        binding.add.setOnClickListener{
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            @Suppress("DEPRECATION")
            startActivityForResult(intent, REQUEST_IMAGE)
        }
    }

    @Suppress("DEPRECATION")
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun authenticateWithKeyguard() {
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (keyguardManager.isKeyguardSecure) {
            val intent = keyguardManager.createConfirmDeviceCredentialIntent(
                "Authentication Required",
                "Please authenticate to access private safe"
            )
            startActivityForResult(intent, REQUEST_CODE_AUTHENTICATION)
        } else {
            Toast.makeText(this, "No lock screen security set up", Toast.LENGTH_LONG).show()
            finish()
        }
    }
  
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE) {
                val imageUri = data?.data
                if (imageUri != null) {
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                    val path: String? = ImageUtils.saveBitmapToFile(this@PrivateSafe, bitmap)
                    if (path != null) {
                        database.insertImagePath(path)
                        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
                        val autoDeleteEnabled = sharedPreferences.getBoolean("DELETE_AFTER_SAVE", false)
                        if (autoDeleteEnabled) {
                            val imagePath = getRealPathFromUri(imageUri)
                            if (imagePath != null) {
                                ImageUtils.deletePath(imagePath)
                            }
                        }
                        loadImages()
                    }
                }
            }
            if (requestCode == REQUEST_CODE_AUTHENTICATION) {
                if (resultCode == RESULT_OK) {
                    Toast.makeText(this, "Authentication Successful", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Authentication Failed", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
    
    private fun getRealPathFromUri(uri: Uri): String? {
        val cursor = contentResolver.query(uri, arrayOf(MediaStore.Images.Media.DATA), null, null, null)
        return cursor?.use {
            if (it.moveToFirst()) {
                it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
            } else null
        }
    }
    
    private fun loadImages(){
        val images = database.getAllImagePaths()
        val adapter = ImageAdapter(images){ _ ->
            
        }
        binding.recyclerView.layoutManager = GridLayoutManager(this@PrivateSafe, 4)
        binding.recyclerView.adapter = adapter
    }
    
    override fun onDestroy(){
        super.onDestroy()
        _binding = null
    }
}