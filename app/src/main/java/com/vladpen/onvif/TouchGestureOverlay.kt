package com.vladpen.onvif

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.*

class TouchGestureOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val centerRadius = 100f * resources.displayMetrics.density // Larger touch area
    private val dotRadius = 15f * resources.displayMetrics.density
    private val movementThreshold = 5f * resources.displayMetrics.density // More responsive
    
    private var centerX = 0f
    private var centerY = 0f
    private var dotX = 0f
    private var dotY = 0f
    private var dotAlpha = 0f
    private var isActive = false
    
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    
    private var gestureListener: PTZGestureListener? = null
    private var returnAnimator: ValueAnimator? = null
    private var fadeAnimator: ValueAnimator? = null
    
    init {
        // Ensure the view can receive touch events
        isClickable = true
        isFocusable = true
        android.util.Log.d("PTZ_INIT", "TouchGestureOverlay initialized")
    }
    
    interface PTZGestureListener {
        fun onGestureStart()
        fun onGestureMove(panSpeed: Float, tiltSpeed: Float)
        fun onGestureEnd()
    }
    
    fun setGestureListener(listener: PTZGestureListener?) {
        gestureListener = listener
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        android.util.Log.d("PTZ_SIZE", "Overlay size: ${w}x${h}, center: ($centerX, $centerY), radius: $centerRadius")
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        android.util.Log.d("PTZ_TOUCH", "Touch event: ${event.action} at (${event.x}, ${event.y})")
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val dx = event.x - centerX
                val dy = event.y - centerY
                val distance = sqrt(dx * dx + dy * dy)
                
                android.util.Log.d("PTZ_TOUCH", "Distance from center: $distance, centerRadius: $centerRadius")
                
                if (distance <= centerRadius) {
                    android.util.Log.d("PTZ_TOUCH", "Touch activated! Showing dot at (${event.x}, ${event.y})")
                    cancelAnimations()
                    isActive = true
                    dotX = event.x
                    dotY = event.y
                    dotAlpha = 1f
                    gestureListener?.onGestureStart()
                    invalidate()
                    return true
                }
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (isActive) {
                    dotX = event.x
                    dotY = event.y
                    
                    val dx = dotX - centerX
                    val dy = dotY - centerY
                    val distance = sqrt(dx * dx + dy * dy)
                    
                    if (distance > movementThreshold) {
                        val panSpeed = (dx / (width / 2f)).coerceIn(-1f, 1f)
                        val tiltSpeed = -(dy / (height / 2f)).coerceIn(-1f, 1f) // Invert Y
                        
                        gestureListener?.onGestureMove(panSpeed, tiltSpeed)
                    }
                    
                    invalidate()
                    return true
                }
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isActive) {
                    isActive = false
                    gestureListener?.onGestureEnd()
                    startReturnAnimation()
                    return true
                }
            }
        }
        return false
    }
    
    private fun cancelAnimations() {
        returnAnimator?.cancel()
        fadeAnimator?.cancel()
    }
    
    private fun startReturnAnimation() {
        val startX = dotX
        val startY = dotY
        
        returnAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                dotX = startX + (centerX - startX) * progress
                dotY = startY + (centerY - startY) * progress
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    startFadeAnimation()
                }
            })
        }
        returnAnimator?.start()
    }
    
    private fun startFadeAnimation() {
        fadeAnimator = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 200
            addUpdateListener { animator ->
                dotAlpha = animator.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    dotAlpha = 0f
                    invalidate()
                }
            })
        }
        fadeAnimator?.start()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        android.util.Log.d("PTZ_DRAW", "Drawing dot: alpha=$dotAlpha at ($dotX, $dotY)")
        
        if (dotAlpha > 0f) {
            dotPaint.alpha = (255 * dotAlpha).toInt()
            canvas.drawCircle(dotX, dotY, dotRadius, dotPaint)
            android.util.Log.d("PTZ_DRAW", "Dot drawn with alpha ${dotPaint.alpha}")
        }
    }
}
