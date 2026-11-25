package net.dotevolve.benchmark.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import net.dotevolve.benchmark.BuildConfig
import net.dotevolve.benchmark.R
import net.dotevolve.benchmark.ads.GoogleMobileAdsConsentManager
import net.dotevolve.benchmark.core.BenchmarkEngine
import net.dotevolve.benchmark.core.PerformanceMetrics
import net.dotevolve.benchmark.work.ScheduledBenchmarkWorker
import java.util.Collections
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var testDeviceHashedId: String

    private val backgroundExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
    private var mInterstitialAd: InterstitialAd? = null

    private lateinit var googleMobileAdsConsentManager: GoogleMobileAdsConsentManager
    private lateinit var benchmarkEngine: BenchmarkEngine
    private var currentMetrics by mutableStateOf<PerformanceMetrics?>(null)
    private var isBenchmarkRunning by mutableStateOf(false)
    private var benchmarkProgressText by mutableStateOf("Ready to benchmark")
    private var benchmarkScoreText by mutableStateOf("-")
    private var showDialog by mutableStateOf(false)

    private var bannerAdUnitId: String = ""
    private var interstitialAdUnitId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        testDeviceHashedId = resources.getString(R.string.test_device_hashed_id)
        bannerAdUnitId = getString(R.string.admob_banner_ad_unit_id)
        interstitialAdUnitId = getString(R.string.admob_interstitial_ad_unit_id)

        benchmarkEngine = BenchmarkEngine(this)
        setupBenchmarkCallbacks()

        setupAds()

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Starting AdInspector...")
            MobileAds.openAdInspector(this) { }
        }

        setContent {
            MaterialTheme {
                MainScreen(
                    progressText = benchmarkProgressText,
                    scoreText = benchmarkScoreText,
                    isRunning = isBenchmarkRunning,
                    onStartClick = { runEnhancedBenchmark() },
                    onShareClick = {
                        if (currentMetrics != null) {
                            showDialog = true
                        } else {
                            Toast.makeText(this, "Run benchmark first", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onHistoryClick = {
                        startActivity(Intent(this, HistoryActivity::class.java))
                    },
                    onToggleSchedule = { toggleSchedule() },
                    onToggleDarkMode = { toggleDarkMode() },
                    bannerAdUnitId = bannerAdUnitId
                )

                if (showDialog && currentMetrics != null) {
                    MetricsDialog(
                        metrics = currentMetrics!!,
                        onDismiss = { showDialog = false },
                        onExport = {
                            exportResults()
                            showDialog = false
                        },
                        onShare = {
                            shareResults()
                            showDialog = false
                        }
                    )
                }
            }
        }
    }

    private fun setupBenchmarkCallbacks() {
        benchmarkEngine.setProgressCallback(object : BenchmarkEngine.BenchmarkProgressCallback {
            override fun onProgressUpdate(progress: Int, currentTest: String) {
                runOnUiThread {
                    benchmarkProgressText = "Running \$currentTest...\nProgress: \$progress%"
                }
            }

            override fun onTestComplete(testName: String, duration: Long) {
                runOnUiThread {
                    benchmarkProgressText += "\n✓ \$testName completed in \${formatNanoTime(duration)}"
                }
            }

            override fun onBenchmarkComplete(metrics: PerformanceMetrics) {
                backgroundExecutor.execute { metrics.saveToHistory(this@MainActivity) }
                runOnUiThread {
                    isBenchmarkRunning = false
                    currentMetrics = metrics
                    benchmarkProgressText = metrics.formattedResults
                    benchmarkScoreText = metrics.overallScore.toString()
                    showInterstitial()
                }
            }
        })
    }

    private fun runEnhancedBenchmark() {
        isBenchmarkRunning = true
        benchmarkProgressText = """
            🚀 Starting Enhanced Performance Benchmark...
            
            This will test:
            • SHA-1 Hash Performance
            • MD5 Hash Performance
            • AES Encryption Performance
            • Matrix Multiplication
            • Sorting Algorithms
            • Data Compression
            • Memory Bandwidth
            • Multi-threaded Performance
            • System Overhead Analysis
            
            Please wait...
        """.trimIndent()
        benchmarkScoreText = "..."
        backgroundExecutor.execute { benchmarkEngine.runComprehensiveBenchmark() }
    }

    private fun setupAds() {
        googleMobileAdsConsentManager = GoogleMobileAdsConsentManager.getInstance(applicationContext)
        googleMobileAdsConsentManager.gatherConsent(this) { consentError ->
            if (consentError != null) {
                Log.w(TAG, "${consentError.errorCode}: ${consentError.message}")
            }
            if (googleMobileAdsConsentManager.canRequestAds()) {
                initializeMobileAdsSdk()
            }
        }
        if (googleMobileAdsConsentManager.canRequestAds()) {
            initializeMobileAdsSdk()
        }
    }

    private fun initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) return
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setTestDeviceIds(listOf(testDeviceHashedId))
                .build()
        )
        backgroundExecutor.execute {
            MobileAds.initialize(this) {
                Log.d(TAG, "Google Mobile Ads SDK Initialized.")
            }
        }
        requestNewInterstitial()
    }

    private fun requestNewInterstitial() {
        if (mInterstitialAd != null) return
        InterstitialAd.load(
            this,
            interstitialAdUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                    interstitialAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() { mInterstitialAd = null }
                        override fun onAdFailedToShowFullScreenContent(error: AdError) { mInterstitialAd = null }
                    }
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    mInterstitialAd = null
                }
            }
        )
    }

    private fun showInterstitial() {
        if (mInterstitialAd != null) {
            mInterstitialAd?.show(this)
        } else if (googleMobileAdsConsentManager.canRequestAds()) {
            requestNewInterstitial()
        }
    }

    private fun toggleSchedule() {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val enabled = prefs.getBoolean("schedule_enabled", false)
        if (enabled) {
            ScheduledBenchmarkWorker.cancel(this)
        } else {
            ScheduledBenchmarkWorker.scheduleDaily(this)
        }
        prefs.edit().putBoolean("schedule_enabled", !enabled).apply()
        Toast.makeText(this, if (!enabled) "Daily benchmark scheduled" else "Daily benchmark canceled", Toast.LENGTH_SHORT).show()
    }

    private fun toggleDarkMode() {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val dark = prefs.getBoolean("dark_mode", false)
        val newValue = !dark
        prefs.edit().putBoolean("dark_mode", newValue).apply()
        AppCompatDelegate.setDefaultNightMode(
            if (newValue) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
        Toast.makeText(this, if (newValue) "Dark mode on" else "Dark mode off", Toast.LENGTH_SHORT).show()
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

    private fun exportResults() {
        val metrics = currentMetrics ?: return
        val text = generateExportText(metrics)
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Benchmark Results", text))

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, "CPU Benchmark Results - ${metrics.deviceModel}")
        }
        startActivity(Intent.createChooser(intent, "Share Benchmark Results"))
        Toast.makeText(this, "Results copied", Toast.LENGTH_LONG).show()
    }

    private fun shareResults() {
        val metrics = currentMetrics ?: return
        val text = generateShareText(metrics)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, "My CPU Benchmark Results - ${metrics.overallScore}/100")
        }
        startActivity(Intent.createChooser(intent, "Share Benchmark Results"))
    }

    private fun generateExportText(metrics: PerformanceMetrics): String {
        return """
            === CPU BENCHMARK RESULTS ===
            Generated: ${java.util.Date()}
            
            DEVICE INFORMATION
            Model: ${metrics.deviceModel}
            CPU Cores: ${metrics.cpuCores}
            Max Memory: ${formatBytes(metrics.totalMemory)}
            
            PERFORMANCE SCORES
            Overall Score: ${metrics.overallScore}/100
            Crypto Performance: ${metrics.cryptoScore}/100
            Efficiency: ${metrics.efficiencyScore}/100
            Stability: ${metrics.stabilityScore}/100
            
            DETAILED TIMING
            ${metrics.detailedTimingInfo}
            PERFORMANCE ANALYSIS
            Category: ${getPerformanceCategory(metrics.overallScore)}
            Algorithm Comparison: SHA-1 vs MD5 efficiency analysis
            System Utilization: CPU and memory efficiency metrics
            
            Generated by CPU Benchmark App
        """.trimIndent()
    }

    private fun generateShareText(metrics: PerformanceMetrics): String {
        return """
            🚀 CPU Benchmark Results!
            
            📱 Device: ${metrics.deviceModel}
            🎯 Overall Score: ${metrics.overallScore}/100 (${getPerformanceCategory(metrics.overallScore)})
            🔐 Crypto Performance: ${metrics.cryptoScore}/100
            ⚡ Efficiency: ${metrics.efficiencyScore}/100
            🎯 Stability: ${metrics.stabilityScore}/100
            
            Tested with CPU Benchmark App!
        """.trimIndent()
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

    private fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
        val pre = "KMGTPE"[exp - 1]
        return String.format(java.util.Locale.US, "%.1f %sB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
    }

    override fun onDestroy() {
        backgroundExecutor.shutdown()
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    progressText: String,
    scoreText: String,
    isRunning: Boolean,
    onStartClick: () -> Unit,
    onShareClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onToggleSchedule: () -> Unit,
    onToggleDarkMode: () -> Unit,
    bannerAdUnitId: String
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CPU Benchmark") },
                actions = {
                    var expanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { expanded = true }) {
                        Text("⋮")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("History") }, onClick = { expanded = false; onHistoryClick() })
                        DropdownMenuItem(text = { Text("Toggle Schedule") }, onClick = { expanded = false; onToggleSchedule() })
                        DropdownMenuItem(text = { Text("Toggle Dark Mode") }, onClick = { expanded = false; onToggleDarkMode() })
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onShareClick) {
                Text("📈")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "CPU Performance Test", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onStartClick, enabled = !isRunning) {
                    Text(if (isRunning) "Running..." else "Begin Test")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = progressText,
                        modifier = Modifier.padding(16.dp),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Score: $scoreText",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Ad Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                if (bannerAdUnitId.isNotEmpty()) {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth(),
                        factory = { context ->
                            AdView(context).apply {
                                setAdUnitId(bannerAdUnitId)
                                val displayMetrics = context.resources.displayMetrics
                                val adWidth = (displayMetrics.widthPixels / displayMetrics.density).toInt()
                                setAdSize(AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, adWidth))
                                loadAd(AdRequest.Builder().build())
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MetricsDialog(
    metrics: PerformanceMetrics,
    onDismiss: () -> Unit,
    onExport: () -> Unit,
    onShare: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "📊 Detailed Performance Metrics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp).align(Alignment.CenterHorizontally)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("📈 Performance Visualization", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                                PerformanceVisualizer(metrics = metrics)
                            }
                        }
                    }

                    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("🎯 Performance Scores", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Overall Score: ${metrics.overallScore}")
                            Text("Crypto Performance: ${metrics.cryptoScore}")
                            Text("Efficiency: ${metrics.efficiencyScore}")
                            Text("Stability: ${metrics.stabilityScore}")
                        }
                    }

                    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("📱 System Information", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Device: ${metrics.deviceModel}")
                            Text("CPU Cores: ${metrics.cpuCores}")
                        }
                    }

                    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("⏱️ Timing Results", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(metrics.detailedTimingInfo, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 12.sp)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = onExport, modifier = Modifier.weight(1f)) {
                        Text("Export")
                    }
                    Button(onClick = onShare, modifier = Modifier.weight(1f)) {
                        Text("Share")
                    }
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Close")
                    }
                }
            }
        }
    }
}