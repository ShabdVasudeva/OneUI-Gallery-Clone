package apw.sec.android.gallery

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners

class MediaAdapter(
    private val mediaFiles: List<MediaFile>
) : RecyclerView.Adapter<MediaAdapter.MediaViewHolder>() {

    inner class MediaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
        val playButton: ImageView = view.findViewById(R.id.playButton)
        init {
            view.setOnClickListener {
                val context = itemView.context
                val intent = Intent(context, ViewActivity::class.java).apply {
                    putParcelableArrayListExtra("mediaList", ArrayList(mediaFiles))
                    putExtra("position", adapterPosition)
                }
                context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.image_item, parent, false)
        return MediaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val mediaFile = mediaFiles[position]
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

    override fun getItemCount(): Int = mediaFiles.size
}