package apw.sec.android.gallery

import android.view.LayoutInflater
import android.view.View
import android.content.Intent
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import apw.sec.android.gallery.data.MediaHub
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import java.util.UUID

class SearchAdapter(
    private val mediaFiles: List<MediaFile>,
) : RecyclerView.Adapter<SearchAdapter.MediaViewHolder>() {

    private var filteredList: MutableList<MediaFile> = ArrayList(mediaFiles)

    inner class MediaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
        val playButton: ImageView = view.findViewById(R.id.playButton)
        init {
            view.setOnClickListener {
                val key: String = UUID.randomUUID().toString()
                MediaHub.save(key, ArrayList(mediaFiles))
                val context = itemView.context
                val intent = Intent(context, ViewActivity::class.java).apply {
                    putExtra("media_key", key)
                    putExtra("position", adapterPosition)
                }
                context.startActivity(intent)
            }
        }
    }
    
    fun filter(query: String?) {
        filteredList.clear()
        if (query.isNullOrEmpty()) {
            filteredList.addAll(mediaFiles)
        } else {
            val filePattern: String = query.lowercase().trim()
            for (media in mediaFiles) {
                if (media.name.lowercase().contains(filePattern)) {
                    filteredList.add(media)
                }
            }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.image_item, parent, false)
        return MediaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val mediaFile = filteredList[position]
        if(mediaFile.isVideo()){
            holder.playButton.visibility = View.VISIBLE
        } else{
            holder.playButton.visibility = View.GONE
        }
        Glide.with(holder.itemView.context)
            .load(mediaFile.uri)
            .centerCrop()
            .transform(RoundedCorners(20))
            .into(holder.imageView)
    }

    override fun getItemCount(): Int = filteredList.size
}