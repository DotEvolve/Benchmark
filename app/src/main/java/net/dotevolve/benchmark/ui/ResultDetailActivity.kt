package net.dotevolve.benchmark.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.IntentCompat
import net.dotevolve.benchmark.data.model.BenchmarkResult

class ResultDetailActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val result = IntentCompat.getSerializableExtra(intent, "benchmark_result", BenchmarkResult::class.java)

        setContent {
            MaterialTheme {
                ResultDetailScreen(
                    result = result,
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultDetailScreen(result: BenchmarkResult?, onBackClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Benchmark Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Text("<", modifier = Modifier.padding(16.dp))
                    }
                }
            )
        }
    ) { padding ->
        val details = result?.let { generateDetails(it) } ?: "No result data available"
        Text(
            text = details,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp
        )
    }
}

private fun generateDetails(result: BenchmarkResult): String {
    val details = java.lang.StringBuilder()
    details.append("=== BENCHMARK RESULT DETAILS ===\n\n")

    details.append("📅 TIMESTAMP\n")
    details.append("Date: ").append(result.formattedDate).append("\n")
    details.append("Time: ").append(result.formattedTime).append("\n\n")

    details.append("📱 DEVICE INFORMATION\n")
    details.append("Model: ").append(result.deviceModel).append("\n")
    details.append("Android: ").append(result.androidVersion).append("\n")
    details.append("CPU Cores: ").append(result.cpuCores).append("\n")
    details.append("Architecture: ").append(result.architecture).append("\n")
    details.append("Memory: ").append(result.formattedMemory).append("\n")
    details.append("Benchmark Engine: v").append(result.benchmarkVersion).append("\n\n")

    details.append("🎯 PERFORMANCE SCORES\n")
    details.append("Overall Score: ").append(result.overallScore).append("/100 (")
        .append(result.performanceCategory).append(")\n")
    details.append("Crypto Performance: ").append(result.cryptoScore).append("/100\n")
    details.append("Computational: ").append(result.computationalScore).append("/100\n")
    details.append("Memory: ").append(result.memoryScore).append("/100\n")
    details.append("Multi-threading: ").append(result.multiThreadingScore).append("/100\n")
    details.append("Efficiency: ").append(result.efficiencyScore).append("/100\n")
    details.append("Stability: ").append(result.stabilityScore).append("/100\n\n")

    details.append("⏱️ TIMING RESULTS\n")
    details.append("SHA-1 Time: ").append(formatNanoTime(result.sha1Time)).append("\n")
    details.append("MD5 Time: ").append(formatNanoTime(result.md5Time)).append("\n")
    details.append("AES Time: ").append(formatNanoTime(result.aesTime)).append("\n")
    details.append("RSA Time: ").append(formatNanoTime(result.rsaTime)).append("\n")
    details.append("Loop Time: ").append(formatNanoTime(result.loopTime)).append("\n")
    details.append("Matrix Multiplication: ").append(formatNanoTime(result.matrixTime)).append("\n")
    details.append("Sorting Time: ").append(formatNanoTime(result.sortTime)).append("\n")
    details.append("Compression Time: ").append(formatNanoTime(result.compressionTime)).append("\n")
    details.append("Memory Bandwidth Time: ").append(formatNanoTime(result.memoryBandwidthTime)).append("\n")
    details.append("Multi-threaded Time: ").append(formatNanoTime(result.multiThreadedTime)).append("\n\n")

    if (result.hasAdvancedMetrics()) {
        details.append("🔍 ADVANCED METRICS\n")
        details.append("CPU Temperature: ").append(result.formattedCpuTemperature).append("\n")
        details.append("Battery Level: ").append(result.formattedBatteryLevel).append("\n")
        details.append("Memory Usage: ").append(result.formattedMemoryUsage).append("\n")
        details.append("Thermal Throttling: ").append(if (result.isThermalThrottling) "YES" else "NO").append("\n")
        details.append("Background Apps: ").append(result.backgroundAppsCount).append("\n")
    }
    return details.toString()
}

private fun formatNanoTime(nanoTime: Long): String {
    return if (nanoTime < 1_000) {
        "$nanoTime ns"
    } else if (nanoTime < 1_000_000) {
        String.format(java.util.Locale.US, "%.2f μs", nanoTime / 1_000.0)
    } else if (nanoTime < 1_000_000_000) {
        String.format(java.util.Locale.US, "%.2f ms", nanoTime / 1_000_000.0)
    } else {
        String.format(java.util.Locale.US, "%.2f s", nanoTime / 1_000_000_000.0)
    }
}