package org.example.project.operatinghand

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.project.model.HeuristicOperatingHandClassifier
import org.example.project.model.OperatingHand
import org.example.project.model.OperatingHandClassifier
import org.example.project.model.OperatingHandPrediction
import org.example.project.model.OperatingHandTensorSize
import org.example.project.model.OperatingHandTouchPoint
import org.example.project.model.resampleForOperatingHand
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AndroidOperatingHandClassifier(
    context: Context,
    private val assetName: String = MODEL_FILE
) : OperatingHandClassifier {
    private val appContext = context.applicationContext
    private val lock = Any()
    private var interpreter: Interpreter? = null
    private var modelMissing = false
    private var sampleCount = 9
    private var tensorSize = OperatingHandTensorSize
    private var outputSize = 1

    override suspend fun classify(
        points: List<OperatingHandTouchPoint>
    ): OperatingHandPrediction = withContext(Dispatchers.Default) {
        val activeInterpreter = ensureInterpreter()
            ?: return@withContext HeuristicOperatingHandClassifier.classify(points)

        runCatching {
            val sampledPoints = points.resampleForOperatingHand(sampleCount)
            val input = sampledPoints.toModelInput()
            val output = Array(1) { FloatArray(outputSize) }

            activeInterpreter.run(input, output)

            val rightScore = if (outputSize >= 2) output[0][1] else output[0][0]
            val boundedRightScore = rightScore.coerceIn(0f, 1f)
            if (boundedRightScore > 0.5f) {
                OperatingHandPrediction(OperatingHand.RIGHT, boundedRightScore)
            } else {
                OperatingHandPrediction(OperatingHand.LEFT, 1f - boundedRightScore)
            }
        }.getOrElse { throwable ->
            Log.w(TAG, "Operating hand inference failed, fallback to heuristic.", throwable)
            HeuristicOperatingHandClassifier.classify(points)
        }
    }

    override fun close() {
        synchronized(lock) {
            interpreter?.close()
            interpreter = null
        }
    }

    private fun ensureInterpreter(): Interpreter? = synchronized(lock) {
        if (modelMissing) return@synchronized null
        interpreter?.let { return@synchronized it }

        runCatching {
            val modelBuffer = appContext.assets.open(assetName).use { inputStream ->
                val bytes = inputStream.readBytes()
                ByteBuffer.allocateDirect(bytes.size)
                    .order(ByteOrder.nativeOrder())
                    .apply {
                        put(bytes)
                        rewind()
                    }
            }

            Interpreter(modelBuffer).also { loadedInterpreter ->
                val inputShape = loadedInterpreter.getInputTensor(0).shape()
                when (inputShape.size) {
                    2 -> {
                        tensorSize = OperatingHandTensorSize
                        sampleCount = (inputShape[1] / tensorSize).coerceAtLeast(1)
                    }
                    else -> {
                        if (inputShape.size >= 2) {
                            sampleCount = inputShape[1]
                        }
                        if (inputShape.size >= 3) {
                            tensorSize = inputShape[2]
                        }
                    }
                }
                outputSize = loadedInterpreter.getOutputTensor(0).shape().lastOrNull() ?: 1
                interpreter = loadedInterpreter
            }
        }.getOrElse { throwable ->
            modelMissing = true
            Log.w(TAG, "Operating hand model asset '$assetName' is unavailable.", throwable)
            null
        }
    }

    private fun List<OperatingHandTouchPoint>.toModelInput(): ByteBuffer {
        val relativeFeatures = toRelativeFeatures()
        val input = ByteBuffer
            .allocateDirect(relativeFeatures.size * tensorSize * FLOAT_BYTES)
            .order(ByteOrder.nativeOrder())

        relativeFeatures.forEach { values ->
            repeat(tensorSize) { index ->
                input.putFloat(values.getOrElse(index) { 0f })
            }
        }

        input.rewind()
        return input
    }

    private fun List<OperatingHandTouchPoint>.toRelativeFeatures(): List<FloatArray> {
        if (isEmpty()) return emptyList()

        val startX = first().x
        val startY = first().y
        val startTime = first().downTimeDeltaMillis
        val duration = (maxOf { it.downTimeDeltaMillis } - minOf { it.downTimeDeltaMillis })
            .coerceAtLeast(1f)

        return map { point ->
            val width = point.width.coerceAtLeast(1f)
            val height = point.height.coerceAtLeast(1f)
            floatArrayOf(
                point.x / width,
                point.y / height,
                (point.x - startX) / width,
                (point.y - startY) / height,
                (point.downTimeDeltaMillis - startTime) / duration,
                duration / 1000f
            )
        }
    }

    private companion object {
        const val TAG = "OperatingHand"
        const val MODEL_FILE = "operating_hand.tflite"
        const val FLOAT_BYTES = 4
    }
}
