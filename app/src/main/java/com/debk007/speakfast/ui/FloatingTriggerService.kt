package com.debk007.speakfast.ui

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Thin controller service. It no longer draws any overlay itself.
 * It simply tells SpeakFastAccessibilityService to show/hide the floating
 * bubble using TYPE_ACCESSIBILITY_OVERLAY, which does NOT trigger the
 * "displaying over other apps" system notification.
 */
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
        const val ACTION_SHOW_BUBBLE = "com.debk007.speakfast.ACTION_SHOW_BUBBLE"
        const val ACTION_HIDE_BUBBLE = "com.debk007.speakfast.ACTION_HIDE_BUBBLE"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        sendBroadcast(Intent(ACTION_SHOW_BUBBLE).setPackage(packageName))
        return START_STICKY
    }

    override fun onDestroy() {
        sendBroadcast(Intent(ACTION_HIDE_BUBBLE).setPackage(packageName))
        super.onDestroy()
    }
}