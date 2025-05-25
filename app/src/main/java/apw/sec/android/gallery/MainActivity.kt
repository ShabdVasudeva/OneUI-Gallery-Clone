package apw.sec.android.gallery

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.*
import android.widget.*
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.*
import android.view.*
import androidx.lifecycle.*
import androidx.preference.*
import android.util.Log
import android.graphics.*
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import android.content.Intent
import com.google.android.material.dialog.*
import apw.sec.android.gallery.databinding.ActivityMainBinding
import apw.sec.android.gallery.viewmodel.MediaViewModel

class MainActivity : AppCompatActivity(), ColorPickerDialogListener{

    private var _binding: ActivityMainBinding? = null
    private val PERMISSION_REQUEST_PERM: Int = 100
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setTitle("Gallery")
        if (!hasPermissions()) {
            requestPermissions()
        } else {
            loadImages()
        }
    }

    private fun setCurrentFragment(fragment: Fragment) =
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.placeholder, fragment)
            commit()
        }
        
    private fun hasPermissions(): Boolean {
        return requiredPermissions().all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, requiredPermissions(), PERMISSION_REQUEST_PERM)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_PERM) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                loadImages()
            } else {
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun loadImages() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.images -> {
                    binding.toolbar.setTitle("Gallery")
                    setCurrentFragment(MainFrag())
                    true
                }
                R.id.albums -> {
                    binding.toolbar.setTitle("Albums")
                    setCurrentFragment(Album())
                    true
                }
                R.id.search ->{
                    binding.toolbar.setTitle("Search")
                    setCurrentFragment(Search())
                    true
                }
                R.id.utilities ->{
                    binding.toolbar.setTitle("Utils")
                    setCurrentFragment(Utils())
                    true
                }
                else -> false
            }
        }
        binding.bottomNavigation.setSelectedItemId(R.id.images)
    }

    companion object {
        class MainFrag : Fragment() {

            private var mediaList: List<MediaFile>? = null
            private lateinit var adapter: MediaAdapter
            private lateinit var model: MediaViewModel
            override fun onCreateView(
                inflater: LayoutInflater, container: ViewGroup?,
                savedInstanceState: Bundle?
            ): View? {
                return inflater.inflate(R.layout.fragment_main, container, false)
            }

            override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
                super.onViewCreated(view, savedInstanceState)
                model = ViewModelProvider(requireActivity())[MediaViewModel::class.java]
                val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
                recyclerView.layoutManager = GridLayoutManager(context, 4)
                loadImages()
                recyclerView.adapter = adapter
            }

            private fun loadImages() {
                val mediaFetcher = MediaFetcher(requireContext())
                mediaList = mediaFetcher.fetchMediaFiles()
                model.saveMediaData(mediaList!!)
                adapter = MediaAdapter(mediaList!!)
            }
        }
        
        class Search: Fragment(){
            
            private var mediaList: List<MediaFile>? = null
            private lateinit var adapter: SearchAdapter

            override fun onCreateView(
                inflater: LayoutInflater, container: ViewGroup?,
                savedInstanceState: Bundle?
            ): View? {
                return inflater.inflate(R.layout.fragment_search, container, false)
            }

            override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
                super.onViewCreated(view, savedInstanceState)
                val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
                val search = view.findViewById<ApwSearch>(R.id.search)
                search.setSearchViewListener(object: ApwSearch.SearchViewListener{
                    override fun onQueryTextChange(query: String) {
                        adapter.filter(query)
                    }
                    override fun onQueryTextSubmit(query: String) {}
                })
                recyclerView.layoutManager = GridLayoutManager(context, 4)
                loadImages()
                recyclerView.adapter = adapter
            }

            private fun loadImages() {
                val mediaFetcher = FetchAll(requireContext())
                mediaList = mediaFetcher.fetchMediaFiles()
                adapter = SearchAdapter(mediaList!!)
            }
        }
        
        class Album: Fragment(){
            
            private var mediaList: List<MediaFile>? = null
            private lateinit var adapter: AlbumAdapter

            override fun onCreateView(
                inflater: LayoutInflater, container: ViewGroup?,
                savedInstanceState: Bundle?
            ): View? {
                return inflater.inflate(R.layout.fragment_album, container, false)
            }

            override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
                super.onViewCreated(view, savedInstanceState)
                val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
                recyclerView.layoutManager = GridLayoutManager(context, 3)
                loadImages()
                recyclerView.adapter = adapter
            }

            private fun loadImages() {
                val mediaFetcher = Albums(requireContext())
                mediaList = mediaFetcher.fetchAlbums()
                adapter = AlbumAdapter(requireContext(), mediaList!!)
            }
        }
        
        class Utils : PreferenceFragmentCompat(){
            
            private val DIALOG_ID = 0
            private val TAG = "Color"
            
            override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
                setPreferencesFromResource(R.xml.preferences, rootKey)
                val ai: Preference? = findPreference("safe")
                val quote: Preference? = findPreference("quotes")
                ai?.setOnPreferenceClickListener{pref ->
                    startActivity(Intent(requireContext(), PrivateSafe::class.java))
                    true
                }
                quote?.setOnPreferenceClickListener{pref ->
                    ColorPickerDialog.newBuilder()
                        .setDialogType(ColorPickerDialog.TYPE_CUSTOM)
                        .setAllowPresets(false)
                        .setDialogId(DIALOG_ID)
                        .setColor(Color.BLACK)
                        .setShowAlphaSlider(true)
                        .show(requireActivity())
                    true
                }
            }
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }
    
    override fun onColorSelected(dialogId: Int, color: Int) {
        Log.d("Color", "onColorSelected() -> dialogId: $dialogId, color: $color")
        if (dialogId == 0) {
            val colorHex = String.format("#%08X", color)
            GenerateQuoteImage().createQuoteImage(this, colorHex, getSelectedFont(this))
        }
    }

    fun getSelectedFont(context: Context): String {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val fontName = sharedPreferences.getString("font_preference", "Roboto Slab")

        return when (fontName) {
            "RobotoSlab.ttf" -> "RobotoSlab.ttf"
            "PermanentMarker.ttf" -> "PermanentMarker.ttf"
            "PlayfairDisplay.ttf" -> "PlayfairDisplay.ttf"
            "Handwriting.ttf" -> "Handwriting.ttf"
            else -> "RobotoSlab.ttf"
        }
    }

    override fun onDialogDismissed(dialogId: Int) {
        Log.d("Color", "onDialogDismissed() -> dialogId: $dialogId")
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean{
        when(item.getItemId()){
            R.id.all ->{
                startActivity(Intent(this, AllActivity::class.java))
                return true
            }
            R.id.settings ->{
                startActivity(Intent(this, SettingsActivity::class.java))
                return true
            }
        }
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}