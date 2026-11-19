package com.debk007.speakfast.ui

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Build
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
import com.debk007.speakfast.data.SettingsManager
import com.debk007.speakfast.data.dataStore
import kotlinx.coroutines.runBlocking
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
        const val ACTION_TTS_FINISHED = "com.debk007.speakfast.ACTION_TTS_FINISHED"
        const val ACTION_UPDATE_BUBBLE_SIZE = "com.debk007.speakfast.ACTION_UPDATE_BUBBLE_SIZE"
    }

    private lateinit var wm: WindowManager
    private lateinit var bubbleParams: WindowManager.LayoutParams
    private lateinit var bubble: ImageView
    private var playerBar: LinearLayout? = null
    private var selectionView: SelectionView? = null
    private var playerVisible = false
    private var started = false
    private var isPausedByUser = false
    private var playPauseButton: TextView? = null

    /**
     * FIX (stale play/pause state): receives ACTION_TTS_FINISHED from
     * SpeakFastAccessibilityService so the play/pause button resets to "▶"
     * when speech ends on its own (natural end of the last sentence, OCR
     * result finishing, or an explicit stop).
     */
    private val ttsFinishedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != ACTION_TTS_FINISHED || !playerVisible) return
            started = false
            isPausedByUser = false
            playPauseButton?.text = "▶"
            hidePlayerAndShowBubble()
        }
    }

    // Receiver for bubble size updates
    private val bubbleSizeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_UPDATE_BUBBLE_SIZE) {
                val size = intent.getFloatExtra("size", 1.0f)
                updateBubbleSize(size)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        createBubble()
        
        // Register TTS finished receiver
        val ttsFilter = IntentFilter(ACTION_TTS_FINISHED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(ttsFinishedReceiver, ttsFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(ttsFinishedReceiver, ttsFilter)
        }
        
        // Register bubble size update receiver
        val sizeFilter = IntentFilter(ACTION_UPDATE_BUBBLE_SIZE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(bubbleSizeReceiver, sizeFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(bubbleSizeReceiver, sizeFilter)
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun getFloatingButtonSize(): Float {
        return runBlocking {
            context.dataStore.data.first()[SettingsManager.FLOATING_BUTTON_SIZE_KEY] ?: 1.0f
        }
    }

    private fun createBubble() {
        val savedSize = getFloatingButtonSize()
        val multiplier = savedSize.coerceIn(0.5f, 2.0f)
        val sizePx = (dp(56) * multiplier).toInt().coerceAtLeast(dp(48))

        bubbleParams = WindowManager.LayoutParams(
            sizePx, sizePx,
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

    private fun updateBubbleSize(size: Float) {
        if (!::bubble.isInitialized) return
        val multiplier = size.coerceIn(0.5f, 2.0f)
        val newSize = (dp(56) * multiplier).toInt().coerceAtLeast(dp(48))
        bubbleParams.width = newSize
        bubbleParams.height = newSize
        wm.updateViewLayout(bubble, bubbleParams)
    }

    private fun showPlayer() {
        playerVisible = true
        started = false
        isPausedByUser = false
        bubble.visibility = View.GONE

        // FIX (BUG 1): SelectionView must NOT steal accessibility focus.
        // FLAG_NOT_FOCUSABLE prevents it from becoming the "active window"
        // so rootInActiveWindow returns the underlying app instead of this overlay.
        val overlayParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
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
        // FIX (accessibility): icon-only buttons now carry contentDescription
        extraGroup.addView(barButton("⚙", "Settings") {
            startActivity(Intent(this@FloatingTriggerService, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        })
        extraGroup.addView(barButton("⏮", "Rewind") { send(ACTION_REWIND) })
        extraGroup.addView(barButton("⏭", "Forward") { send(ACTION_FORWARD) })

        val arrow = TextView(this).apply {
            text = "›"
            contentDescription = "Toggle extra controls"
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
            contentDescription = "Play or Pause"
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            val pad = dp(12)
            setPadding(pad, pad, pad, pad)
            setOnClickListener {
                when {
                    !started -> {
                        started = true
                        isPausedByUser = false
                        send(ACTION_READ)
                        text = "⏸"
                    }
                    isPausedByUser -> {
                        isPausedByUser = false
                        send(ACTION_RESUME)
                        text = "⏸"
                    }
                    else -> {
                        isPausedByUser = true
                        send(ACTION_PAUSE)
                        text = "▶"
                    }
                }
            }
        }
        playPauseButton = playPause

        val stop = TextView(this).apply {
            text = "■"
            contentDescription = "Stop"
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            val pad = dp(12)
            setPadding(pad, pad, pad, pad)
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor("#D93025")) }
            setOnClickListener {
                send(ACTION_STOP)
                started = false
                isPausedByUser = false
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
        playPauseButton = null
        bubble.visibility = View.VISIBLE
    }

    private fun hidePlayerAndShowBubble() {
        playerVisible = false
        selectionView?.let { wm.removeView(it) }
        selectionView = null
        playerBar?.let { wm.removeView(it) }
        playerBar = null
        playPauseButton = null
        bubble.visibility = View.VISIBLE
    }

    private fun barButton(icon: String, contentDescription: String, onClick: () -> Unit): TextView = TextView(this).apply {
        text = icon
        this.contentDescription = contentDescription
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
        try { unregisterReceiver(ttsFinishedReceiver) } catch (e: Exception) {}
        try { unregisterReceiver(bubbleSizeReceiver) } catch (e: Exception) {}
        if (playerVisible) hidePlayer()
        if (::bubble.isInitialized) wm.removeView(bubble)
        super.onDestroy()
    }
}