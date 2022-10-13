package com.vladpen

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.vladpen.cams.MainApp.Companion.context
import kotlin.math.*

/**
 * @param view outer (root) view
 * @param videoView inner view
 */
class VideoGestureDetector(private val view: View, private val videoView: View) {
    private val maxScaleFactor = 20f
    private var scaleFactor = 1f
    private var maxX = 0f
    private var maxY = 0f

    private val gestureDetector = GestureDetector(
        context,
        VideoDetectorListener()
    )

    private val scaleGestureDetector = ScaleGestureDetector(
        context,
        VideoScaleDetectorListener()
    )

    fun onTouchEvent(event: MotionEvent): Boolean {
        val scaleEventResult = scaleGestureDetector.onTouchEvent(event)
        return if (scaleEventResult == scaleGestureDetector.isInProgress) true
        else gestureDetector.onTouchEvent(event)
    }

    fun reset() {
        scaleFactor = 1f
        view.scaleX = scaleFactor
        view.scaleY = scaleFactor
        view.x = 0f
        view.y = 0f
    }

    private fun setLimit() {
        val videoWidth = videoView.width
        val videoHeight = videoView.height
        maxX = max(0f, (videoWidth * (scaleFactor - 1) - view.width + videoWidth) / 2)
        maxY = max(0f, (videoHeight * (scaleFactor - 1) - view.height + videoHeight) / 2)
    }

    private fun move(distanceX: Float, distanceY: Float) {
        if (abs(view.x - distanceX) < maxX)
            view.x -= distanceX
        else
            view.x = maxX * sign(view.x)

        if (abs(view.y - distanceY) < maxY)
            view.y -= distanceY
        else
            view.y = maxY * sign(view.y)
    }

    private inner class VideoScaleDetectorListener :
            ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = max(1f, min(scaleFactor, 20f))
            view.scaleX = scaleFactor
            view.scaleY = scaleFactor

            if (scaleFactor == maxScaleFactor && detector.scaleFactor > 1)
                return true

            setLimit()
            val distanceX = view.x * (1 - detector.scaleFactor)
            val distanceY = view.y * (1 - detector.scaleFactor)
            move(distanceX, distanceY)
            return true
        }
    }

    private inner class VideoDetectorListener : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            move(distanceX, distanceY)
            return scaleFactor > 1f // allow single click handling if the image is not scaled
        }

        override fun onDown(e: MotionEvent): Boolean {
            setLimit()
            return super.onDown(e)
        }
    }
}