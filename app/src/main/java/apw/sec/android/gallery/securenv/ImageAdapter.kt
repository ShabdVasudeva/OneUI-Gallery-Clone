package apw.sec.android.gallery.securenv

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import apw.sec.android.gallery.R
import android.content.Intent
import apw.sec.android.gallery.securenv.*
import androidx.recyclerview.widget.RecyclerView

class ImageAdapter(
    private val imageList: List<ImageItem>,
    private val onMediaClick: (ImageItem)-> Unit
) : RecyclerView.Adapter<ImageAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
        init{
            view.setOnClickListener {
                val context = itemView.context
                val intent = Intent(context, PrivateImageViewer::class.java).apply {
                    putParcelableArrayListExtra("mediaList", ArrayList(imageList))
                    putExtra("position", adapterPosition)
                }
                context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.image_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val imagePath = imageList[position].imagePath
        val bitmap = BitmapFactory.decodeFile(imagePath)
        holder.imageView.setImageBitmap(bitmap)
    }

    override fun getItemCount(): Int = imageList.size
}