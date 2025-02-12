package apw.sec.android.gallery

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import apw.sec.android.gallery.R

class ApwSearch @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private var searchInput: EditText
    private var clearIcon: ImageView
    private var searchListener: SearchViewListener? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.search_view, this, true)
        searchInput = findViewById(R.id.input)
        clearIcon = findViewById(R.id.clear)

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.length ?: 0 > 0) {
                    clearIcon.visibility = View.VISIBLE
                    searchListener?.onQueryTextChange(s.toString())
                } else {
                    clearIcon.visibility = View.GONE
                }
            }

            override fun afterTextChanged(editable: Editable?) {}
        })

        clearIcon.setOnClickListener {
            searchInput.setText("")
        }
    }

    fun setSearchViewListener(listener: SearchViewListener) {
        searchListener = listener
    }

    interface SearchViewListener {
        fun onQueryTextSubmit(query: String)
        fun onQueryTextChange(query: String)
    }
}