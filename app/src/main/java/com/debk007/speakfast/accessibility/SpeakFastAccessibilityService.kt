package com.debk007.speakfast.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.debk007.speakfast.data.SettingsManager
import com.debk007.speakfast.speech.SpeechManager
import com.debk007.speakfast.ui.FloatingTriggerService
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.Executor
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SpeakFastAccessibilityService : AccessibilityService() {

    @Inject lateinit var speechManager: SpeechManager
    @Inject lateinit var settingsManager: SettingsManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // Stores the screen Rect for each sentence being spoken (parallel to SpeechManager.sentences)
    private var sentenceRects: List<Rect> = emptyList()

    private val commandReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                FloatingTriggerService.ACTION_READ -> readWholeScreen()
                FloatingTriggerService.ACTION_READ_RECT -> {
                    val r = Rect(
                        intent.getIntExtra("left", 0), intent.getIntExtra("top", 0),
                        intent.getIntExtra("right", 0), intent.getIntExtra("bottom", 0)
                    )
                    readRect(r)
                }
                FloatingTriggerService.ACTION_PAUSE -> speechManager.pause()
                FloatingTriggerService.ACTION_RESUME -> speechManager.resume()
                FloatingTriggerService.ACTION_REWIND -> speechManager.rewind()
                FloatingTriggerService.ACTION_FORWARD -> speechManager.forward()
                FloatingTriggerService.ACTION_STOP -> speechManager.stop()
            }
        }
    }

    private val ocrListener = object : OcrFallback.OnTextRecognizedListener {
        override fun onTextRecognized(text: String) { if (text.isNotBlank()) speechManager.speakScreen(text) }
        override fun onError(error: Exception) { Log.e(TAG, "OCR failed", error) }
    }

    /**
     * FIX (stale play/pause state): notifies the floating player bar that
     * speech has ended, so its play/pause button resets to "▶".
     */
    private fun sendTtsFinished() {
        sendBroadcast(
            Intent(FloatingTriggerService.ACTION_TTS_FINISHED)
                .setPackage(packageName)
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            eventTypes = 0
            feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN
            notificationTimeout = 100
        }
        setServiceInfo(info)
        speechManager.initialize()
        // FIX (stale play/pause state): broadcast ACTION_TTS_FINISHED to the
        // floating player bar whenever speech stops, so its play/pause
        // button resets to "▶" instead of staying stale.
        serviceScope.launch {
            speechManager.speechFinished.collect { finished ->
                if (finished) sendTtsFinished()
            }
        }
        val filter = IntentFilter().apply {
            addAction(FloatingTriggerService.ACTION_READ)
            addAction(FloatingTriggerService.ACTION_READ_RECT)
            addAction(FloatingTriggerService.ACTION_PAUSE)
            addAction(FloatingTriggerService.ACTION_RESUME)
            addAction(FloatingTriggerService.ACTION_REWIND)
            addAction(FloatingTriggerService.ACTION_FORWARD)
            addAction(FloatingTriggerService.ACTION_STOP)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(commandReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(commandReceiver, filter)
        }
    }

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Ignore events from our own package to prevent reading the Settings screen
        if (event?.packageName?.toString() == packageName) return
    }

    private fun readWholeScreen() {
        val root = rootInActiveWindow ?: return
        val nodes = mutableListOf<AccessibilityNodeInfo>()
        try {
            ScreenNodeReader.findIntersectingNodes(root, Rect(0, 0, 10000, 10000), nodes)
            val sorted = BoundsSorter.sortNodesForReading(nodes)
            val texts = sorted.mapNotNull { it.text?.toString() ?: it.contentDescription?.toString() }.filter { it.isNotBlank() }
            if (texts.isEmpty()) triggerOcrFallback(screenRect())
            else {
                sentenceRects = sorted.map { node ->
                    val r = Rect()
                    node.getBoundsInScreen(r)
                    r
                }
                speechManager.speakScreen(texts.joinToString(" "))
            }
        } finally {
            nodes.forEach { it.recycle() }
            if (nodes.none { it === root }) root.recycle()
        }
    }

    private fun readRect(rect: Rect) {
        val root = rootInActiveWindow ?: return
        val precise = settingsManager.getSettings().readExactSelection
        val nodes = mutableListOf<AccessibilityNodeInfo>()
        try {
            ScreenNodeReader.findIntersectingNodes(root, rect, nodes)
            val sorted = BoundsSorter.sortNodesForReading(nodes)
            val texts = sorted.mapNotNull { node ->
                val t = PreciseTextExtractor.extract(node, rect, precise)
                t.ifBlank { null }
            }
            
            // FIX (Select-to-Speak): Log extracted text and handle empty case
            Log.d(TAG, "Found ${texts.size} text nodes in selection rect: $rect")
            if (texts.isEmpty()) {
                Log.w(TAG, "No text extracted from selection, triggering OCR fallback")
                triggerOcrFallback(rect)
            } else {
                val extractedText = texts.joinToString(" ")
                Log.d(TAG, "Extracted text: $extractedText")
                // Store rects for each sentence (one rect per node, since we speak joined text)
                sentenceRects = sorted.map { node ->
                    val r = Rect()
                    node.getBoundsInScreen(r)
                    r
                }
                speechManager.speakScreen(extractedText)
            }
        } finally {
            nodes.forEach { it.recycle() }
            if (nodes.none { it === root }) root.recycle()
        }
    }

    private fun screenRect(): Rect {
        val m = resources.displayMetrics
        return Rect(0, 0, m.widthPixels, m.heightPixels)
    }

    private fun triggerOcrFallback(rect: Rect) {
        if (!settingsManager.getSettings().ocrEnabled) {
            Log.w(TAG, "OCR mode is disabled by user, skipping OCR fallback")
            speechManager.speakText("No text found at that location. Enable OCR mode to read images.")
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val info = serviceInfo ?: return
        if ((info.capabilities and AccessibilityServiceInfo.CAPABILITY_CAN_TAKE_SCREENSHOT) == 0) return
        takeScreenshot(Display.DEFAULT_DISPLAY, Executor { mainHandler.post(it) }, object : TakeScreenshotCallback {
            override fun onSuccess(result: ScreenshotResult) {
                val hw = Bitmap.wrapHardwareBuffer(result.hardwareBuffer, result.colorSpace)
                result.hardwareBuffer.close()
                if (hw == null) return
                val sw = hw.copy(Bitmap.Config.ARGB_8888, false)
                hw.recycle()
                if (sw == null) return
                try { OcrFallback.recognizeText(sw, rect, ocrListener) } finally { sw.recycle() }
            }
            override fun onFailure(errorCode: Int) { Log.e(TAG, "Screenshot failed: $errorCode") }
        })
    }

    override fun onDestroy() {
        serviceScope.cancel()
        try { unregisterReceiver(commandReceiver) } catch (e: Exception) {}
        speechManager.shutdown()
        super.onDestroy()
    }

    companion object { private const val TAG = "SpeakFast" }
}