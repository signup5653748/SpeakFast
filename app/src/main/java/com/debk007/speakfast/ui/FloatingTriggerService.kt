package com.debk007.speakfast.ui

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.debk007.speakfast.MainActivity
import com.debk007.speakfast.R
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class FloatingTriggerService : Service() {

    companion object {
        const val ACTION_READ = "com.debk007.speakfast.ACTION_READ"
        const val ACTION_READ_RECT = "com.debk007.speakfast.ACTION_READ_RECT"
        const val ACTION_STOP = "com.debk007.speakfast.ACTION_STOP"
        const val ACTION_PAUSE = "com.debk007.speakfast.ACTION_PAUSE"
        const val ACTION_RESUME = "com.debk007.speakfast.ACTION_RESUME"
        const val ACTION_REWIND = "com.debk007.speakfast.ACTION_REWIND"
        const val ACTION_FORWARD = "com.debk007.speakfast.ACTION_FORWARD"
    }

    private lateinit var wm: WindowManager
    private lateinit var bubbleParams: WindowManager.LayoutParams
    private lateinit var bubble: ImageView
    private var playerBar: LinearLayout? = null
    private var selectionView: SelectionView? = null
    private var playerVisible = false
    private var started = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        createBubble()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun createBubble() {
        bubbleParams = WindowManager.LayoutParams(
            dp(56), dp(56),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = dp(16); y = dp(220) }

        bubble = ImageView(this).apply {
            setImageResource(R.drawable.ic_floating_speak)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor("#4A6FE3")) }
        }

        var sx = 0; var sy = 0; var tx = 0f; var ty = 0f; var dragged = false
        bubble.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = bubbleParams.x; sy = bubbleParams.y; tx = e.rawX; ty = e.rawY; dragged = false; true }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - tx; val dy = e.rawY - ty
                    if (abs(dx) > 8 || abs(dy) > 8) dragged = true
                    if (dragged) {
                        bubbleParams.x = sx + dx.toInt(); bubbleParams.y = sy + dy.toInt()
                        wm.updateViewLayout(bubble, bubbleParams)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> { if (!dragged && !playerVisible) showPlayer(); true }
                else -> false
            }
        }
        wm.addView(bubble, bubbleParams)
    }

    private fun showPlayer() {
        playerVisible = true
        started = false
        bubble.visibility = View.GONE

        // FIX: Remove FLAG_NOT_FOCUSABLE so SelectionView receives touch events
        val overlayParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        val sv = SelectionView(this)
        selectionView = sv
        wm.addView(sv, overlayParams)

        val barParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL; y = dp(24) }

        val extraGroup = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            visibility = View.GONE
        }
        extraGroup.addView(barButton("⚙") { startActivity(Intent(this@FloatingTriggerService, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) })
        extraGroup.addView(barButton("⏮") { send(ACTION_REWIND) })
        extraGroup.addView(barButton("⏭") { send(ACTION_FORWARD) })

        val arrow = TextView(this).apply {
            text = "›"
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            val pad = dp(12)
            setPadding(pad, pad, pad, pad)
            setOnClickListener {
                val show = extraGroup.visibility == View.GONE
                extraGroup.visibility = if (show) View.VISIBLE else View.GONE
                text = if (show) "‹" else "›"
            }
        }

        val playPause = TextView(this).apply {
            text = "▶"
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            val pad = dp(12)
            setPadding(pad, pad, pad, pad)
            setOnClickListener {
                if (!started) { started = true; send(ACTION_READ); text = "⏸" }
                else if (text == "▶") { send(ACTION_RESUME); text = "⏸" }
                else { send(ACTION_PAUSE); text = "▶" }
            }
        }

        val stop = TextView(this).apply {
            text = "■"
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            val pad = dp(12)
            setPadding(pad, pad, pad, pad)
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor("#D93025")) }
            setOnClickListener {
                send(ACTION_STOP)
                started = false
                hidePlayer()
            }
        }

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#E6202124"))
            setPadding(dp(6), dp(4), dp(6), dp(4))
            addView(arrow)
            addView(extraGroup)
            addView(playPause)
            addView(stop)
        }
        playerBar = bar
        wm.addView(bar, barParams)
    }

    private fun hidePlayer() {
        playerVisible = false
        selectionView?.let { wm.removeView(it) }
        playerBar?.let { wm.removeView(it) }
        selectionView = null
        playerBar = null
        bubble.visibility = View.VISIBLE
    }

    private fun barButton(icon: String, onClick: () -> Unit): TextView = TextView(this).apply {
        text = icon
        textSize = 18f
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        val pad = dp(12)
        setPadding(pad, pad, pad, pad)
        setOnClickListener { onClick() }
    }

    private fun send(action: String) = sendBroadcast(Intent(action).setPackage(packageName))

    private fun sendRect(r: Rect) {
        sendBroadcast(Intent(ACTION_READ_RECT).setPackage(packageName)
            .putExtra("left", r.left).putExtra("top", r.top)
            .putExtra("right", r.right).putExtra("bottom", r.bottom))
    }

    private inner class SelectionView : View {
        private val fill = Paint().apply { color = Color.parseColor("#554A6FE3"); style = Paint.Style.FILL }
        private val stroke = Paint().apply { color = Color.parseColor("#34A853"); style = Paint.Style.STROKE; strokeWidth = 6f }
        private var rect: Rect? = null
        private var startX = 0f; private var startY = 0f
        private val screenOffset = IntArray(2)

        constructor(c: Context) : super(c)

        override fun onTouchEvent(e: MotionEvent): Boolean {
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = e.rawX
                    startY = e.rawY
                    rect = Rect()
                    invalidate()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val left = min(startX, e.rawX).toInt()
                    val top = min(startY, e.rawY).toInt()
                    val right = max(startX, e.rawX).toInt()
                    val bottom = max(startY, e.rawY).toInt()
                    rect = Rect(left, top, right, bottom)
                    invalidate()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val r = rect
                    rect = null
                    invalidate()
                    if (r != null && r.width() > 40 && r.height() > 40) {
                        // Convert to screen coordinates (SelectionView is full-screen, so rawX/rawY are already screen coords)
                        sendRect(r)
                    }
                    true
                }
            }
            return true
        }

        override fun onDraw(canvas: Canvas) {
            rect?.let { canvas.drawRect(it, fill); canvas.drawRect(it, stroke) }
        }
    }

    override fun onDestroy() {
        if (playerVisible) hidePlayer()
        if (::bubble.isInitialized) wm.removeView(bubble)
        super.onDestroy()
    }
}