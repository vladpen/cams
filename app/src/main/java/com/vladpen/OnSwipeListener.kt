package com.vladpen

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.vladpen.cams.MainApp.Companion.context
import kotlin.math.abs

private const val SWIPE_THRESHOLD = 100
private const val SWIPE_VELOCITY_THRESHOLD = 100

open class OnSwipeListener : View.OnTouchListener {
    private val gestureDetector = GestureDetector(context, GestureListener())

    override fun onTouch(v: View?, e: MotionEvent?): Boolean {
        if (e == null)
            return true
        val res = gestureDetector.onTouchEvent(e)
        return res || v?.performClick() ?: true
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onFling(
            e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            try {
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y
                if (abs(diffX) < abs(diffY))
                    return false
                if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    onSwipe()
                    return true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return false
        }
    }

    open fun onSwipe() {}
}