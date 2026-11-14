package net.dotevolve.benchmark.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import net.dotevolve.benchmark.core.PerformanceMetrics

@Composable
fun PerformanceVisualizer(metrics: PerformanceMetrics?, modifier: Modifier = Modifier) {
    if (metrics == null) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Background
        drawRect(color = Color.LightGray, size = Size(width, height))

        // Title
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 24f
            isAntiAlias = true
        }
        drawContext.canvas.nativeCanvas.drawText("Performance Visualization", 20f, 40f, paint)

        // Draw score bars
        drawScoreBar("Overall", metrics.overallScore, 60f, width, Color.Blue, paint)
        drawScoreBar("Crypto", metrics.cryptoScore, 100f, width, Color.Green, paint)
        drawScoreBar("Efficiency", metrics.efficiencyScore, 140f, width, Color.Red, paint)
        drawScoreBar("Stability", metrics.stabilityScore, 180f, width, Color.Magenta, paint)

        // Draw device info
        paint.textSize = 16f
        drawContext.canvas.nativeCanvas.drawText("Device: ${metrics.deviceModel}", 20f, 220f, paint)
        drawContext.canvas.nativeCanvas.drawText("CPU Cores: ${metrics.cpuCores}", 20f, 240f, paint)

        // Draw performance category
        val category = getPerformanceCategory(metrics.overallScore)
        val scorePaint = android.graphics.Paint().apply {
            color = getCategoryColor(category).toArgb()
            textSize = 32f
            isAntiAlias = true
            isFakeBoldText = true
        }
        drawContext.canvas.nativeCanvas.drawText("Category: $category", 20f, 280f, scorePaint)
    }
}

private fun DrawScope.drawScoreBar(label: String, score: Int, y: Float, width: Float, color: Color, textPaint: android.graphics.Paint) {
    val barWidth = width - 40f
    val barHeight = 20f
    val x = 20f

    // Background bar
    drawRect(
        color = Color.LightGray,
        topLeft = Offset(x, y),
        size = Size(barWidth, barHeight)
    )

    // Progress bar
    val progressWidth = (score / 100f) * barWidth
    drawRect(
        color = color,
        topLeft = Offset(x, y),
        size = Size(progressWidth, barHeight)
    )

    textPaint.textSize = 16f
    drawContext.canvas.nativeCanvas.drawText("$label: $score", x, y - 5f, textPaint)
}

private fun getPerformanceCategory(score: Int): String {
    return when {
        score >= 90 -> "EXCELLENT"
        score >= 70 -> "GOOD"
        score >= 50 -> "AVERAGE"
        score >= 30 -> "BELOW_AVERAGE"
        else -> "POOR"
    }
}

private fun getCategoryColor(category: String): androidx.compose.ui.graphics.Color {
    return when (category) {
        "EXCELLENT" -> Color.Green
        "GOOD" -> Color.Blue
        "AVERAGE" -> Color.Yellow
        "BELOW_AVERAGE" -> Color(0xFFFF8C00) // Orange
        "POOR" -> Color.Red
        else -> Color.Gray
    }
}

fun androidx.compose.ui.graphics.Color.toArgb(): Int {
    return android.graphics.Color.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt()
    )
}
