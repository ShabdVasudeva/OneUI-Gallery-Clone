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

    private var  selectedPosition = 0

    inner class FilmstripViewHolder(val imageView: FilmImageView): RecyclerView.ViewHolder(imageView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilmstripViewHolder {
        val imageView = FilmImageView(parent.context).apply{
            layoutParams = ViewGroup.LayoutParams(110, 150)
            scaleType = CENTER_CROP
            setPadding(8, 8, 8, 8)
        }
        return FilmstripViewHolder(imageView)
    }

    override fun onBindViewHolder(holder: FilmstripViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val uri = imageList[position].uri.toUri()
        val requestOptions = RequestOptions().centerCrop()
        Glide.with(holder.imageView.context).load(uri).apply(requestOptions).into(holder.imageView)
        if(position == selectedPosition){
            holder.imageView.scaleX = 1.2f
            holder.imageView.scaleY = 1.2f
        } else{
            holder.imageView.scaleX = 1.0f
            holder.imageView.scaleY = 1.0f
        }
        holder.imageView.setOnClickListener{
            val preview = selectedPosition
            selectedPosition = position
            notifyItemChanged(preview)
            notifyItemChanged(position)
            onItemClick(position)
        }
    }

    override fun getItemCount(): Int {
        return imageList.size
    }

    fun setSelectedPosition(position: Int){
        val preview = selectedPosition
        selectedPosition = position
        notifyItemChanged(preview)
        notifyItemChanged(position)
    }
}