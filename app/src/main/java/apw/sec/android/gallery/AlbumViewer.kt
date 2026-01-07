package apw.sec.android.gallery

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import apw.sec.android.gallery.databinding.*
import androidx.recyclerview.widget.*
import android.util.Log
import androidx.lifecycle.*
import apw.sec.android.gallery.data.MediaHub

class AlbumViewer: AppCompatActivity(){

    private var _binding: LayoutAlbumViewerBinding? = null
    private val binding get() = _binding!!
    private lateinit var mediaFiles: MutableList<MediaFile>
    private lateinit var adapter: MediaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = LayoutAlbumViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationButtonAsBack()

        val mediaKey = intent.getStringExtra("media_key")
        val folderName = intent.getStringExtra("folderName") ?: "Album"

        binding.toolbar.setTitle(folderName)

        mediaFiles = if (mediaKey != null) {
            MediaHub.get(mediaKey)?.toMutableList() ?: mutableListOf()
        } else {
            fetchMediaFilesFromFolder(folderName).toMutableList()
        }
        mediaFiles.sortByDescending { it.dateAdded ?: 0L }
        setupRecyclerView(mediaFiles, folderName)

        // Calculate and display counts
        val videoCount = mediaFiles.count { it.isVideo() }
        val imageCount = mediaFiles.size - videoCount

        val parts = mutableListOf<String>()
        if (imageCount > 0) parts.add("$imageCount image${if (imageCount > 1) "s" else ""}")
        if (videoCount > 0) parts.add("$videoCount video${if (videoCount > 1) "s" else ""}")
        val mediaCountText = parts.joinToString(" ")

        binding.toolbar.toolbar.setSubtitle(mediaCountText)
        binding.toolbar.setExpandedSubtitle(mediaCountText)
    }

    fun setupRecyclerView(mediaFiles: MutableList<MediaFile>, folderName: String?){
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
