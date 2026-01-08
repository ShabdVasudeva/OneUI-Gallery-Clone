package apw.sec.android.gallery

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import apw.sec.android.gallery.AlbumItem
import apw.sec.android.gallery.AlbumViewer
import apw.sec.android.gallery.R
import apw.sec.android.gallery.components.AlbumImage
import com.bumptech.glide.Glide

class GroupAlbumAdapter(
    private val context: Context,
    private val albums: List<AlbumItem.Album>
) : RecyclerView.Adapter<GroupAlbumAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val image: AlbumImage = view.findViewById(R.id.imageView)
        val name: TextView = view.findViewById(R.id.text)
        val count: TextView = view.findViewById(R.id.text2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.album_item, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val album = albums[position]

        holder.name.text = album.folderName
        holder.count.text = "${album.media.size}"

        Glide.with(context)
            .load(album.media.firstOrNull()?.uri)
            .centerCrop()
            .into(holder.image)

        holder.itemView.setOnClickListener {
            val intent = Intent(context, AlbumViewer::class.java)
            intent.putExtra("folderName", album.folderName)
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = albums.size
}
