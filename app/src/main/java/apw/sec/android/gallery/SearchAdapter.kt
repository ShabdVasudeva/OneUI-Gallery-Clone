package apw.sec.android.gallery

import android.view.LayoutInflater
import android.view.View
import android.content.Intent
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
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
        val videoDurationOverlay: LinearLayout = view.findViewById(R.id.videoDurationOverlay)
        val videoDurationText: TextView = view.findViewById(R.id.videoDuration)

        init {
            view.setOnClickListener {
                val key: String = UUID.randomUUID().toString()
                MediaHub.save(key, ArrayList(filteredList))
                val context = itemView.context
                val intent = Intent(context, ViewActivity::class.java).apply {
                    putExtra("media_key", key)
                    putExtra("position", bindingAdapterPosition)
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

        if (mediaFile.isVideo()) {
            // Hide center play button
            holder.playButton.visibility = View.GONE

            // Show duration overlay
            holder.videoDurationOverlay.visibility = View.VISIBLE

            // Format and set duration
            val duration = mediaFile.duration ?: 0L
            holder.videoDurationText.text = formatDuration(duration)
        } else {
            // Hide both for images
            holder.playButton.visibility = View.GONE
            holder.videoDurationOverlay.visibility = View.GONE
        }

        Glide.with(holder.itemView.context)
            .load(mediaFile.uri)
            .centerCrop()
            .transform(RoundedCorners(20))
            .into(holder.imageView)
    }

    override fun getItemCount(): Int = filteredList.size

    private fun formatDuration(durationMs: Long): String {
        if (durationMs == 0L) {
            return "0:00"
        }

        val seconds = (durationMs / 1000).toInt()
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60

        return if (minutes >= 60) {
            val hours = minutes / 60
            val remainingMinutes = minutes % 60
            String.format("%d:%02d:%02d", hours, remainingMinutes, remainingSeconds)
        } else {
            String.format("%d:%02d", minutes, remainingSeconds)
        }
    }
}