package com.vladpen.onvif

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator

class MotionIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val borderPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 8f
        isAntiAlias = true
    }

    private val iconPaint = Paint().apply {
        color = Color.RED
        alpha = 200
        isAntiAlias = true
        textSize = 48f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private var isMotionDetected = false
    private var borderAlpha = 0f
    private var iconAlpha = 0f
    private var fadeAnimator: ValueAnimator? = null

    fun showMotionDetected() {
        isMotionDetected = true
        
        // Cancel any existing animation
        fadeAnimator?.cancel()
        
        // Start fade-in animation
        fadeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val value = animator.animatedValue as Float
                borderAlpha = value
                iconAlpha = value
                updatePaintAlpha()
                invalidate()
            }
            start()
        }
        
        // Schedule fade-out after 3 seconds
        postDelayed({
            hideMotionDetected()
        }, 3000)
    }

    fun hideMotionDetected() {
        if (!isMotionDetected) return
        
        fadeAnimator?.cancel()
        
        fadeAnimator = ValueAnimator.ofFloat(borderAlpha, 0f).apply {
            duration = 1000
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val value = animator.animatedValue as Float
                borderAlpha = value
                iconAlpha = value
                updatePaintAlpha()
                invalidate()
                
                if (value <= 0f) {
                    isMotionDetected = false
                }
            }
            start()
        }
    }

    private fun updatePaintAlpha() {
        borderPaint.alpha = (borderAlpha * 255).toInt()
        iconPaint.alpha = (iconAlpha * 200).toInt()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (!isMotionDetected || borderAlpha <= 0f) return
        
        val width = width.toFloat()
        val height = height.toFloat()
        
        // Draw red border around the entire view
        val borderRect = RectF(4f, 4f, width - 4f, height - 4f)
        canvas.drawRect(borderRect, borderPaint)
        
        // Draw motion icon in top-right corner
        val iconX = width - 60f
        val iconY = 60f
        
        // Draw motion icon background circle
        val iconBgPaint = Paint().apply {
            color = Color.BLACK
            alpha = (iconAlpha * 100).toInt()
            isAntiAlias = true
        }
        canvas.drawCircle(iconX, iconY, 25f, iconBgPaint)
        
        // Draw motion icon (simplified motion symbol)
        canvas.drawText("⚡", iconX, iconY + 15f, iconPaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        fadeAnimator?.cancel()
    }
}
