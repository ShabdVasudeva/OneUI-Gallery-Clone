package apw.sec.android.gallery

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.provider.MediaStore
import android.graphics.Bitmap
import androidx.recyclerview.widget.RecyclerView
import androidx.core.net.toUri
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

        // Load media (works for both images and videos)
        Glide.with(context)
            .load(mediaFile.uri)
            .into(holder.binding.imageView)

        // Set click listener to toggle UI for both photos and videos
        holder.binding.imageView.setOnClickListener {
            onImageClick()
        }
    }

    override fun getItemCount() = mediaFiles.size
}