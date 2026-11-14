package com.debk007.speakfast.accessibility

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

object OcrFallback {

    interface OnTextRecognizedListener {
        fun onTextRecognized(text: String)
        fun onError(error: Exception)
    }

    fun recognizeText(bitmap: Bitmap, selectionRect: Rect, listener: OnTextRecognizedListener) {
        val left = selectionRect.left.coerceAtLeast(0)
        val top = selectionRect.top.coerceAtLeast(0)
        val width = selectionRect.width().coerceAtMost(bitmap.width - left)
        val height = selectionRect.height().coerceAtMost(bitmap.height - top)

        if (width <= 0 || height <= 0) {
            listener.onError(Exception("Invalid crop rect"))
            return
        }

        val croppedBitmap = Bitmap.createBitmap(bitmap, left, top, width, height)
        val recognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val inputImage = InputImage.fromBitmap(croppedBitmap, 0)

        recognizer.process(inputImage)
            .addOnSuccessListener { text ->
                val extractedText = text.textBlocks.joinToString(" ") { it.text }
                listener.onTextRecognized(extractedText)
                recognizer.close()
            }
            .addOnFailureListener { e ->
                listener.onError(e)
                recognizer.close()
            }
    }
}