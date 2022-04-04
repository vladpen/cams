package com.vladpen

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.max
import kotlin.math.min
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.sign

class VideoGestureDetector(context: Context, private val view: View) {
    private val ratio = 16f / 9f
    private val maxScaleFactor = 20f
    private var scaleFactor = 1f

    private val visibleSize = object {
        var width = 0
        var height = 0
        var availableX = 0f
        var availableY = 0f
        fun set() {
            if (view.width > view.height) {
                height = view.height
                width = round(height * ratio).toInt()
            } else {
                width = view.width
                height = round(width / ratio).toInt()
            }
            availableX = max(0f, (width * (scaleFactor - 1) - view.width + width) / 2)
            availableY = max(0f, (height * (scaleFactor - 1) - view.height + height) / 2)
        }
    }

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

    private fun move(distanceX: Float, distanceY: Float) {
        if (abs(view.x - distanceX) < visibleSize.availableX)
            view.x -= distanceX
        else
            view.x = visibleSize.availableX * sign(view.x)

        if (abs(view.y - distanceY) < visibleSize.availableY)
            view.y -= distanceY
        else
            view.y = visibleSize.availableY * sign(view.y)
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

            visibleSize.set()
            val distanceX = view.x * (1 - detector.scaleFactor)
            val distanceY = view.y * (1 - detector.scaleFactor)
            move(distanceX, distanceY)
            return true
        }

        override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
            visibleSize.set()
            return super.onScaleBegin(detector)
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
            return true
        }

        override fun onDown(e: MotionEvent?): Boolean {
            visibleSize.set()
            return super.onDown(e)
        }
    }
}