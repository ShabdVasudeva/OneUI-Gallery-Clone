package apw.sec.android.gallery.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.ImageView
import apw.sec.android.gallery.R

class FilmImageView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
): ImageView(context, attributeSet, defStyleAttr) {
    private var cornerRadius: Float = 10f
    private val path = Path()

    init {
        attributeSet?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.FilmImageView, 0, 0)
            cornerRadius =
                typedArray.getDimension(R.styleable.FilmImageView_customCorner, cornerRadius)
            typedArray.recycle()
        }
    }

    override fun onDraw(canvas: Canvas) {
        path.reset()
        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        path.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
        canvas.clipPath(path)
        super.onDraw(canvas)
    }
}