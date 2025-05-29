package apw.sec.android.gallery

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import apw.sec.android.gallery.databinding.ActivityAboutBinding
import dev.oneuiproject.oneui.layout.AppInfoLayout

class AboutActivity: AppCompatActivity(){
    
    private var _binding: ActivityAboutBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.aboutSourceCode.setOnClickListener{
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    "https://github.com/ShabdVasudeva/OneUI-Gallery-Clone".toUri()
                )
            )
            binding.appInfoLayout.status = AppInfoLayout.NO_UPDATE
        }
    }
    
    override fun onDestroy(){
        super.onDestroy()
    }
}