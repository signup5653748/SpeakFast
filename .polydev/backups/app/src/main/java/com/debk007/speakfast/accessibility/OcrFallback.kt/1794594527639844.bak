package com.debk007.speakfast.accessibility

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizerOptions

object OcrFallback {

    interface OnTextRecognizedListener {
        fun onTextRecognized(text: String)
        fun onError(error: Exception)
    }

    fun recognizeText(
        bitmap: Bitmap,
        rect: Rect,
        listener: OnTextRecognizedListener
    ) {
        val croppedBitmap = Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height())

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.processImage(croppedBitmap)
            .addOnSuccessListener { visionText ->
                val recognizedText = visionText.text
                listener.onTextRecognized(recognizedText)
            }
            .addOnFailureListener { exception ->
                listener.onError(exception)
            }
    }
}