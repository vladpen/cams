package com.vladpen.cams

import android.view.View
import android.view.ViewTreeObserver
import android.widget.RelativeLayout
import androidx.core.graphics.Insets
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sqrt

private const val ASPECT_RATIO = 16f / 9f

interface Layout {
    var rootView: View
    var layoutListener: ViewTreeObserver.OnGlobalLayoutListener
    var insets: Insets?
    var hideBars: Boolean

    fun initLayout(view: View) {
        rootView = view
        layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            resizeLayout()
            rootView.viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
        }
        rootView.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
    }

    fun resizeGrid(fragments: ArrayList<StreamFragment>) {
        val (rootWidth, rootHeight) = getRootSize()

        val cellQty = if (rootHeight > rootWidth)
            max(4, fragments.count() - 5 / fragments.count()) / 4.0 // 1 column for up to 5 cells
        else
            fragments.count().toDouble()
        val columnCount = ceil(sqrt(cellQty)).toInt()
        val rowCount = ceil(fragments.count() / columnCount.toDouble()).toInt()

        val frameHeight: Int
        val rootAspectRatio = rootWidth.toFloat() / rootHeight.toFloat()
        val videoAspectRatio = ASPECT_RATIO * columnCount / rowCount
        if (rootAspectRatio > videoAspectRatio) { // vertical margins
            frameHeight = rootHeight / rowCount
            hideBars = true
        } else { // horizontal margins
            frameHeight = ((rootWidth / columnCount) / ASPECT_RATIO).toInt()
            hideBars = false
        }
        val frameWidth = (frameHeight * ASPECT_RATIO).toInt()

        for ((i, f) in fragments.withIndex()) {
            val layout = f.getFrame()
            val params = layout.layoutParams as RelativeLayout.LayoutParams
            params.width = frameWidth
            params.height = frameHeight
            layout.layoutParams = params

            if (i == 0)
                continue

            params.removeRule(RelativeLayout.BELOW)
            params.removeRule(RelativeLayout.RIGHT_OF)
            params.marginStart = 0

            val restCount = fragments.count() - i
            if (i % columnCount != 0) // except first cell in each row
                params.addRule(RelativeLayout.RIGHT_OF, fragments[i - 1].getFrame().id)
            else if (restCount <= columnCount) // first cell in the last row, center horizontally
                params.marginStart = (frameWidth * (columnCount - restCount) / 2)
            if (i >= columnCount) // except first row
                params.addRule(RelativeLayout.BELOW, fragments[i - columnCount].getFrame().id)
        }
    }

    fun resizeVideo(layout: View) {
        val (rootWidth, rootHeight) = getRootSize()

        val frameWidth: Int
        val frameHeight: Int
        val rootAspectRatio = rootWidth.toFloat() / rootHeight.toFloat()
        if (rootAspectRatio > ASPECT_RATIO) { // vertical margins
            frameHeight = rootHeight
            frameWidth = (frameHeight * ASPECT_RATIO).toInt()
            hideBars = true
        } else { // horizontal margins
            frameWidth = rootWidth
            frameHeight = (frameWidth / ASPECT_RATIO).toInt()
            hideBars = false
        }
        val params = layout.layoutParams
        params.width = frameWidth
        params.height = frameHeight
        layout.layoutParams = params
    }

    private fun getRootSize(): Pair<Int, Int> {
        return Pair(
            rootView.width - (insets?.left ?: 0) - (insets?.right ?: 0),
            rootView.height - (insets?.top ?: 0) - (insets?.bottom ?: 0)
        )
    }

    fun resizeLayout() { }
}