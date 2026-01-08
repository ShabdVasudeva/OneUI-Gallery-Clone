package apw.sec.android.gallery

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import apw.sec.android.gallery.data.MediaHub
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import java.util.UUID

class VirtualAlbumAdapter(
    private val recentMedia: List<MediaFile>,
    private val videosMedia: List<MediaFile>
) : RecyclerView.Adapter<VirtualAlbumAdapter.AlbumViewHolder>() {

    private val albums = listOf("Recent", "Videos")

    inner class AlbumViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnail: ImageView = view.findViewById(R.id.albumThumbnail)
        val title: TextView = view.findViewById(R.id.albumTitle)
        val subtitle: TextView = view.findViewById(R.id.albumSubtitle)

        init {
            view.setOnClickListener {
                val position = bindingAdapterPosition
                val mediaList = when (position) {
                    0 -> recentMedia
                    1 -> videosMedia
                    else -> emptyList()
                }
                // Using existing MediaHub caching to pass the list efficiently
                val key = UUID.randomUUID().toString()
                MediaHub.save(key, mediaList)

                val context = itemView.context
                val intent = Intent(context, AlbumViewer::class.java).apply {
                    putExtra("media_key", key)
                    putExtra("folderName", albums[position]) // To set toolbar title properly
                }
                context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.virtual_album_item, parent, false)
        return AlbumViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        val mediaList = when (position) {
            0 -> recentMedia
            1 -> videosMedia
            else -> emptyList()
        }

        holder.title.text = albums[position]

        val videoCount = mediaList.count { it.isVideo() }
        val imageCount = mediaList.size - videoCount

        val parts = mutableListOf<String>()
        if (imageCount > 0) parts.add("$imageCount image${if (imageCount > 1) "s" else ""}")
        if (videoCount > 0) parts.add("$videoCount video${if (videoCount > 1) "s" else ""}")
        holder.subtitle.text = parts.joinToString(" ")

        // Show thumbnail of first media or placeholder
        if (mediaList.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(mediaList[0].uri)
                .centerCrop()
                .transform(RoundedCorners(20))
                .into(holder.thumbnail)
        }
    }

    override fun getItemCount(): Int = albums.size
}
