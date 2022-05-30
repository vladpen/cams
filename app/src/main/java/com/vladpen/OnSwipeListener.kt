package com.vladpen

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.MotionEvent.*
import android.view.View
import android.view.ViewGroup
import com.vladpen.cams.MainApp.Companion.context
import kotlin.math.abs

private const val SWIPE_THRESHOLD = 100
private const val SWIPE_VELOCITY_THRESHOLD = 100

open class OnSwipeListener : View.OnTouchListener {
    private val gestureDetector = GestureDetector(context, GestureListener())
    private lateinit var childView: View
    private var childViewX = 0f

    override fun onTouch(v: View?, e: MotionEvent?): Boolean {
        if (e == null || v == null)
            return true

        childView = (v as ViewGroup).getChildAt(0)

        if (e.action == ACTION_DOWN) {
            v.alpha = 0.5f
            childViewX = childView.x
        } else if (e.action == ACTION_UP) {
            v.alpha = 1f
            childView.x = childViewX
        }
        return gestureDetector.onTouchEvent(e) || v.performClick()
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

        override fun onScroll(
            e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float
        ): Boolean {
            childView.x -= distanceX
            return true
        }
    }

    open fun onSwipe() {}
}