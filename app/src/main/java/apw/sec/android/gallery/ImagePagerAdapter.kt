package apw.sec.android.gallery

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import apw.sec.android.gallery.databinding.ItemImageViewerBinding
import com.bumptech.glide.Glide

class ImagePagerAdapter(
    private val context: Context,
    private val mediaFiles: List<MediaFile>,
    private val onImageClick: () -> Unit
) : RecyclerView.Adapter<ImagePagerAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemImageViewerBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemImageViewerBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val mediaFile = mediaFiles[position]
        if (mediaFile.name.endsWith(".mp4")) {
            Glide.with(context)
                .load(mediaFile.uri)
                .into(holder.binding.imageView)
            holder.binding.playButton.visibility = View.VISIBLE
            holder.binding.playButton.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(mediaFile.uri), "video/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
            }
            holder.binding.imageView.setOnClickListener(null)

        } else {
            holder.binding.playButton.visibility = View.GONE
            Glide.with(context)
                .load(mediaFile.uri)
                .into(holder.binding.imageView)
            holder.binding.imageView.setOnClickListener {
                onImageClick()
            }
        }
    }

    override fun getItemCount() = mediaFiles.size
}