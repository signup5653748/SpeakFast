package com.debk007.speakfast.ui

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast

/**
 * Shows a single floating "Read" button over every app.
 *
 * The button is intentionally simple: no drag handling, no long-press menu.
 * Tapping it notifies [com.debk007.speakfast.accessibility.SpeakFastAccessibilityService]
 * through a package-scoped broadcast so the service can read the current selection.
 */
class FloatingTriggerService : Service() {

    private var windowManager: WindowManager? = null
    private var triggerView: View? = null

    override fun onCreate() {
        super.onCreate()

        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "SYSTEM_ALERT_WINDOW not granted - stopping floating trigger service")
            stopSelf()
            return
        }

        val manager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val button = Button(this).apply {
            text = TRIGGER_LABEL
            isAllCaps = false
            setOnClickListener { onTriggerClicked() }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = EDGE_MARGIN_PX
            y = EDGE_MARGIN_PX
        }

        triggerView = button
        windowManager = manager

        try {
            manager.addView(button, params)
            Log.d(TAG, "Floating trigger button added")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add floating trigger button", e)
            triggerView = null
            windowManager = null
            stopSelf()
        }
    }

    @Suppress("UnusedParameter") // signature fixed by Service; the system may restart us with a null intent
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    @Suppress("UnusedParameter") // binding is not supported for the floating trigger
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun onTriggerClicked() {
        Toast.makeText(this, TOAST_READING, Toast.LENGTH_SHORT).show()

        val readIntent = Intent(ACTION_READ).setPackage(packageName)
        sendBroadcast(readIntent)

        Log.d(TAG, "Read requested - broadcast $ACTION_READ sent")
    }

    override fun onDestroy() {
        val view = triggerView
        val manager = windowManager

        if (view != null && manager != null) {
            try {
                manager.removeView(view)
                Log.d(TAG, "Floating trigger button removed")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove floating trigger button", e)
            }
        }

        triggerView = null
        windowManager = null

        super.onDestroy()
    }

    companion object {
        private const val TAG = "SpeakFastFloatingTrigger"
        private const val TRIGGER_LABEL = "🎯 Read"
        private const val TOAST_READING = "Reading..."
        private const val EDGE_MARGIN_PX = 24

        /** Package-scoped action consumed by SpeakFastAccessibilityService. */
        const val ACTION_READ = "com.debk007.speakfast.action.READ_SELECTION"
    }
}