package apw.sec.android.gallery.securenv

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import apw.sec.android.gallery.databinding.ItemImageViewerBinding
import com.bumptech.glide.Glide
import android.graphics.*

class PrivatePagerAdapter(
    private val context: Context,
    private val mediaFiles: List<ImageItem>,
    private val onImageClick: () -> Unit
): RecyclerView.Adapter<PrivatePagerAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemImageViewerBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemImageViewerBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val mediaFile = mediaFiles[position].imagePath
        val bitmap = BitmapFactory.decodeFile(mediaFile)
        holder.binding.imageView.setImageBitmap(bitmap)
        holder.binding.imageView.setOnClickListener {
            onImageClick()
        }
    }

    override fun getItemCount() = mediaFiles.size
}