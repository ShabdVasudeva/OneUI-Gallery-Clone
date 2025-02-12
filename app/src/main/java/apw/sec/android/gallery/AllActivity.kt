package apw.sec.android.gallery

import android.Manifest
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
import apw.sec.android.gallery.databinding.ActivityAllBinding
import android.content.Intent

class AllActivity: AppCompatActivity(){

    private var _binding: ActivityAllBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityAllBinding.inflate(getLayoutInflater())
        setContentView(binding.root)
        setCurrentFragment(AllFrag())
        binding.toolbar.setNavigationButtonAsBack()
    }
    
    private fun setCurrentFragment(fragment: Fragment) =
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.placeholder, fragment)
            commit()
        }
    
    companion object{
        class AllFrag : Fragment() {

            private var mediaList: List<MediaFile>? = null
            private lateinit var adapter: SearchAdapter

            override fun onCreateView(
                inflater: LayoutInflater, container: ViewGroup?,
                savedInstanceState: Bundle?
            ): View? {
                return inflater.inflate(R.layout.fragment_album, container, false)
            }

            override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
                super.onViewCreated(view, savedInstanceState)
                val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
                recyclerView.layoutManager = GridLayoutManager(context, 4)
                loadImages()
                recyclerView.adapter = adapter
            }

            private fun loadImages() {
                val mediaFetcher = AllMediaType(requireContext())
                mediaList = mediaFetcher.fetchMediaFiles()
                adapter = SearchAdapter(mediaList!!)
            }
        }
    }
    
    override fun onDestroy(){
        super.onDestroy()
        _binding = null
    }
}
