package apw.sec.android.gallery

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.widget.ImageView.ScaleType.CENTER_CROP
import androidx.recyclerview.widget.RecyclerView
import apw.sec.android.gallery.components.FilmImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import androidx.core.net.toUri

class FilmstripAdapter(
    private val imageList: List<MediaFile>,
    private val onItemClick: (Int) -> Unit
): RecyclerView.Adapter<FilmstripAdapter.FilmstripViewHolder>(){

    private var selectedPosition = 0

    inner class FilmstripViewHolder(val imageView: FilmImageView): RecyclerView.ViewHolder(imageView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilmstripViewHolder {
        val imageView = FilmImageView(parent.context).apply{
            layoutParams = ViewGroup.MarginLayoutParams(100, 140).apply {
                // Reduce margin for selected item, increase for others
                setMargins(4, 4, 4, 4)
            }
            scaleType = CENTER_CROP
        }
        return FilmstripViewHolder(imageView)
    }

    override fun onBindViewHolder(holder: FilmstripViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val uri = imageList[position].uri.toUri()
        val requestOptions = RequestOptions().centerCrop()
        Glide.with(holder.imageView.context).load(uri).apply(requestOptions).into(holder.imageView)

        // Update margins based on selection
        val layoutParams = holder.imageView.layoutParams as ViewGroup.MarginLayoutParams
        if(position == selectedPosition){
            holder.imageView.scaleX = 1.15f
            holder.imageView.scaleY = 1.15f
            // Less margin for selected item
            layoutParams.setMargins(4, 4, 4, 4)
        } else {
            holder.imageView.scaleX = 1.0f
            holder.imageView.scaleY = 1.0f
            // More margin for non-selected items
            layoutParams.setMargins(8, 8, 8, 8)
        }
        holder.imageView.layoutParams = layoutParams

        holder.imageView.setOnClickListener{
            val previous = selectedPosition
            selectedPosition = position
            notifyItemChanged(previous)
            notifyItemChanged(position)
            onItemClick(position)
        }
    }

    override fun getItemCount(): Int {
        return imageList.size
    }

    fun setSelectedPosition(position: Int){
        val previous = selectedPosition
        selectedPosition = position
        notifyItemChanged(previous)
        notifyItemChanged(position)
    }
}