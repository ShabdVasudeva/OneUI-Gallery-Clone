package apw.sec.android.gallery

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import apw.sec.android.gallery.components.AlbumImage
import androidx.core.net.toUri
import dev.oneuiproject.oneui.widget.Toast

class AlbumAdapter(
    private val context: Context,
    private val mediaFiles: MutableList<MediaFile>
) : RecyclerView.Adapter<AlbumAdapter.MediaViewHolder>() {

    private val albumMap: Map<String, MutableList<MediaFile>> =
        mediaFiles.groupBy { it.folderName ?: "Unknown" } as Map<String, MutableList<MediaFile>>
    private val albumList = albumMap.keys.toList()
    private var pos: Int? = null

    inner class MediaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: AlbumImage = view.findViewById(R.id.imageView)
        val folderNameTextView: TextView = view.findViewById(R.id.text)
        val folderCountTextView: TextView = view.findViewById(R.id.text2)

        init {
            view.setOnClickListener {
                val folderName = albumList[adapterPosition]
                pos = adapterPosition
                val folderMediaList = albumMap[folderName] ?: emptyList()
                val intent = Intent(context, AlbumViewer::class.java).apply {
                    putExtra("folderName", folderName)
                }
                context.startActivity(intent)
            }
            view.setOnLongClickListener {
                val folderName = albumList[adapterPosition]
                showPopUpMenu(view, folderName)
                true
            }
        }
    }

    fun showPopUpMenu(view: View, folderName: String){
        val popupMenu = PopupMenu(context, view)
        popupMenu.menuInflater.inflate(R.menu.album_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when(menuItem.itemId){
                R.id.menu_delete ->{
                    deleteAlbum(folderName)
                    true
                }
                R.id.menu_move ->{
                    /* moveAlbum(folderName) */
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun deleteAlbum(folderName: String) {
        val files = albumMap[folderName] ?: return
        val resolver = context.contentResolver

        var deletedCount = 0

        for (file in files) {
            try {
                val uri = file.uri.toUri()
                val rows = resolver.delete(uri, null, null)
                if (rows > 0) deletedCount++
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        Toast.makeText(context, "$deletedCount file(s) deleted from \"$folderName\"", Toast.LENGTH_SHORT).show()

        val fragmentManager = (context as? AppCompatActivity)?.supportFragmentManager
        val currentfragment = fragmentManager?.findFragmentById(R.id.placeholder)
        currentfragment?.let {
            fragmentManager.beginTransaction().detach(it).attach(it).commit()
        }

        mediaFiles.removeAll { it.folderName == folderName }
        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.album_item, parent, false)
        return MediaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val folderName = albumList[position]
        holder.folderNameTextView.text = folderName
        val firstImageUri = albumMap[folderName]?.firstOrNull()?.uri
        Glide.with(holder.itemView.context)
            .load(firstImageUri)
            .centerCrop()
            .into(holder.imageView)
        val count: Int = albumMap[folderName]?.count()  ?: 0
        holder.folderCountTextView.text = count.toString()
    }

    override fun getItemCount(): Int = albumList.size
}