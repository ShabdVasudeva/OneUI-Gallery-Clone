package apw.sec.android.gallery

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import apw.sec.android.gallery.components.Image
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import apw.sec.android.gallery.data.MediaHub
import java.util.UUID

class MediaAdapter(
    private val mediaFiles: MutableList<MediaFile>
) : RecyclerView.Adapter<MediaAdapter.MediaViewHolder>() {

    inner class MediaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: Image = view.findViewById(R.id.imageView)
        val playButton: ImageView = view.findViewById(R.id.playButton)
        val videoDurationOverlay: LinearLayout = view.findViewById(R.id.videoDurationOverlay)
        val videoDurationText: TextView = view.findViewById(R.id.videoDuration)

        init {
            view.setOnClickListener {
                val key: String = UUID.randomUUID().toString()
                MediaHub.save(key, mediaFiles)
                val context = itemView.context
                val intent = Intent(context, ViewActivity::class.java).apply {
                    putExtra("media_key", key)
                    putExtra("position", bindingAdapterPosition)
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

    override fun getItemCount(): Int = mediaFiles.size

    private fun formatDuration(durationMs: Long): String {
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