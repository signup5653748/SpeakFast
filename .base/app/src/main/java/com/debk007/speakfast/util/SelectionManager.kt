package com.debk007.speakfast.util

import android.graphics.Rect

class SelectionManager {

    private var selectionStartX = 0f
    private var selectionStartY = 0f
    private var selectionEndX = 0f
    private var selectionEndY = 0f
    private var isSelecting = false

    fun startSelection(startX: Float, startY: Float) {
        selectionStartX = startX
        selectionStartY = startY
        selectionEndX = startX
        selectionEndY = startY
        isSelecting = true
    }

    fun updateSelection(endX: Float, endY: Float) {
        selectionEndX = endX
        selectionEndY = endY
    }

    fun endSelection(): Rect {
        isSelecting = false
        val left = selectionStartX.coerceAtLeast(selectionEndX)
        val right = selectionStartX.coerceAtMost(selectionEndX)
        val top = selectionStartY.coerceAtLeast(selectionEndY)
        val bottom = selectionStartY.coerceAtMost(selectionEndY)

        return Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
    }

    fun isSelecting(): Boolean {
        return isSelecting
    }

    fun getSelectionBounds(): Rect {
        val left = selectionStartX.coerceAtLeast(selectionEndX)
        val right = selectionStartX.coerceAtMost(selectionEndX)
        val top = selectionStartY.coerceAtLeast(selectionEndY)
        val bottom = selectionStartY.coerceAtMost(selectionEndY)

        return Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
    }
}