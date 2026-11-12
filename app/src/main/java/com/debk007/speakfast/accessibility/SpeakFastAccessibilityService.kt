package com.debk007.speakfast.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.debk007.speakfast.R
import com.debk007.speakfast.di.AppModule
import com.debk007.speakfast.speech.SpeechManager
import com.debk007.speakfast.util.SelectionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SpeakFastAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var speechManager: SpeechManager

    @Inject
    lateinit var selectionManager: SelectionManager

    private val mainHandler = Handler(Looper.getMainLooper())
    private var selectionStartX = 0f
    private var selectionStartY = 0f
    private var selectionEndX = 0f
    private var selectionEndY = 0f
    private var isSelecting = false
    private var selectionTimeoutRunnable: Runnable? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            this.flags = AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_GESTURE
            this.eventTypes = AccessibilityEvent.TYPE_VIEW_FOCUSED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED or
                    AccessibilityEvent.TYPE_VIEW_SELECTED or
                    AccessibilityEvent.TYPE_TEXT_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            this.feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN
            this.notificationTimeout = 100
        }
        this.setServiceInfo(info)
        Log.d("SpeakFast", "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        when (event?.eventType) {
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                handleViewFocused(event)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                handleWindowContentChanged(event)
            }
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                handleViewClicked(event)
            }
            AccessibilityEvent.TYPE_VIEW_SELECTED -> {
                handleViewSelected(event)
            }
            AccessibilityEvent.TYPE_TEXT_CHANGED -> {
                handleTextChanged(event)
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                handleWindowStateChanged(event)
            }
        }
    }

    private fun handleViewFocused(event: AccessibilityEvent) {
        val node = event.source ?: return
        handleSelection(node)
    }

    private fun handleWindowContentChanged(event: AccessibilityEvent) {
        // Handle window content changes
        val root = getRootInActiveWindow() ?: return
        handleSelection(root)
    }

    private fun handleViewClicked(event: AccessibilityEvent) {
        val node = event.source ?: return
        handleSelection(node)
    }

    private fun handleViewSelected(event: AccessibilityEvent) {
        val node = event.source ?: return
        handleSelection(node)
    }

    private fun handleTextChanged(event: AccessibilityEvent) {
        // Handle text changes
        val node = event.source ?: return
        handleSelection(node)
    }

    private fun handleWindowStateChanged(event: AccessibilityEvent) {
        // Handle window state changes
        val root = getRootInActiveWindow() ?: return
        handleSelection(root)
    }

    private fun handleSelection(node: AccessibilityNodeInfo) {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        // Sample bounds multiple times for animation tolerance
        val boundsList = mutableListOf<Rect>()
        for (i in 0..2) {
            val sampleBounds = Rect()
            node.getBoundsInScreen(sampleBounds)
            boundsList.add(sampleBounds)
        }

        // Use the last sample for more accurate bounds
        val finalBounds = boundsList.last()

        // Check if this node is within our selection area
        if (isSelecting && isInSelectionArea(finalBounds)) {
            // Get text from node
            val text = node.text ?: node.contentDescription?.toString() ?: ""

            if (text.isNotEmpty()) {
                // Sort and process nodes
                processSelectionBounds(finalBounds)
                speakText(text)
            }
        }
    }

    private fun isInSelectionArea(bounds: Rect): Boolean {
        return bounds.left <= selectionEndX && bounds.right > selectionStartX &&
                bounds.top <= selectionEndY && bounds.bottom > selectionStartY
    }

    private fun processSelectionBounds(bounds: Rect) {
        // Get all nodes intersecting with selection
        val intersectingNodes = mutableListOf<AccessibilityNodeInfo>()
        val root = getRootInActiveWindow() ?: return

        findIntersectingNodes(root, bounds, intersectingNodes)

        // Sort nodes by screen bounds (top-to-bottom, left-to-right)
        val sortedNodes = sortNodesByBounds(intersectingNodes)

        // Extract text from all nodes
        val allText = mutableListOf<String>()
        for (node in sortedNodes) {
            val text = node.text ?: node.contentDescription?.toString() ?: ""
            if (text.isNotEmpty()) {
                allText.add(text)
            }
        }

        // Combine all text
        val combinedText = allText.joinToString(" ")
        if (combinedText.isNotEmpty()) {
            speakText(combinedText)
        }
    }

    private fun findIntersectingNodes(
        node: AccessibilityNodeInfo,
        selectionBounds: Rect,
        intersectingNodes: MutableList<AccessibilityNodeInfo>
    ) {
        if (node.boundsInScreen.intersectsWith(selectionBounds)) {
            intersectingNodes.add(node)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findIntersectingNodes(
                child,
                selectionBounds,
                intersectingNodes
            )
        }
    }

    private fun sortNodesByBounds(nodes: List<AccessibilityNodeInfo>): List<AccessibilityNodeInfo> {
        return nodes.sortedWith(compareBy(
            { it.boundsInScreen.top },
            { it.boundsInScreen.left }
        ))
    }

    private fun speakText(text: String) {
        speechManager.speakText(text)
    }

    override fun onDestroy() {
        super.onDestroy()
        selectionTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        Log.d("SpeakFast", "Accessibility service destroyed")
    }

    companion object {
        private const val TAG = "SpeakFast"
        private const val SELECTION_TIMEOUT_MS = 300L
    }
}