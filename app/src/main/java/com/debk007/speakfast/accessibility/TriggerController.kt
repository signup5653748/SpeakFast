package com.debk007.speakfast.accessibility

import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityNodeInfo

object TriggerController {

    fun findStableNodeAt(
        root: AccessibilityNodeInfo,
        x: Int,
        y: Int,
        onNodeFound: (AccessibilityNodeInfo?) -> Unit
    ) {
        val initialNode = findNodeAtCoordinates(root, x, y)
        
        if (initialNode == null) {
            onNodeFound(null)
            return
        }

        var stable = true
        val checks = 3
        val delayMs = 50L
        val handler = Handler(Looper.getMainLooper())
        var checkCount = 0

        val runnable = object : Runnable {
            override fun run() {
                checkCount++
                val currentNode = findNodeAtCoordinates(root, x, y)
                
                if (currentNode == null || !isNodeAtCoordinates(currentNode, x, y)) {
                    stable = false
                }

                if (checkCount < checks) {
                    handler.postDelayed(this, delayMs)
                } else {
                    if (stable) {
                        onNodeFound(initialNode)
                    } else {
                        onNodeFound(null)
                    }
                }
            }
        }

        handler.post(runnable)
    }

    private fun findNodeAtCoordinates(
        node: AccessibilityNodeInfo,
        x: Int,
        y: Int
    ): AccessibilityNodeInfo? {
        if (!isNodeAtCoordinates(node, x, y)) {
            return null
        }

        for (i in node.childCount downTo 0) {
            val child = node.getChild(i) ?: continue
            val result = findNodeAtCoordinates(child, x, y)
            if (result != null) {
                return result
            }
        }

        return node
    }

    private fun isNodeAtCoordinates(
        node: AccessibilityNodeInfo,
        x: Int,
        y: Int
    ): Boolean {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        return x >= rect.left && x < rect.right && y >= rect.top && y < rect.bottom
    }
}