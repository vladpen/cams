package com.vladpen

import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.RelativeLayout
import com.vladpen.cams.MainApp.Companion.context
import com.vladpen.cams.R

object Effects {
    private var animation: Animation? = null
    private var handler: Handler? = null

    fun delayedFadeOut(views: Array<View>, delay: Long = 3000) {
        handler = Handler(Looper.getMainLooper())
        handler?.postDelayed({
            fadeOut(views)
        }, delay)
    }

    private fun fadeOut(views: Array<View>, duration: Long = 500) {
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

    fun dimmer(view: ViewGroup) {
        val overlay = RelativeLayout(context)
        overlay.layoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
        )
        overlay.setBackgroundColor(context.getColor(R.color.overlay_foreground))
        view.addView(overlay)
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
}