package com.debk007.speakfast.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.debk007.speakfast.speech.SpeechManager
import com.debk007.speakfast.ui.FloatingTriggerService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SpeakFastAccessibilityService : AccessibilityService() {

    @Inject lateinit var speechManager: SpeechManager

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

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("SpeakFast", "Accessibility Service Connected")
        val filter = IntentFilter().apply {
            addAction(FloatingTriggerService.ACTION_READ_RECT)
            addAction(FloatingTriggerService.ACTION_READ)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(readRectReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            registerReceiver(readAllReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(readRectReceiver, filter)
            registerReceiver(readAllReceiver, filter)
        }
        speechManager.initialize()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(readRectReceiver)
            unregisterReceiver(readAllReceiver)
        } catch (e: Exception) {}
    }

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
            node.text?.let { 
                if (it.isNotBlank()) sb.append(it).append(" ") 
            }
            node.contentDescription?.let {
                if (it.isNotBlank()) sb.append(it).append(" ")
            }
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
}
