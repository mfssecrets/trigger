package com.triggerapp.feature.profile.verification

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.ByteArrayOutputStream

/** Liveness challenge steps shown to the user while the camera is open. @author udit */
enum class LivenessPhase {
    /** Waiting for a single, large, centred face. */
    CENTER_FACE,

    /** Face acquired — user must blink once (a photo cannot blink). */
    BLINK,

    /** Blink observed — user must smile (confirms live expression control). */
    SMILE,
}

/**
 * Realtime liveness analyzer: ML Kit face detection drives a small state machine
 * (`CENTER_FACE -> BLINK -> SMILE`), then the winning frame's face is cropped and
 * emitted for on-device gender classification. Nothing leaves the device.
 *
 * Frame data stays in YUV until the capture moment to keep per-frame CPU cost low;
 * only the captured face crop is converted to an upright RGB [Bitmap].
 *
 * @param onPhaseChanged Liveness step transitions (main-thread not guaranteed; UI posts).
 * @param onFaceCaptured Upright RGB face crop (bbox + [FACE_MARGIN_FRACTION] margin).
 * @param onMultipleFaces Raised whenever more than one face is visible in a frame.
 * @author udit
 */
class FaceLivenessAnalyzer(
    private val onPhaseChanged: (LivenessPhase) -> Unit,
    private val onFaceCaptured: (Bitmap) -> Unit,
    private val onMultipleFaces: () -> Unit,
) : ImageAnalysis.Analyzer {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(MIN_FACE_SIZE)
            .build(),
    )

    @Volatile
    private var phase: LivenessPhase = LivenessPhase.CENTER_FACE

    // Eye-state tracking for blink detection (open -> closed -> open).
    private var eyesCurrentlyOpen = true
    private var blinkObserved = false

    // Smile needs 2 consecutive confident frames to avoid single-frame jitter.
    private var smileStreak = 0

    /**
     * Restarts the challenge from [LivenessPhase.CENTER_FACE] (used after a failed
     * classification or an explicit retry).
     * @author udit
     */
    fun reset() {
        phase = LivenessPhase.CENTER_FACE
        eyesCurrentlyOpen = true
        blinkObserved = false
        smileStreak = 0
        onPhaseChanged(phase)
    }

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val rotation = imageProxy.imageInfo.rotationDegrees
        val input = InputImage.fromMediaImage(mediaImage, rotation)
        detector.process(input)
            .addOnSuccessListener { faces ->
                handleFaces(faces, imageProxy, rotation)
            }
            .addOnFailureListener {
                // Leave the phase unchanged; the screen-level timeout reports the failure.
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    /**
     * Advances the state machine for one detector result; captures the frame when the
     * smile challenge completes.
     * @author udit
     */
    private fun handleFaces(faces: List<Face>, imageProxy: ImageProxy, rotation: Int) {
        if (faces.size > 1) {
            onMultipleFaces()
            return
        }
        val face = faces.firstOrNull() ?: return
        val bounds = face.boundingBox
        val uprightWidth = if (rotation == 90 || rotation == 270) imageProxy.height else imageProxy.width
        val uprightHeight = if (rotation == 90 || rotation == 270) imageProxy.width else imageProxy.height

        when (phase) {
            LivenessPhase.CENTER_FACE -> {
                val bigEnough = bounds.height() >= uprightHeight * MIN_FACE_HEIGHT_FRACTION
                val centerX = bounds.centerX().toFloat()
                val centred = centerX >= uprightWidth * 0.20f && centerX <= uprightWidth * 0.80f
                if (bigEnough && centred) {
                    transition(LivenessPhase.BLINK)
                }
            }

            LivenessPhase.BLINK -> {
                if (!faceIsBig(face, uprightHeight)) {
                    transition(LivenessPhase.CENTER_FACE)
                    return
                }
                val leftOpen = face.leftEyeOpenProbability ?: 1f
                val rightOpen = face.rightEyeOpenProbability ?: 1f
                val open = leftOpen > EYE_OPEN_THRESHOLD && rightOpen > EYE_OPEN_THRESHOLD
                val closed = leftOpen < EYE_CLOSED_THRESHOLD && rightOpen < EYE_CLOSED_THRESHOLD
                if (open && !eyesCurrentlyOpen) {
                    // Closed -> open transition: a blink completed.
                    eyesCurrentlyOpen = true
                    blinkObserved = true
                    transition(LivenessPhase.SMILE)
                } else if (closed && eyesCurrentlyOpen) {
                    eyesCurrentlyOpen = false
                }
            }

            LivenessPhase.SMILE -> {
                if (!faceIsBig(face, uprightHeight)) {
                    transition(LivenessPhase.CENTER_FACE)
                    return
                }
                val smiling = (face.smilingProbability ?: 0f) > SMILE_THRESHOLD
                if (smiling) {
                    smileStreak++
                    if (smileStreak >= SMILE_FRAMES_REQUIRED) {
                        captureFace(imageProxy, bounds, rotation)
                    }
                } else {
                    smileStreak = 0
                }
            }
        }
    }

    private fun faceIsBig(face: Face, uprightHeight: Int): Boolean =
        face.boundingBox.height() >= uprightHeight * MIN_FACE_HEIGHT_FRACTION

    private fun transition(next: LivenessPhase) {
        if (phase != next) {
            phase = next
            if (next != LivenessPhase.SMILE) smileStreak = 0
            onPhaseChanged(next)
        }
    }

    /**
     * Converts the current YUV frame into an upright RGB bitmap, crops the detector bbox
     * plus margin, and hands it to classification. Runs synchronously inside this frame's
     * callback so the buffer is still valid.
     * @author udit
     */
    private fun captureFace(imageProxy: ImageProxy, bounds: android.graphics.Rect, rotation: Int) {
        val upright = imageProxy.toUprightBitmap(rotation) ?: return
        val margin = (bounds.width().coerceAtLeast(bounds.height()) * FACE_MARGIN_FRACTION).toInt()
        val left = (bounds.left - margin).coerceAtLeast(0)
        val top = (bounds.top - margin).coerceAtLeast(0)
        val right = (bounds.right + margin).coerceAtMost(upright.width)
        val bottom = (bounds.bottom + margin).coerceAtMost(upright.height)
        if (right - left < 24 || bottom - top < 24) return
        val crop = Bitmap.createBitmap(upright, left, top, right - left, bottom - top)
        if (crop !== upright) {
            // Keep the crop; the full frame is no longer needed.
            upright.recycle()
        }
        onFaceCaptured(crop)
    }

    /** Phase visible to tests/screens without touching ML Kit internals. @author udit */
    fun currentPhase(): LivenessPhase = phase

    fun release() {
        detector.close()
    }

    private companion object {
        const val MIN_FACE_SIZE = 0.25f
        const val MIN_FACE_HEIGHT_FRACTION = 0.30f
        const val EYE_OPEN_THRESHOLD = 0.55f
        const val EYE_CLOSED_THRESHOLD = 0.30f
        const val SMILE_THRESHOLD = 0.75f
        const val SMILE_FRAMES_REQUIRED = 2
        const val FACE_MARGIN_FRACTION = 0.15f
    }
}

