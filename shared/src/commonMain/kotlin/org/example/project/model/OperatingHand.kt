package org.example.project.model

enum class OperatingHand {
    LEFT,
    RIGHT,
    UNKNOWN
}

data class OperatingHandTouchPoint(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val density: Float,
    val downTimeDeltaMillis: Float
)

data class OperatingHandPrediction(
    val hand: OperatingHand,
    val score: Float
)

interface OperatingHandClassifier {
    suspend fun classify(points: List<OperatingHandTouchPoint>): OperatingHandPrediction

    fun close() = Unit
}

object NoOpOperatingHandClassifier : OperatingHandClassifier {
    override suspend fun classify(points: List<OperatingHandTouchPoint>): OperatingHandPrediction {
        return OperatingHandPrediction(OperatingHand.UNKNOWN, 0f)
    }
}

object HeuristicOperatingHandClassifier : OperatingHandClassifier {
    override suspend fun classify(points: List<OperatingHandTouchPoint>): OperatingHandPrediction {
        if (points.size < OperatingHandMinPointCount) {
            return OperatingHandPrediction(OperatingHand.UNKNOWN, 0f)
        }

        val sampledPoints = points.resampleForOperatingHand()
        val averageX = sampledPoints.map { it.x / it.width.coerceAtLeast(1f) }.average().toFloat()
        val hand = if (averageX < 0.5f) OperatingHand.LEFT else OperatingHand.RIGHT
        val score = (0.5f + kotlin.math.abs(averageX - 0.5f)).coerceIn(0.5f, 0.72f)

        return OperatingHandPrediction(hand, score)
    }
}

const val OperatingHandSampleCount = 9
const val OperatingHandTensorSize = 6
const val OperatingHandMinPointCount = 6

fun List<OperatingHandTouchPoint>.resampleForOperatingHand(
    sampleCount: Int = OperatingHandSampleCount
): List<OperatingHandTouchPoint> {
    if (isEmpty()) return emptyList()
    if (sampleCount <= 1) return listOf(first())

    val step = size.toFloat() / sampleCount

    return List(sampleCount) { index ->
        this[(index * step).toInt().coerceIn(0, lastIndex)]
    }
}
