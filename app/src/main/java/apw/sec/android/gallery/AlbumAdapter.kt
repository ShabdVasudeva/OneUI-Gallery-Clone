package apw.sec.android.gallery

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import apw.sec.android.gallery.components.Image

class AlbumAdapter(
    private val context: Context,
    private val mediaFiles: List<MediaFile>
) : RecyclerView.Adapter<AlbumAdapter.MediaViewHolder>() {

    private val albumMap: Map<String, List<MediaFile>> = mediaFiles.groupBy { it.folderName ?: "Unknown" }
    private val albumList = albumMap.keys.toList()

    inner class MediaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: Image = view.findViewById(R.id.imageView)
        val folderNameTextView: TextView = view.findViewById(R.id.text)

        init {
            view.setOnClickListener {
                val folderName = albumList[adapterPosition]
                val folderMediaList = albumMap[folderName] ?: emptyList()

                val intent = Intent(context, AlbumViewer::class.java).apply {
                    putExtra("folderName", folderName)
                    putExtra("mediauri", folderMediaList[adapterPosition].uri.toString())
                }
                context.startActivity(intent)
            }
        }
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
    }

    override fun getItemCount(): Int = albumList.size
}