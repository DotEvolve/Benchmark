package net.dotevolve.benchmark.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.dotevolve.benchmark.BuildConfig
import net.dotevolve.benchmark.core.DeviceIdentityProvider
import net.dotevolve.benchmark.core.PerformanceMetrics
import net.dotevolve.benchmark.data.model.BenchmarkResult
import net.dotevolve.benchmark.data.model.DeviceStatistics
import net.dotevolve.benchmark.data.model.PerformanceTrend

class HistoryActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val deviceModel = Build.MODEL

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val isDarkMode = if (prefs.contains("dark_mode")) {
            prefs.getBoolean("dark_mode", false)
        } else {
            val nightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
            nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
        }

        setContent {
            BenchmarkTheme(darkTheme = isDarkMode) {
                HistoryScreen(
                    deviceModel = deviceModel,
                    onBackClick = { finish() },
                    onResultClick = { result ->
                        val intent = Intent(this@HistoryActivity, ResultDetailActivity::class.java).apply {
                            putExtra("benchmark_result", result)
                        }
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    deviceModel: String,
    onBackClick: () -> Unit,
    onResultClick: (BenchmarkResult) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var deviceStats by remember { mutableStateOf<DeviceStatistics?>(null) }
    var trends by remember { mutableStateOf<List<PerformanceTrend>?>(null) }
    var results by remember { mutableStateOf<List<BenchmarkResult>>(emptyList()) }
    var selectedDays by remember { mutableIntStateOf(7) }

    LaunchedEffect(deviceModel) {
        coroutineScope.launch {
            val stats = withContext(Dispatchers.IO) {
                try { PerformanceMetrics.getDeviceStatistics(context, deviceModel) } catch (e: Exception) { null }
            }
            deviceStats = stats

            val trendList = withContext(Dispatchers.IO) {
                try { PerformanceMetrics.getPerformanceTrends(context, deviceModel, 30) } catch (e: Exception) { null }
            }
            trends = trendList
        }
    }

    LaunchedEffect(selectedDays, deviceModel) {
        coroutineScope.launch {
            val res = withContext(Dispatchers.IO) {
                try {
                    if (selectedDays == -1) {
                        PerformanceMetrics.getHistoricalResults(context, deviceModel)
                    } else {
                        PerformanceMetrics.getRecentResults(context, deviceModel, selectedDays)
                    }
                } catch (e: Exception) {
                    emptyList<BenchmarkResult>()
                }
            }
            results = res ?: emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Performance History") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val clientDeviceId = remember(context) { DeviceIdentityProvider.getOrCreateClientDeviceId(context) }

            // Online Dashboard & Device ID
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🌐 Online Dashboard", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "View your uploaded runs and compare rankings on ${BuildConfig.BENCHMARK_API_BASE_URL.removePrefix("https://")}.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Device ID: $clientDeviceId",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { WebLinks.openUrl(context, WebLinks.buildMyRunsUrl(clientDeviceId)) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("View My Runs Online")
                        }
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Device ID", clientDeviceId))
                                Toast.makeText(context, "Device ID copied", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Copy ID")
                        }
                    }
                }
            }

            // Device Stats
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📊 Device Statistics", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(deviceStats?.summary ?: "Loading...", fontSize = 14.sp)
                }
            }

            // Trends
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📈 Performance Trends", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (trends.isNullOrEmpty()) {
                        Text("No trend data available. Run more benchmarks!", fontSize = 14.sp)
                    } else {
                        val trendsText = trends!!.joinToString("\n") { trend ->
                            "${trend.trendIcon} ${trend.trendDate}: ${trend.formattedAverageScore}/100 (${trend.performanceTrend})"
                        }
                        Text(trendsText, fontSize = 14.sp)
                    }
                }
            }

            // Recent Results
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📋 Recent Results", fontSize = 18.sp, fontWeight = FontWeight.Bold)

                        var expanded by remember { mutableStateOf(false) }
                        val options = listOf("Last 7 Days" to 7, "Last 30 Days" to 30, "Last 90 Days" to 90, "All Time" to -1)
                        Box {
                            TextButton(onClick = { expanded = true }) {
                                Text(options.find { it.second == selectedDays }?.first ?: "Last 7 Days")
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                options.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.first) },
                                        onClick = {
                                            selectedDays = option.second
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Column {
                        results.forEach { result ->
                            Column(modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onResultClick(result) }
                                .padding(vertical = 8.dp)
                            ) {
                                Text("Score: ${result.overallScore}", fontWeight = FontWeight.Bold)
                                Text("Date: ${result.timestamp}", fontSize = 12.sp)
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }

            // Analysis
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🔍 Performance Analysis", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(generateAnalysis(results), fontSize = 14.sp)
                }
            }
        }
    }
}

private fun generateAnalysis(results: List<BenchmarkResult>): String {
    if (results.isEmpty()) return "No data available for analysis"

    val totalResults = results.size
    var totalScore = 0
    var bestScore = 0
    var worstScore = 100
    var excellentCount = 0
    var goodCount = 0
    var averageCount = 0
    var belowAverageCount = 0
    var poorCount = 0

    for (result in results) {
        val score = result.overallScore
        totalScore += score
        bestScore = maxOf(bestScore, score)
        worstScore = minOf(worstScore, score)

        when (result.performanceCategory) {
            "EXCELLENT" -> excellentCount++
            "GOOD" -> goodCount++
            "AVERAGE" -> averageCount++
            "BELOW_AVERAGE" -> belowAverageCount++
            "POOR" -> poorCount++
        }
    }

    val averageScore = totalScore.toDouble() / totalResults
    val scoreRange = bestScore - worstScore

    val analysis = StringBuilder()
    analysis.append("📊 PERFORMANCE ANALYSIS\n\n")
    analysis.append("Total Tests: ").append(totalResults).append("\n")
    analysis.append("Average Score: ").append(String.format(java.util.Locale.US, "%.1f", averageScore)).append("/100\n")
    analysis.append("Best Score: ").append(bestScore).append("/100\n")
    analysis.append("Worst Score: ").append(worstScore).append("/100\n")
    analysis.append("Score Range: ").append(scoreRange).append(" points\n\n")

    analysis.append("📈 PERFORMANCE DISTRIBUTION\n")
    analysis.append("Excellent (90+): ").append(excellentCount).append(" (").append(String.format(java.util.Locale.US, "%.1f", excellentCount.toDouble() / totalResults * 100)).append("%)\n")
    analysis.append("Good (70-89): ").append(goodCount).append(" (").append(String.format(java.util.Locale.US, "%.1f", goodCount.toDouble() / totalResults * 100)).append("%)\n")
    analysis.append("Average (50-69): ").append(averageCount).append(" (").append(String.format(java.util.Locale.US, "%.1f", averageCount.toDouble() / totalResults * 100)).append("%)\n")
    analysis.append("Below Average (30-49): ").append(belowAverageCount).append(" (").append(String.format(java.util.Locale.US, "%.1f", belowAverageCount.toDouble() / totalResults * 100)).append("%)\n")
    analysis.append("Poor (0-29): ").append(poorCount).append(" (").append(String.format(java.util.Locale.US, "%.1f", poorCount.toDouble() / totalResults * 100)).append("%)\n\n")

    if (scoreRange <= 10) {
        analysis.append("🎯 Performance is very consistent!")
    } else if (scoreRange <= 20) {
        analysis.append("✅ Performance is generally consistent")
    } else if (scoreRange <= 30) {
        analysis.append("⚠️ Performance shows moderate variation")
    } else {
        analysis.append("🔍 Performance shows significant variation - investigate potential causes")
    }

    return analysis.toString()
}