package com.jarvis.aegis.learning

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

internal data class OcrResult(val text: String, val warning: String?)

internal class ImageOcrImporter(private val context: Context) {
    suspend fun recognize(uri: Uri): OcrResult {
        val image = InputImage.fromFilePath(context, uri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            val result = suspendCancellableCoroutine { continuation ->
                val task = recognizer.process(image)
                task.addOnSuccessListener { text ->
                    if (continuation.isActive) continuation.resume(text)
                }.addOnFailureListener { error ->
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
                continuation.invokeOnCancellation { recognizer.close() }
            }
            val extracted = result.text.trim()
            require(extracted.isNotBlank()) { "OCR found no readable text in the selected image." }
            val confidences = result.textBlocks.flatMap { it.lines }.flatMap { it.elements }
                .flatMap { it.symbols }.mapNotNull { it.confidence }
            val average = confidences.takeIf(List<Float>::isNotEmpty)?.average()
            val warning = when {
                average == null -> "OCR confidence is unavailable. Review the extracted text before saving."
                average < 0.65 -> "OCR confidence is low (${(average * 100).toInt()}%). Correct extraction mistakes before studying."
                else -> null
            }
            OcrResult(extracted, warning)
        } finally {
            recognizer.close()
        }
    }
}
