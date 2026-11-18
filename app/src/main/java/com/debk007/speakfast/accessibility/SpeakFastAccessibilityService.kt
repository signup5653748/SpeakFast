package com.debk007.speakfast.accessibility

import android.accessibilityservice.AccessibilityService
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
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.debk007.speakfast.MainActivity
import com.debk007.speakfast.R
import com.debk007.speakfast.speech.SpeechManager
import com.debk007.speakfast.ui.FloatingTriggerService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@AndroidEntryPoint
class SpeakFastAccessibilityService : AccessibilityService() {

    @Inject lateinit var speechManager: SpeechManager

    private val wm: WindowManager by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }

    private var bubble: ImageView? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var playerBar: LinearLayout? = null
    private var selectionView: SelectionView? = null
    private var playerVisible = false
    private var started = false
    private var isPausedByUser = false
    private var playPauseButton: TextView? = null

    // ---------- Broadcast Receivers ----------

    private val readRectReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != FloatingTriggerService.ACTION_READ_RECT) return
            val left = intent.getIntExtra("left", 0)
            val top = intent.getIntExtra("top", 0)
            val right = intent.getIntExtra("right", 0)
            val bottom = intent.getIntExtra("bottom", 0)
            val selectionRect = Rect(left, top, right, bottom)

            Log.d("SpeakFast", "Reading selection: $selectionRect")
            val extractedText = extractTextInRect(selectionRect)
            if (extractedText.isBlank()) {
                speechManager.speakText("No text found at that location")
            } else {
                speechManager.speakScreen(extractedText)
            }
        }
    }

    private val readAllReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != FloatingTriggerService.ACTION_READ) return
            Log.d("SpeakFast", "Reading entire screen")
            val rootNode = rootInActiveWindow ?: return
            val extractedText = extractAllText(rootNode)
            if (extractedText.isBlank()) {
                speechManager.speakText("No text found on this screen")
            } else {
                speechManager.speakScreen(extractedText)
            }
        }
    }

    private val uiControlReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                FloatingTriggerService.ACTION_SHOW_BUBBLE -> createBubble()
                FloatingTriggerService.ACTION_HIDE_BUBBLE -> {
                    hidePlayer()
                    removeBubble()
                }
                FloatingTriggerService.ACTION_TTS_FINISHED -> {
                    if (!playerVisible) return
                    started = false
                    isPausedByUser = false
                    playPauseButton?.text = "▶"
                }
            }
        }
    }

    // ---------- Lifecycle ----------

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("SpeakFast", "Accessibility Service Connected")
        val filter = IntentFilter().apply {
            addAction(FloatingTriggerService.ACTION_READ_RECT)
            addAction(FloatingTriggerService.ACTION_READ)
            addAction(FloatingTriggerService.ACTION_SHOW_BUBBLE)
            addAction(FloatingTriggerService.ACTION_HIDE_BUBBLE)
            addAction(FloatingTriggerService.ACTION_TTS_FINISHED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(readRectReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            registerReceiver(readAllReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            registerReceiver(uiControlReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(readRectReceiver, filter)
            registerReceiver(readAllReceiver, filter)
            registerReceiver(uiControlReceiver, filter)
        }
        speechManager.initialize()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        hidePlayer()
        removeBubble()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        hidePlayer()
        removeBubble()
        try {
            unregisterReceiver(readRectReceiver)
            unregisterReceiver(readAllReceiver)
            unregisterReceiver(uiControlReceiver)
        } catch (e: Exception) {}
    }

    // ---------- Floating UI (TYPE_ACCESSIBILITY_OVERLAY = no system notification) ----------

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun createBubble() {
        if (bubble != null) return
        val params = WindowManager.LayoutParams(
            dp(56), dp(56),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = dp(16); y = dp(220) }
        bubbleParams = params

        val img = ImageView(this).apply {
            setImageResource(R.drawable.ic_floating_speak)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor("#4A6FE3")) }
        }

        var sx = 0; var sy = 0; var tx = 0f; var ty = 0f; var dragged = false
        img.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = params.x; sy = params.y; tx = e.rawX; ty = e.rawY; dragged = false; true }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - tx; val dy = e.rawY - ty
                    if (abs(dx) > 8 || abs(dy) > 8) dragged = true
                    if (dragged) {
                        params.x = sx + dx.toInt(); params.y = sy + dy.toInt()
                        wm.updateViewLayout(img, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> { if (!dragged && !playerVisible) showPlayer(); true }
                else -> false
            }
        }
        wm.addView(img, params)
        bubble = img
    }

    private fun removeBubble() {
        bubble?.let { runCatching { wm.removeView(it) } }
        bubble = null
        bubbleParams = null
    }

    private fun showPlayer() {
        if (playerVisible) return
        playerVisible = true
        started = false
        isPausedByUser = false
        bubble?.visibility = View.GONE

        // FLAG_NOT_FOCUSABLE keeps rootInActiveWindow pointing at the real app, not our overlay.
        val overlayParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        val sv = SelectionView(this)
        selectionView = sv
        wm.addView(sv, overlayParams)

        val barParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL; y = dp(24) }

        val extraGroup = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            visibility = View.GONE
        }
        extraGroup.addView(barButton("⚙", "Settings") {
            startActivity(Intent(this@SpeakFastAccessibilityService, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        })
        extraGroup.addView(barButton("⏮", "Rewind") { send(FloatingTriggerService.ACTION_REWIND) })
        extraGroup.addView(barButton("⏭", "Forward") { send(FloatingTriggerService.ACTION_FORWARD) })

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
                        send(FloatingTriggerService.ACTION_READ)
                        text = "⏸"
                    }
                    isPausedByUser -> {
                        isPausedByUser = false
                        send(FloatingTriggerService.ACTION_RESUME)
                        text = "⏸"
                    }
                    else -> {
                        isPausedByUser = true
                        send(FloatingTriggerService.ACTION_PAUSE)
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
                send(FloatingTriggerService.ACTION_STOP)
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
        if (!playerVisible) return
        playerVisible = false
        selectionView?.let { runCatching { wm.removeView(it) } }
        playerBar?.let { runCatching { wm.removeView(it) } }
        selectionView = null
        playerBar = null
        playPauseButton = null
        bubble?.visibility = View.VISIBLE
    }

    private fun barButton(icon: String, desc: String, onClick: () -> Unit): TextView = TextView(this).apply {
        text = icon
        contentDescription = desc
        textSize = 18f
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        val pad = dp(12)
        setPadding(pad, pad, pad, pad)
        setOnClickListener { onClick() }
    }

    private fun send(action: String) = sendBroadcast(Intent(action).setPackage(packageName))

    private fun sendRect(r: Rect) {
        sendBroadcast(Intent(FloatingTriggerService.ACTION_READ_RECT).setPackage(packageName)
            .putExtra("left", r.left).putExtra("top", r.top)
            .putExtra("right", r.right).putExtra("bottom", r.bottom))
    }

    // ---------- Text extraction ----------

    private fun extractTextInRect(rect: Rect): String {
        val rootNode = rootInActiveWindow ?: return ""
        val sb = StringBuilder()
        traverseNodesForRect(rootNode, rect, sb)
        return sb.toString().trim()
    }

    private fun traverseNodesForRect(node: AccessibilityNodeInfo, targetRect: Rect, sb: StringBuilder) {
        val nodeRect = Rect()
        node.getBoundsInScreen(nodeRect)

        if (Rect.intersects(nodeRect, targetRect)) {
            node.text?.let { if (it.isNotBlank()) sb.append(it).append(" ") }
            node.contentDescription?.let { if (it.isNotBlank()) sb.append(it).append(" ") }
        }

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                traverseNodesForRect(child, targetRect, sb)
                child.recycle()
            }
        }
    }

    private fun extractAllText(node: AccessibilityNodeInfo): String {
        val sb = StringBuilder()
        traverseAllNodes(node, sb)
        return sb.toString().trim()
    }

    private fun traverseAllNodes(node: AccessibilityNodeInfo, sb: StringBuilder) {
        node.text?.let { if (it.isNotBlank()) sb.append(it).append(" ") }
        node.contentDescription?.let { if (it.isNotBlank()) sb.append(it).append(" ") }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                traverseAllNodes(child, sb)
                child.recycle()
            }
        }
    }

    // ---------- Selection box ----------

    private inner class SelectionView(context: Context) : View(context) {
        private val fill = Paint().apply { color = Color.parseColor("#554A6FE3"); style = Paint.Style.FILL }
        private val stroke = Paint().apply { color = Color.parseColor("#34A853"); style = Paint.Style.STROKE; strokeWidth = 6f }
        private var rect: Rect? = null
        private var startX = 0f; private var startY = 0f

        override fun onTouchEvent(e: MotionEvent): Boolean {
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = e.rawX
                    startY = e.rawY
                    rect = Rect()
                    invalidate()
                }
                MotionEvent.ACTION_MOVE -> {
                    val left = min(startX, e.rawX).toInt()
                    val top = min(startY, e.rawY).toInt()
                    val right = max(startX, e.rawX).toInt()
                    val bottom = max(startY, e.rawY).toInt()
                    rect = Rect(left, top, right, bottom)
                    invalidate()
                }
                MotionEvent.ACTION_UP -> {
                    val r = rect
                    rect = null
                    invalidate()
                    if (r != null && r.width() > 40 && r.height() > 40) sendRect(r)
                }
            }
            return true
        }

        override fun onDraw(canvas: Canvas) {
            rect?.let { canvas.drawRect(it, fill); canvas.drawRect(it, stroke) }
        }
    }
}