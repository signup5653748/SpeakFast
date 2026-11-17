package com.debk007.speakfast.accessibility

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

object ScreenNodeReader {

    fun findIntersectingNodes(
        node: AccessibilityNodeInfo,
        selectionRect: Rect,
        resultList: MutableList<AccessibilityNodeInfo>
    ) {
        val nodeRect = Rect()
        node.getBoundsInScreen(nodeRect)

        if (Rect.intersects(nodeRect, selectionRect)) {
            val text = node.text
            if (text != null && text.toString().isNotBlank()) {
                resultList.add(node)
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                findIntersectingNodes(child, selectionRect, resultList)
            }
        }
    }
}