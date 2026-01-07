package apw.sec.android.gallery.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Space
import androidx.core.view.setPadding
import com.bumptech.glide.Glide
import apw.sec.android.gallery.R

class GroupCoverView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val gap = resources.getDimensionPixelSize(R.dimen.group_cover_gap)
    private val cornerRadius = resources.getDimension(R.dimen.album_corner_radius)

    private val clipPath = Path()
    private val rect = RectF()

    init {
        clipChildren = false
        clipToPadding = false
        setWillNotDraw(false)
    }

    fun setMedia(uris: List<String>) {
        removeAllViews()

        when (uris.size) {
            0 -> return
            1 -> one(uris)
            2 -> two(uris)
            3 -> three(uris)
            else -> four(uris)
        }
    }


    private fun one(uris: List<String>) {
        addView(makeImage(uris[0]), match())
    }

    private fun two(uris: List<String>) {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(makeImage(uris[0]), weightH(1f))
            addView(spaceV())
            addView(makeImage(uris[1]), weightH(1f))
        }
        addView(row, match())
    }

    private fun three(uris: List<String>) {
        val leftColumn = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(makeImage(uris[0]), weightV(1f))
            addView(spaceH())
            addView(makeImage(uris[1]), weightV(1f))
        }

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(leftColumn, weightH(1f))
            addView(spaceV())
            addView(makeImage(uris[2]), weightH(1f))
        }

        addView(row, match())
    }

    private fun four(uris: List<String>) {
        val grid = GridLayout(context).apply {
            rowCount = 2
            columnCount = 2
        }

        uris.take(4).forEachIndexed { index, uri ->
            val image = makeImage(uri)

            val row = index / 2
            val col = index % 2

            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = 0
                rowSpec = GridLayout.spec(row, 1f)
                columnSpec = GridLayout.spec(col, 1f)

                // Only add gaps on inner edges
                setMargins(
                    if (col == 0) 0 else gap / 2,  // left
                    if (row == 0) 0 else gap / 2,  // top
                    if (col == 1) 0 else gap / 2,  // right
                    if (row == 1) 0 else gap / 2   // bottom
                )
            }

            grid.addView(image, params)
        }

        addView(grid, match())
    }

    private fun makeImage(uri: String): ImageView =
        ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            Glide.with(this).load(uri).into(this)
        }

    private fun match() =
        LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

    private fun weightH(weight: Float) =
        LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, weight)

    private fun weightV(weight: Float) =
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, weight)

    private fun spaceV(): Space =
        Space(context).apply {
            layoutParams = LinearLayout.LayoutParams(gap, LinearLayout.LayoutParams.MATCH_PARENT)
        }

    private fun spaceH(): Space =
        Space(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, gap)
        }

    // Rounded corners clipping
    override fun dispatchDraw(canvas: Canvas) {
        val save = canvas.save()
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        clipPath.reset()
        clipPath.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
        canvas.clipPath(clipPath)
        super.dispatchDraw(canvas)
        canvas.restoreToCount(save)
    }
}
