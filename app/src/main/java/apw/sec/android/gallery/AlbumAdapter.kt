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
        mediaFiles.groupBy { it.folderName ?: "Unknown" }
            .mapValues { it.value.toMutableList() }
    private val albumList = albumMap.keys.toList()
    private var pos: Int? = null

    inner class MediaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: AlbumImage = view.findViewById(R.id.imageView)
        val folderNameTextView: TextView = view.findViewById(R.id.text)
        val folderCountTextView: TextView = view.findViewById(R.id.text2)

        init {
            view.setOnClickListener {
                val folderName = albumList[bindingAdapterPosition]
                pos = bindingAdapterPosition
                val intent = Intent(context, AlbumViewer::class.java).apply {
                    putExtra("folderName", folderName)
                }
                context.startActivity(intent)
            }
            view.setOnLongClickListener {
                val folderName = albumList[bindingAdapterPosition]
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
                    moveAlbum(folderName)
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

    private fun moveAlbum(folderName: String) {
        val groupAlbums = albumMap.keys.filter { it != folderName }
        if (groupAlbums.isEmpty()) {
            Toast.makeText(context, "No group albums available", Toast.LENGTH_SHORT).show()
            return
        }

        val anchorView = (context as? AppCompatActivity)?.window?.decorView?.findViewById<View>(android.R.id.content)
            ?: return

        val popupMenu = PopupMenu(context, anchorView)
        groupAlbums.forEachIndexed { index, groupName ->
            popupMenu.menu.add(0, index, 0, groupName)
        }
        popupMenu.setOnMenuItemClickListener { menuItem ->
            val targetGroup = groupAlbums[menuItem.itemId]
            val filesToMove = albumMap[folderName]?.toList() ?: return@setOnMenuItemClickListener false
            albumMap[targetGroup]?.addAll(filesToMove)
            albumMap[folderName]?.clear()
            mediaFiles.removeAll(elements = filesToMove)
            mediaFiles.addAll( filesToMove.map { transformedFile ->
                transformedFile.copy(folderName = targetGroup)
            })
            notifyDataSetChanged()
            Toast.makeText(context, "Album \"$folderName\" moved to \"$targetGroup\"", Toast.LENGTH_SHORT).show()
            true
        }
        popupMenu.show()
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