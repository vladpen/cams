package com.vladpen

import android.animation.ValueAnimator
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
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

    fun fadeOut(views: Array<View>, duration: Long = 500) {
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

    fun toggle(view: LinearLayout, duration: Long = 300) {
        // show/hide given view with slideDown/slideUp effect
        var from = 0
        var to = 0
        if (view.height > 0)
            from = view.height
        else
            to = getHeight(view)

        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, from)

        val valueAnimator = ValueAnimator.ofFloat(from.toFloat(), to.toFloat())
        valueAnimator.addUpdateListener {
            params.height = (it.animatedValue as Float).toInt()
            view.layoutParams = params
        }
        valueAnimator.duration = duration
        valueAnimator.start()
    }

    private fun getHeight(view: View): Int {
        // calculate total height including hidden views
        view.measure(
            View.MeasureSpec.makeMeasureSpec(view.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        return view.measuredHeight
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
        if (handler != null) {
            handler?.removeCallbacksAndMessages(null)
            handler = null
        }
        if (animation != null) {
            animation?.cancel()
            animation?.setAnimationListener(null)
            animation = null
        }
    }
}