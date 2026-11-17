package com.debk007.speakfast.accessibility

import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

object PreciseTextExtractor {

    private const val TAG = "PreciseTextExtractor"
    private const val MAX_CHUNK = 500

    fun extract(node: AccessibilityNodeInfo, selection: Rect, precise: Boolean): String {
        val full = node.text?.toString() ?: return ""
        if (!precise || full.isEmpty()) return full

        charLocationSubstring(node, full, selection)?.let { if (it.isNotBlank()) return it }
        lineEstimateSubstring(node, full, selection)?.let { if (it.isNotBlank()) return it }
        return full
    }

    private fun charLocationSubstring(node: AccessibilityNodeInfo, full: String, selection: Rect): String? {
        return try {
            val rects = mutableListOf<RectF?>()
            var start = 0
            while (start < full.length) {
                val length = minOf(MAX_CHUNK, full.length - start)
                val args = Bundle().apply {
                    putInt(AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_START_INDEX, start)
                    putInt(AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_LENGTH, length)
                }
                val ok = node.refreshWithExtraData(
                    AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY, args
                )
                if (!ok) return null
                val extras = node.extras ?: return null
                @Suppress("DEPRECATION")
                val chunk = extras.getParcelableArray(AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY)
                if (chunk == null || chunk.isEmpty()) return null
                chunk.forEach { p -> rects.add(p as? RectF) }
                start += length
            }
            if (rects.isEmpty()) return null

            var minIdx = -1
            var maxIdx = -1
            for (i in rects.indices) {
                val rf = rects[i] ?: continue
                val r = Rect(rf.left.toInt(), rf.top.toInt(), rf.right.toInt() + 1, rf.bottom.toInt() + 1)
                if (Rect.intersects(r, selection)) {
                    if (minIdx == -1) minIdx = i
                    maxIdx = i
                }
            }
            if (minIdx == -1) return null
            val safeStart = minOf(minIdx, full.length)
            val safeEnd = minOf(maxIdx + 1, full.length)
            if (safeStart >= safeEnd) return null
            full.substring(safeStart, safeEnd).trim()
        } catch (e: Exception) {
            Log.w(TAG, "char-location failed: ${e.message}")
            null
        }
    }

    private fun lineEstimateSubstring(node: AccessibilityNodeInfo, full: String, selection: Rect): String? {
        val lines = full.split('\n')
        if (lines.size <= 1) return null
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (bounds.height() <= 0) return null
        val lineHeight = bounds.height().toFloat() / lines.size
        val startLine = ((selection.top - bounds.top) / lineHeight).toInt().coerceIn(0, lines.size - 1)
        val endLine = ((selection.bottom - bounds.top) / lineHeight).toInt().coerceIn(0, lines.size - 1)
        val lo = minOf(startLine, endLine)
        val hi = maxOf(startLine, endLine)
        return lines.subList(lo, hi + 1).joinToString("\n").trim()
    }
}
