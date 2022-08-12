package com.vladpen

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.vladpen.cams.MainApp.Companion.context
import kotlin.math.*

private const val ASPECT_RATIO = 16f / 9f

class VideoGestureDetector(private val view: View) {
    private val maxScaleFactor = 20f
    private var scaleFactor = 1f
    private var aspectRatio = ASPECT_RATIO
    private var width = 0
    private var height = 0
    private var availableX = 0f
    private var availableY = 0f

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

    fun reset(videoAspectRatio: Float = ASPECT_RATIO) {
        aspectRatio = videoAspectRatio
        scaleFactor = 1f
        view.scaleX = scaleFactor
        view.scaleY = scaleFactor
        view.x = 0f
        view.y = 0f
    }

    private fun setSize() {
        if (view.width.toFloat() / view.height.toFloat() > aspectRatio) {
            height = view.height
            width = (height * aspectRatio).roundToInt()
        } else {
            width = view.width
            height = (width / aspectRatio).roundToInt()
        }
    }

    private fun setLimit() {
        availableX = max(0f, (width * (scaleFactor - 1) - view.width + width) / 2)
        availableY = max(0f, (height * (scaleFactor - 1) - view.height + height) / 2)
    }

    private fun move(distanceX: Float, distanceY: Float) {
        if (abs(view.x - distanceX) < availableX)
            view.x -= distanceX
        else
            view.x = availableX * sign(view.x)

        if (abs(view.y - distanceY) < availableY)
            view.y -= distanceY
        else
            view.y = availableY * sign(view.y)
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
            e1: MotionEvent?,
            e2: MotionEvent?,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            move(distanceX, distanceY)
            return scaleFactor > 1f // allow single click handling if the image is not scaled
        }

        override fun onDown(e: MotionEvent?): Boolean {
            setSize()
            setLimit()
            return super.onDown(e)
        }
    }
}