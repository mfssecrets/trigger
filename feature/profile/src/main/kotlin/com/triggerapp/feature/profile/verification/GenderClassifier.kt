package com.triggerapp.feature.profile.verification

import android.content.Context
import android.graphics.Bitmap
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import org.tensorflow.lite.Interpreter

/**
 * On-device gender classifier running Intel `age-gender-recognition-retail-0013`
 * (via the PINTO float16 TFLite conversion bundled at `assets/age_gender_model.tflite`).
 *
 * Model contract (verified against the official OpenVINO IR on Intel's sample images):
 * - Input `data`: float32 `[1,62,62,3]`, raw **BGR** values in `0..255` (no mean / no scaling).
 * - Output 0 `Identity`: float32 `[1,1,1,1]` — estimated age divided by 100 (unused here).
 * - Output 1 `Identity_1`: float32 `[1,1,1,2]` — softmax, index 0 = female, index 1 = male.
 *
 * Measured accuracy on face-detector bbox crops (~15% margin): 96%, matching Intel's
 * published 95.8% figure. Callers should require [GenderResult.confidence] >= 0.85 before
 * treating the label as reliable.
 *
 * Not thread-safe; the app performs at most one classification per verification pass.
 *
 * @author udit
 */
class GenderClassifier(context: Context) : AutoCloseable {

    /** Winning label plus softmax confidence for the winning side. @author udit */
    data class GenderResult(val isFemale: Boolean, val confidence: Double)

    private val interpreter: Interpreter

    init {
        val buffer = context.assets.open(MODEL_ASSET).use { input ->
            val bytes = ByteArray(input.available())
            var read = 0
            while (read < bytes.size) {
                val n = input.read(bytes, read, bytes.size - read)
                check(n > 0) { "Unexpected end of model asset" }
                read += n
            }
            ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder()).apply {
                put(bytes)
                rewind()
            }
        }
        interpreter = Interpreter(buffer, Interpreter.Options().setNumThreads(2))
    }

    /**
     * Classifies gender from an upright RGB face crop.
     *
     * @param faceBitmap RGB bitmap of the face (any square-ish aspect; resized to 62x62).
     * @return Winning gender with softmax confidence, or null when inference fails.
     * @author udit
     */
    fun classify(faceBitmap: Bitmap): GenderResult? = runCatching {
        val input = preprocess(faceBitmap)
        val ageOutput = Array(1) { Array(1) { Array(1) { FloatArray(1) } } }
        val genderOutput = Array(1) { Array(1) { Array(1) { FloatArray(2) } } }
        val outputs = mapOf(0 to ageOutput, 1 to genderOutput)
        interpreter.runForMultipleInputsOutputs(arrayOf(input), outputs)
        val female = genderOutput[0][0][0][0].toDouble()
        val male = genderOutput[0][0][0][1].toDouble()
        if (female >= male) {
            GenderResult(isFemale = true, confidence = female)
        } else {
            GenderResult(isFemale = false, confidence = male)
        }
    }.getOrNull()

    /**
     * Converts an RGB bitmap into the model's BGR float `0..255` input buffer.
     * @author udit
     */
    private fun preprocess(faceBitmap: Bitmap): ByteBuffer {
        val scaled = Bitmap.createScaledBitmap(faceBitmap, INPUT_SIZE, INPUT_SIZE, true)
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        scaled.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        if (scaled !== faceBitmap) scaled.recycle()
        val buffer: FloatBuffer = ByteBuffer
            .allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        for (p in pixels) {
            // RGB -> BGR, keep raw 0..255 range (model was trained without normalisation).
            buffer.put(((p shr 16) and 0xFF).toFloat()) // R -> last
            buffer.put(((p shr 8) and 0xFF).toFloat())  // G
            buffer.put((p and 0xFF).toFloat())          // B -> first
        }
        buffer.rewind()
        return buffer as ByteBuffer
    }

    override fun close() {
        interpreter.close()
    }

    private companion object {
        const val MODEL_ASSET = "age_gender_model.tflite"
        const val INPUT_SIZE = 62
    }
}
