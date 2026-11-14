package com.debk007.speakfast.ui

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.debk007.speakfast.MainActivity
import kotlin.math.abs

class FloatingTriggerService : Service() {

    companion object {
        const val ACTION_READ = "com.debk007.speakfast.ACTION_READ"
        const val ACTION_STOP = "com.debk007.speakfast.ACTION_STOP"
    }

    private lateinit var windowManager: WindowManager
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var container: LinearLayout
    private lateinit var controlBar: LinearLayout
    private var barVisible = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        buildUi()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun buildUi() {
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(16); y = dp(200)
        }

        controlBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#E6202124"))
            visibility = View.GONE
        }
        controlBar.addView(barButton("⚙") {
            startActivity(Intent(this@FloatingTriggerService, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        })
        controlBar.addView(barButton("▶") { send(ACTION_READ) })
        controlBar.addView(barButton("■") { send(ACTION_STOP) })

        val button = TextView(this).apply {
            text = "🔊"
            textSize = 20f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            val size = dp(52)
            width = size; height = size
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#4A6FE3"))
            }
        }

        container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(controlBar)
            addView(button)
        }

        var startX = 0; var startY = 0; var touchX = 0f; var touchY = 0f; var dragged = false
        button.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x; startY = params.y
                    touchX = event.rawX; touchY = event.rawY
                    dragged = false; true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - touchX; val dy = event.rawY - touchY
                    if (abs(dx) > 8 || abs(dy) > 8) dragged = true
                    if (dragged) {
                        params.x = startX + dx.toInt()
                        params.y = startY + dy.toInt()
                        windowManager.updateViewLayout(container, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragged) toggleBar()
                    true
                }
                else -> false
            }
        }

        windowManager.addView(container, params)
    }

    private fun barButton(icon: String, onClick: () -> Unit): TextView = TextView(this).apply {
        text = icon
        textSize = 18f
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        val pad = dp(14)
        setPadding(pad, pad, pad, pad)
        setOnClickListener { onClick() }
    }

    private fun toggleBar() {
        barVisible = !barVisible
        controlBar.visibility = if (barVisible) View.VISIBLE else View.GONE
    }

    private fun send(action: String) {
        sendBroadcast(Intent(action).setPackage(packageName))
    }

    override fun onDestroy() {
        if (::container.isInitialized) windowManager.removeView(container)
        super.onDestroy()
    }
}