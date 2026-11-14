package com.debk007.speakfast.accessibility

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
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

    interface ModelDownloadListener {
        fun onSuccess()
        fun onFailure(e: Exception)
    }

    fun ensureModelDownloaded(context: Context, listener: ModelDownloadListener) {
        // Create a 1x1 dummy bitmap to trigger model download
        val dummyBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        dummyBitmap.eraseColor(Color.TRANSPARENT)
        
        val recognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val inputImage = InputImage.fromBitmap(dummyBitmap, 0)

        recognizer.process(inputImage)
            .addOnSuccessListener { _ ->
                listener.onSuccess()
                recognizer.close()
                dummyBitmap.recycle()
            }
            .addOnFailureListener { e ->
                listener.onFailure(e)
                recognizer.close()
                dummyBitmap.recycle()
            }
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