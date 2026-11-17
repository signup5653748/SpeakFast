package com.debk007.speakfast.accessibility

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

object BoundsSorter {

    fun sortNodesForReading(nodes: List<AccessibilityNodeInfo>): List<AccessibilityNodeInfo> {
        return nodes.sortedWith(compareBy(
            { getNodeTop(it) },
            { getNodeLeft(it) }
        ))
    }

    private fun getNodeTop(node: AccessibilityNodeInfo): Int {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        return rect.top
    }

    private fun getNodeLeft(node: AccessibilityNodeInfo): Int {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        return rect.left
    }

    private fun getNodeBottom(node: AccessibilityNodeInfo): Int {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        return rect.bottom
    }

    private fun getNodeRight(node: AccessibilityNodeInfo): Int {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        return rect.right
    }
}