/**
 * Decodes the YUV_420_888 frame to a rotated (upright) RGB bitmap via NV21 + JPEG path.
 * Only used once per verification capture, so the JPEG round-trip cost is irrelevant.
 * @author udit
 */
private fun ImageProxy.toUprightBitmap(rotationDegrees: Int): Bitmap? = runCatching {
    val mediaImage = image ?: return null
    val width = mediaImage.width
    val height = mediaImage.height
    val ySize = width * height
    val nv21 = ByteArray(ySize + 2 * (ySize / 4))
    val yPlane = mediaImage.planes[0]
    val uPlane = mediaImage.planes[1]
    val vPlane = mediaImage.planes[2]

    var pos = 0
    val yBuf = yPlane.buffer
    val rowStrideY = yPlane.rowStride
    val pixelStrideY = yPlane.pixelStride
    if (rowStrideY == width && pixelStrideY == 1) {
        yBuf.get(nv21, 0, ySize)
        pos = ySize
    } else {
        for (row in 0 until height) {
            for (col in 0 until width) {
                nv21[pos++] = yBuf.get(row * rowStrideY + col * pixelStrideY)
            }
        }
    }

    // V and U interleaved -> NV21 expects VU order.
    val uBuf = uPlane.buffer
    val vBuf = vPlane.buffer
    val rowStrideU = uPlane.rowStride
    val rowStrideV = vPlane.rowStride
    val pixelStrideU = uPlane.pixelStride
    val pixelStrideV = vPlane.pixelStride
    val chromaHeight = height / 2
    val chromaWidth = width / 2
    for (row in 0 until chromaHeight) {
        for (col in 0 until chromaWidth) {
            nv21[pos++] = vBuf.get(row * rowStrideV + col * pixelStrideV)
            nv21[pos++] = uBuf.get(row * rowStrideU + col * pixelStrideU)
        }
    }

    val yuv = android.graphics.YuvImage(nv21, android.graphics.ImageFormat.NV21, width, height, null)
    val out = ByteArrayOutputStream()
    yuv.compressToJpeg(android.graphics.Rect(0, 0, width, height), 92, out)
    val decoded = BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size())
    if (rotationDegrees == 0) {
        decoded
    } else {
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (rotated !== decoded) decoded.recycle()
        rotated
    }
}.getOrNull()
