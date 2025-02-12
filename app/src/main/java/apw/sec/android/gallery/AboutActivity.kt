package apw.sec.android.gallery

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import apw.sec.android.gallery.databinding.ActivityAboutBinding
import dev.oneuiproject.oneui.widget.Toast

class AboutActivity: AppCompatActivity(){
    
    private var _binding: ActivityAboutBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.aboutSourceCode.setOnClickListener{
            val intent: Intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ShabdVasudeva/OneUI-Gallery-Clone"))
            startActivity(intent)
            binding.appInfoLayout.setStatus(2)
        }
    }
    
    override fun onDestroy(){
        super.onDestroy()
    }
}