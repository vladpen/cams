package com.vladpen

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.core.content.ContextCompat.getColor

object Effects {
    private var animation: Animation? = null
    private var handler: Handler? = null

    fun delayedFadeOut(context: Context, views: Array<View>, delay: Long = 3000) {
        handler = Handler(Looper.getMainLooper())
        handler?.postDelayed({
            fadeOut(context, views)
        }, delay)
    }

    private fun fadeOut(context: Context, views: Array<View>, duration: Long = 500) {
        animation = AnimationUtils.loadAnimation(context, android.R.anim.fade_out)
        animation?.duration = duration
        animation?.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationEnd(a: Animation) {
                for (view in views)
                    view.visibility = View.GONE
            }
            override fun onAnimationStart(a: Animation) {
            }
            override fun onAnimationRepeat(a: Animation) {
            }
        })
        for (view in views)
            view.startAnimation(animation)
    }

    fun cancel() {
        if (handler != null)
            handler?.removeCallbacksAndMessages(null)
            handler = null
        if (animation != null) {
            animation?.cancel()
            animation?.setAnimationListener(null)
            animation = null
        }
    }

    fun setTextViewClickable(context: Context, view: TextView, color: Int) {
        view.setTextColor(getColor(context, color))
        val outValue = TypedValue()
        context.theme
            .resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        view.setBackgroundResource(outValue.resourceId)

    }
}