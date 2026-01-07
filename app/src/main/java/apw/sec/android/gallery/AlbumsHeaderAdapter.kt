package apw.sec.android.gallery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AlbumsHeaderAdapter(
    private val onViewAllClick: () -> Unit
) : RecyclerView.Adapter<AlbumsHeaderAdapter.HeaderVH>() {

    private var showEssential = false
    private var visible = true

    fun update(showEssential: Boolean) {
        this.showEssential = showEssential
        notifyItemChanged(0)
    }

    fun setVisible(show: Boolean) {
        if (visible == show) return
        visible = show
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return if (visible) 1 else 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_albums_header, parent, false)
        return HeaderVH(view)
    }

    override fun onBindViewHolder(holder: HeaderVH, position: Int) {
        holder.bind(showEssential)
    }

    inner class HeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        private val title = view.findViewById<TextView>(R.id.title_text)
        private val viewAll = view.findViewById<Button>(R.id.view_more_button)

        fun bind(showEssential: Boolean) {
            title.text =
                if (showEssential) "Essential albums" else "All Albums"

            viewAll.visibility =
                if (showEssential) View.VISIBLE else View.GONE

            viewAll.setOnClickListener {
                onViewAllClick()
            }
        }
    }
}
