package com.vladpen.onvif

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlinx.coroutines.*

class PTZControlView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var ptzController: PTZController? = null
    private var controlJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private val paint = Paint().apply {
        color = Color.WHITE
        alpha = 200
        isAntiAlias = true
    }
    
    private val strokePaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }

    private var centerX = 0f
    private var centerY = 0f
    private var controlRadius = 0f
    private var buttonRadius = 0f
    
    private var lastHideTime = 0L
    private val hideDelay = 5000L

    interface PTZControlListener {
        fun onPTZCommand(direction: PTZDirection, isPressed: Boolean)
        fun onZoomCommand(factor: Float)
    }

    private var listener: PTZControlListener? = null

    fun setPTZController(controller: PTZController?) {
        ptzController = controller
        visibility = if (controller != null) VISIBLE else GONE
    }

    fun setListener(listener: PTZControlListener?) {
        this.listener = listener
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w * 0.85f
        centerY = h * 0.85f
        controlRadius = minOf(w, h) * 0.08f
        buttonRadius = controlRadius * 0.3f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (ptzController == null) return
        
        // Draw main control circle
        canvas.drawCircle(centerX, centerY, controlRadius, paint)
        canvas.drawCircle(centerX, centerY, controlRadius, strokePaint)
        
        // Draw directional buttons
        drawDirectionalButton(canvas, centerX, centerY - controlRadius * 0.6f, "↑")
        drawDirectionalButton(canvas, centerX, centerY + controlRadius * 0.6f, "↓")
        drawDirectionalButton(canvas, centerX - controlRadius * 0.6f, centerY, "←")
        drawDirectionalButton(canvas, centerX + controlRadius * 0.6f, centerY, "→")
        
        // Draw zoom controls
        drawZoomControls(canvas)
        
        // Auto-hide after delay
        if (System.currentTimeMillis() - lastHideTime > hideDelay) {
            visibility = GONE
        }
    }

    private fun drawDirectionalButton(canvas: Canvas, x: Float, y: Float, symbol: String) {
        canvas.drawCircle(x, y, buttonRadius, paint)
        canvas.drawCircle(x, y, buttonRadius, strokePaint)
        
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = buttonRadius
            textAlign = Paint.Align.CENTER
        }
        
        canvas.drawText(symbol, x, y + buttonRadius * 0.3f, textPaint)
    }

    private fun drawZoomControls(canvas: Canvas) {
        val zoomY = centerY + controlRadius * 1.5f
        
        // Zoom in
        drawDirectionalButton(canvas, centerX - controlRadius * 0.4f, zoomY, "+")
        
        // Zoom out  
        drawDirectionalButton(canvas, centerX + controlRadius * 0.4f, zoomY, "-")
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        lastHideTime = System.currentTimeMillis()
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val direction = getTouchedDirection(event.x, event.y)
                val zoom = getTouchedZoom(event.x, event.y)
                
                when {
                    direction != null -> {
                        listener?.onPTZCommand(direction, true)
                        startContinuousMove(direction)
                    }
                    zoom != null -> {
                        listener?.onZoomCommand(zoom)
                        startZoom(zoom)
                    }
                }
                return true
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                stopAllMovement()
                return true
            }
        }
        
        return super.onTouchEvent(event)
    }

    private fun getTouchedDirection(x: Float, y: Float): PTZDirection? {
        val dx = x - centerX
        val dy = y - centerY
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)
        
        if (distance > controlRadius) return null
        
        return when {
            kotlin.math.abs(dy) > kotlin.math.abs(dx) -> {
                if (dy < 0) PTZDirection.UP else PTZDirection.DOWN
            }
            else -> {
                if (dx < 0) PTZDirection.LEFT else PTZDirection.RIGHT
            }
        }
    }

    private fun getTouchedZoom(x: Float, y: Float): Float? {
        val zoomY = centerY + controlRadius * 1.5f
        val distance = kotlin.math.sqrt((x - centerX) * (x - centerX) + (y - zoomY) * (y - zoomY))
        
        if (distance > buttonRadius) return null
        
        return when {
            x < centerX -> 0.5f  // Zoom in
            x > centerX -> -0.5f // Zoom out
            else -> null
        }
    }

    private fun startContinuousMove(direction: PTZDirection) {
        controlJob?.cancel()
        controlJob = scope.launch {
            ptzController?.continuousMove(direction, 0.5f)
        }
    }

    private fun startZoom(factor: Float) {
        controlJob?.cancel()
        controlJob = scope.launch {
            val direction = if (factor > 0) ZoomDirection.IN else ZoomDirection.OUT
            ptzController?.zoom(direction, kotlin.math.abs(factor))
            delay(100)
            ptzController?.stop()
        }
    }

    private fun stopAllMovement() {
        controlJob?.cancel()
        controlJob = scope.launch {
            ptzController?.stop()
        }
    }

    fun show() {
        visibility = VISIBLE
        lastHideTime = System.currentTimeMillis()
        invalidate()
    }

    fun hide() {
        visibility = GONE
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scope.cancel()
    }
}
