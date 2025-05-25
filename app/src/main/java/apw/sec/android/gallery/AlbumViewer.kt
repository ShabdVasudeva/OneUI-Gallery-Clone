package apw.sec.android.gallery

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import apw.sec.android.gallery.databinding.*
import androidx.recyclerview.widget.*
import android.util.Log
import androidx.lifecycle.*

class AlbumViewer: AppCompatActivity(){
    
    private var _binding: LayoutAlbumViewerBinding? = null
    private val binding get() = _binding!!
    private var mediaFiles: List<MediaFile> = listOf()
    private lateinit var adapter: MediaAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);
        _binding = LayoutAlbumViewerBinding.inflate(getLayoutInflater())
        setContentView(binding.root)
        val folderName: String? = intent.getStringExtra("folderName")
        binding.toolbar.setNavigationButtonAsBack()
        binding.toolbar.setTitle(folderName)
        mediaFiles = fetchMediaFilesFromFolder(folderName!!)
        setupRecyclerView(mediaFiles, folderName)
    }

    fun setupRecyclerView(mediaFiles: List<MediaFile>, folderName: String?){
        getSupportActionBar()!!.title = folderName
        Log.e("AlbumError",mediaFiles.size.toString())
        adapter = MediaAdapter(mediaFiles)
        binding.recyclerView.layoutManager = GridLayoutManager(this, 4)
        binding.recyclerView.adapter = adapter
    }
    
    private fun fetchMediaFilesFromFolder(folderName: String?): List<MediaFile> {
        val allMediaFiles = Albums(this).fetchAlbums()
        val filteredMediaFiles = allMediaFiles.filter { it.folderName == folderName }
        return filteredMediaFiles
    }
    
    override fun onDestroy(){
        super.onDestroy()
        _binding = null
    }
}
