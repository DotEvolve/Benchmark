package net.dotevolve.benchmark.core;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import net.dotevolve.benchmark.data.db.PerformanceDatabaseHelper;
import net.dotevolve.benchmark.data.model.BenchmarkResult;
import net.dotevolve.benchmark.data.model.DeviceStatistics;
import net.dotevolve.benchmark.data.model.PerformanceTrend;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Comprehensive performance metrics collection and analysis
 */
public class PerformanceMetrics {
    private static final String TAG = "PerformanceMetrics";
    
    // Timing data
    private long sha1TotalTime;
    private long md5TotalTime;
    private long aesTotalTime;
    private long rsaTotalTime;
    private long loopOverheadTime;
    private long matrixMultiplicationTime;
    private long sortingTime;
    private long compressionTime;
    private long memoryBandwidthTime;
    private long multiThreadedTime;
    
    // Iteration counts - increased for more challenging benchmarks
    static final int SHA1_ITERATIONS = 500000;
    static final int MD5_ITERATIONS = 500000;
    static final int AES_ITERATIONS = 50000;
    private static final int RSA_ITERATIONS = 5000;
    static final int LOOP_ITERATIONS = 10000000;
    static final int MATRIX_SIZE = 512; // For matrix multiplication
    static final int SORT_ARRAY_SIZE = 100000; // For sorting benchmark
    static final int COMPRESSION_ITERATIONS = 1000;
    static final int MEMORY_TEST_SIZE = 50 * 1024 * 1024; // 50MB memory test
    
    // Individual timing samples for statistical analysis
    private final List<Long> sha1Samples = new ArrayList<>();
    private final List<Long> md5Samples = new ArrayList<>();
    private final List<Long> aesSamples = new ArrayList<>();
    private final List<Long> rsaSamples = new ArrayList<>();
    private final List<Long> matrixSamples = new ArrayList<>();
    private final List<Long> sortSamples = new ArrayList<>();
    private final List<Long> compressionSamples = new ArrayList<>();
    
    // System information
    private String deviceModel;
    private String androidVersion;
    private int cpuCores;
    private long totalMemory;
    private String architecture;
    private String benchmarkVersion = "unknown";
    
    // Performance scores
    private int overallScore;
    private int cryptoScore;
    private int efficiencyScore;
    private int stabilityScore;
    private int computationalScore;
    private int memoryScore;
    private int multiThreadingScore;
    
    // Performance categories
    public enum PerformanceCategory {
        EXCELLENT(90, 100),
        GOOD(70, 89),
        AVERAGE(50, 69),
        BELOW_AVERAGE(30, 49),
        POOR(0, 29);
        
        private final int minScore;
        private final int maxScore;
        
        PerformanceCategory(int min, int max) {
            this.minScore = min;
            this.maxScore = max;
        }
        
        public static PerformanceCategory fromScore(int score) {
            for (PerformanceCategory category : values()) {
                if (score >= category.minScore && score <= category.maxScore) {
                    return category;
                }
            }
            return POOR;
        }
    }
    
    public PerformanceMetrics(Context context) {
        initializeSystemInfo(context);
    }
    
    private void initializeSystemInfo(Context context) {
        deviceModel = Build.MODEL;
        androidVersion = "Android " + Build.VERSION.RELEASE;
        cpuCores = Runtime.getRuntime().availableProcessors();
        totalMemory = Runtime.getRuntime().maxMemory();
        architecture = Build.SUPPORTED_ABIS[0];
        
        Log.d(TAG, "System Info - Model: " + deviceModel + 
              ", Android: " + androidVersion + 
              ", Cores: " + cpuCores + 
              ", Memory: " + formatBytes(totalMemory));
    }
    
    // Timing methods
    public void startSha1Timing() {
        sha1TotalTime = System.nanoTime();
    }
    
    public void endSha1Timing() {
        sha1TotalTime = System.nanoTime() - sha1TotalTime;
        Log.d(TAG, "SHA-1 Total Time: " + formatNanoTime(sha1TotalTime));
    }
    
    public void startMd5Timing() {
        md5TotalTime = System.nanoTime();
    }
    
    public void endMd5Timing() {
        md5TotalTime = System.nanoTime() - md5TotalTime;
        Log.d(TAG, "MD5 Total Time: " + formatNanoTime(md5TotalTime));
    }
    
    public void startAesTiming() {
        aesTotalTime = System.nanoTime();
    }
    
    public void endAesTiming() {
        aesTotalTime = System.nanoTime() - aesTotalTime;
        Log.d(TAG, "AES Total Time: " + formatNanoTime(aesTotalTime));
    }
    
    public void startRsaTiming() {
        rsaTotalTime = System.nanoTime();
    }
    
    public void endRsaTiming() {
        rsaTotalTime = System.nanoTime() - rsaTotalTime;
        Log.d(TAG, "RSA Total Time: " + formatNanoTime(rsaTotalTime));
    }
    
    public void startLoopTiming() {
        loopOverheadTime = System.nanoTime();
    }
    
    public void endLoopTiming() {
        loopOverheadTime = System.nanoTime() - loopOverheadTime;
        Log.d(TAG, "Loop Overhead Time: " + formatNanoTime(loopOverheadTime));
    }
    
    public void startMatrixTiming() {
        matrixMultiplicationTime = System.nanoTime();
    }
    
    public void endMatrixTiming() {
        matrixMultiplicationTime = System.nanoTime() - matrixMultiplicationTime;
        Log.d(TAG, "Matrix Multiplication Time: " + formatNanoTime(matrixMultiplicationTime));
    }
    
    public void startSortTiming() {
        sortingTime = System.nanoTime();
    }
    
    public void endSortTiming() {
        sortingTime = System.nanoTime() - sortingTime;
        Log.d(TAG, "Sorting Time: " + formatNanoTime(sortingTime));
    }
    
    public void startCompressionTiming() {
        compressionTime = System.nanoTime();
    }
    
    public void endCompressionTiming() {
        compressionTime = System.nanoTime() - compressionTime;
        Log.d(TAG, "Compression Time: " + formatNanoTime(compressionTime));
    }
    
    public void startMemoryTiming() {
        memoryBandwidthTime = System.nanoTime();
    }
    
    public void endMemoryTiming() {
        memoryBandwidthTime = System.nanoTime() - memoryBandwidthTime;
        Log.d(TAG, "Memory Bandwidth Time: " + formatNanoTime(memoryBandwidthTime));
    }
    
    public void startMultiThreadedTiming() {
        multiThreadedTime = System.nanoTime();
    }
    
    public void endMultiThreadedTiming() {
        multiThreadedTime = System.nanoTime() - multiThreadedTime;
        Log.d(TAG, "Multi-threaded Time: " + formatNanoTime(multiThreadedTime));
    }
    
    // Sample collection for statistical analysis
    public void addSha1Sample(long time) {
        sha1Samples.add(time);
    }
    
    public void addMd5Sample(long time) {
        md5Samples.add(time);
    }
    
    public void addAesSample(long time) {
        aesSamples.add(time);
    }
    
    public void addRsaSample(long time) {
        rsaSamples.add(time);
    }
    
    public void addMatrixSample(long time) {
        matrixSamples.add(time);
    }
    
    public void addSortSample(long time) {
        sortSamples.add(time);
    }
    
    public void addCompressionSample(long time) {
        compressionSamples.add(time);
    }
    
    // Performance calculations
    public void calculateScores() {
        cryptoScore = calculateCryptoScore();
        efficiencyScore = calculateEfficiencyScore();
        stabilityScore = calculateStabilityScore();
        computationalScore = calculateComputationalScore();
        memoryScore = calculateMemoryScore();
        multiThreadingScore = calculateMultiThreadingScore();
        
        // Weighted overall score - more emphasis on computational and crypto performance
        overallScore = (int) Math.round(
            cryptoScore * 0.25 +
            computationalScore * 0.30 +
            memoryScore * 0.20 +
            efficiencyScore * 0.15 +
            stabilityScore * 0.05 +
            multiThreadingScore * 0.05
        );
        
        Log.d(TAG, "Scores - Overall: " + overallScore + 
              ", Crypto: " + cryptoScore + 
              ", Computational: " + computationalScore +
              ", Memory: " + memoryScore +
              ", Efficiency: " + efficiencyScore + 
              ", Stability: " + stabilityScore +
              ", MultiThreading: " + multiThreadingScore);
    }
    
    private int calculateCryptoScore() {
        // Based on operations per second with realistic thresholds
        // Modern high-end devices: 200K+ ops/sec for SHA-1, 300K+ for MD5
        // Mid-range devices: 50K-150K ops/sec
        // Low-end devices: 10K-50K ops/sec
        
        double sha1OpsPerSec = (SHA1_ITERATIONS * 1_000_000_000.0) / sha1TotalTime;
        double md5OpsPerSec = (MD5_ITERATIONS * 1_000_000_000.0) / md5TotalTime;
        double aesOpsPerSec = (AES_ITERATIONS * 1_000_000_000.0) / aesTotalTime;
        
        // Normalize using logarithmic scale for better differentiation
        // 100 points = 200K ops/sec (high-end), 50 points = 50K ops/sec (mid-range), 0 points = 5K ops/sec (low-end)
        int sha1Score = normalizeLogarithmic(sha1OpsPerSec, 5000, 200000, 100);
        int md5Score = normalizeLogarithmic(md5OpsPerSec, 8000, 300000, 100);
        int aesScore = normalizeLogarithmic(aesOpsPerSec, 1000, 20000, 100);
        
        return (sha1Score + md5Score + aesScore) / 3;
    }
    
    private int calculateEfficiencyScore() {
        // Based on time per operation efficiency with realistic thresholds
        // High-end: <50ns per hash, Mid-range: 50-200ns, Low-end: >200ns
        double sha1Efficiency = (double) sha1TotalTime / SHA1_ITERATIONS;
        double md5Efficiency = (double) md5TotalTime / MD5_ITERATIONS;
        double aesEfficiency = (double) aesTotalTime / AES_ITERATIONS;
        
        // Lower time per operation = higher efficiency
        // 100 points = 10ns, 50 points = 100ns, 0 points = 1000ns
        int sha1EffScore = normalizeInverse(sha1Efficiency, 10, 1000, 100);
        int md5EffScore = normalizeInverse(md5Efficiency, 8, 800, 100);
        int aesEffScore = normalizeInverse(aesEfficiency, 50, 5000, 100);
        
        return (sha1EffScore + md5EffScore + aesEffScore) / 3;
    }
    
    private int calculateStabilityScore() {
        // Based on coefficient of variation (CV) - more accurate than raw std dev
        if (sha1Samples.size() < 2) return 50; // Default if no samples
        
        double sha1CV = calculateCoefficientOfVariation(sha1Samples);
        double md5CV = calculateCoefficientOfVariation(md5Samples);
        double aesCV = calculateCoefficientOfVariation(aesSamples);
        
        // Lower CV = higher stability score
        // 100 points = CV < 0.05 (5%), 50 points = CV = 0.15 (15%), 0 points = CV > 0.5 (50%)
        int sha1Stability = normalizeInverse(sha1CV * 100, 5, 50, 100);
        int md5Stability = normalizeInverse(md5CV * 100, 5, 50, 100);
        int aesStability = normalizeInverse(aesCV * 100, 5, 50, 100);
        
        return (sha1Stability + md5Stability + aesStability) / 3;
    }
    
    private int calculateComputationalScore() {
        // Based on matrix multiplication and sorting performance
        if (matrixMultiplicationTime == 0 || sortingTime == 0) {
            return 50; // Default if not run
        }
        
        // Matrix multiplication: O(n^3) complexity
        // High-end: <100ms for 512x512, Mid-range: 100-500ms, Low-end: >500ms
        double matrixScore = normalizeInverse(matrixMultiplicationTime / 1_000_000.0, 50, 1000, 100);
        
        // Sorting: O(n log n) complexity
        // High-end: <50ms for 100K elements, Mid-range: 50-200ms, Low-end: >200ms
        double sortScore = normalizeInverse(sortingTime / 1_000_000.0, 20, 500, 100);
        
        return (int) Math.round((matrixScore + sortScore) / 2);
    }
    
    private int calculateMemoryScore() {
        // Based on memory bandwidth and compression performance
        if (memoryBandwidthTime == 0 || compressionTime == 0) {
            return 50; // Default if not run
        }
        
        // Memory bandwidth: MB/s
        // High-end: >5000 MB/s, Mid-range: 2000-5000 MB/s, Low-end: <2000 MB/s
        double memoryBandwidth = (MEMORY_TEST_SIZE * 1_000_000_000.0) / (memoryBandwidthTime * 1024.0 * 1024.0);
        double bandwidthScore = normalizeLogarithmic(memoryBandwidth, 500, 8000, 100);
        
        // Compression: ops/sec
        double compressionOpsPerSec = (COMPRESSION_ITERATIONS * 1_000_000_000.0) / compressionTime;
        double compressionScore = normalizeLogarithmic(compressionOpsPerSec, 50, 500, 100);
        
        return (int) Math.round((bandwidthScore + compressionScore) / 2);
    }
    
    private int calculateMultiThreadingScore() {
        // Based on multi-threaded performance improvement
        if (multiThreadedTime == 0) {
            return 50; // Default if not run
        }
        
        // Compare single-threaded vs multi-threaded performance
        // High-end: >3x speedup, Mid-range: 2-3x, Low-end: <2x
        long singleThreadedTime = sha1TotalTime + md5TotalTime + matrixMultiplicationTime;
        double speedup = (double) singleThreadedTime / multiThreadedTime;
        
        // 100 points = 4x speedup, 50 points = 2x speedup, 0 points = 1x (no improvement)
        return (int) Math.max(0, Math.min(100, (speedup - 1) * 33.33));
    }
    
    // Helper methods for score normalization
    private int normalizeLogarithmic(double value, double min, double max, int maxScore) {
        if (value <= min) return 0;
        if (value >= max) return maxScore;
        
        // Logarithmic normalization for better distribution
        double logMin = Math.log(min);
        double logMax = Math.log(max);
        double logValue = Math.log(value);
        
        return (int) Math.round(((logValue - logMin) / (logMax - logMin)) * maxScore);
    }
    
    private int normalizeInverse(double value, double best, double worst, int maxScore) {
        // Inverse normalization: lower value = higher score
        if (value <= best) return maxScore;
        if (value >= worst) return 0;
        
        return (int) Math.round(maxScore * (1.0 - (value - best) / (worst - best)));
    }
    
    private double calculateCoefficientOfVariation(List<Long> samples) {
        if (samples.size() < 2) return 0;
        
        double mean = samples.stream().mapToLong(Long::longValue).average().orElse(0);
        if (mean == 0) return 0;
        
        double stdDev = calculateStandardDeviation(samples);
        return stdDev / mean; // CV = std dev / mean
    }
    
    private double calculateStandardDeviation(List<Long> samples) {
        if (samples.size() < 2) return 0;
        
        double mean = samples.stream().mapToLong(Long::longValue).average().orElse(0);
        double variance = samples.stream()
                .mapToDouble(sample -> Math.pow(sample - mean, 2))
                .average().orElse(0);
        
        return Math.sqrt(variance);
    }
    
    // Getters for display
    public String getFormattedResults() {
        StringBuilder result = new StringBuilder();
        
        result.append("=== PERFORMANCE BENCHMARK RESULTS ===\n\n");
        result.append("Engine Version: ").append(benchmarkVersion).append("\n\n");
        
        // System Information
        result.append("📱 DEVICE INFORMATION\n");
        result.append("Model: ").append(deviceModel).append("\n");
        result.append("Android: ").append(androidVersion).append("\n");
        result.append("CPU Cores: ").append(cpuCores).append("\n");
        result.append("Architecture: ").append(architecture).append("\n");
        result.append("Max Memory: ").append(formatBytes(totalMemory)).append("\n\n");
        
        // Timing Results
        result.append("⏱️ TIMING RESULTS\n");
        result.append("SHA-1 (").append(SHA1_ITERATIONS).append(" iterations):\n");
        result.append("  Total Time: ").append(formatNanoTime(sha1TotalTime)).append("\n");
        result.append("  Time per Op: ").append(formatNanoTime(sha1TotalTime / SHA1_ITERATIONS)).append("\n");
        result.append("  Operations/sec: ").append(formatOpsPerSec(sha1TotalTime, SHA1_ITERATIONS)).append("\n\n");
        
        result.append("MD5 (").append(MD5_ITERATIONS).append(" iterations):\n");
        result.append("  Total Time: ").append(formatNanoTime(md5TotalTime)).append("\n");
        result.append("  Time per Op: ").append(formatNanoTime(md5TotalTime / MD5_ITERATIONS)).append("\n");
        result.append("  Operations/sec: ").append(formatOpsPerSec(md5TotalTime, MD5_ITERATIONS)).append("\n\n");
        
        if (matrixMultiplicationTime > 0) {
            result.append("Matrix Multiplication (").append(MATRIX_SIZE).append("x").append(MATRIX_SIZE).append("):\n");
            result.append("  Total Time: ").append(formatNanoTime(matrixMultiplicationTime)).append("\n\n");
        }
        
        if (sortingTime > 0) {
            result.append("Sorting (").append(SORT_ARRAY_SIZE).append(" elements):\n");
            result.append("  Total Time: ").append(formatNanoTime(sortingTime)).append("\n\n");
        }
        
        if (compressionTime > 0) {
            result.append("Compression (").append(COMPRESSION_ITERATIONS).append(" iterations):\n");
            result.append("  Total Time: ").append(formatNanoTime(compressionTime)).append("\n\n");
        }
        
        if (memoryBandwidthTime > 0) {
            result.append("Memory Bandwidth (").append(formatBytes(MEMORY_TEST_SIZE)).append("):\n");
            result.append("  Total Time: ").append(formatNanoTime(memoryBandwidthTime)).append("\n\n");
        }
        
        if (multiThreadedTime > 0) {
            result.append("Multi-threaded Performance:\n");
            result.append("  Total Time: ").append(formatNanoTime(multiThreadedTime)).append("\n\n");
        }
        
        // Performance Scores
        result.append("📊 PERFORMANCE SCORES\n");
        result.append("Overall Score: ").append(overallScore).append("/100 (")
              .append(getPerformanceCategory(overallScore).name()).append(")\n");
        result.append("Crypto Performance: ").append(cryptoScore).append("/100\n");
        result.append("Computational: ").append(computationalScore).append("/100\n");
        result.append("Memory: ").append(memoryScore).append("/100\n");
        result.append("Efficiency: ").append(efficiencyScore).append("/100\n");
        result.append("Stability: ").append(stabilityScore).append("/100\n");
        result.append("Multi-threading: ").append(multiThreadingScore).append("/100\n\n");
        
        // Performance Analysis
        result.append("🔍 PERFORMANCE ANALYSIS\n");
        result.append("Algorithm Comparison:\n");
        double sha1VsMd5 = (double) sha1TotalTime / md5TotalTime;
        result.append("  SHA-1 vs MD5 ratio: ").append(String.format(Locale.US, "%.2fx", sha1VsMd5)).append("\n");
        
        if (sha1Samples.size() > 1) {
            result.append("  SHA-1 consistency: ").append(String.format(Locale.US, "%.1f%%", 
                Math.max(0, 100 - (calculateStandardDeviation(sha1Samples) / 1000)))).append("\n");
        }
        
        if (md5Samples.size() > 1) {
            result.append("  MD5 consistency: ").append(String.format(Locale.US, "%.1f%%", 
                Math.max(0, 100 - (calculateStandardDeviation(md5Samples) / 1000)))).append("\n");
        }
        
        return result.toString();
    }
    
    public String getDetailedTimingInfo() {
        StringBuilder info = new StringBuilder();
        
        info.append("=== DETAILED TIMING INFORMATION ===\n\n");
        
        // SHA-1 Details
        info.append("🔐 SHA-1 HASH PERFORMANCE\n");
        info.append("Iterations: ").append(SHA1_ITERATIONS).append("\n");
        info.append("Total Time: ").append(formatNanoTime(sha1TotalTime)).append("\n");
        info.append("Average per Operation: ").append(formatNanoTime(sha1TotalTime / SHA1_ITERATIONS)).append("\n");
        info.append("Operations per Second: ").append(formatOpsPerSec(sha1TotalTime, SHA1_ITERATIONS)).append("\n");
        info.append("Operations per Millisecond: ").append(String.format(Locale.US, "%.2f", 
            (SHA1_ITERATIONS * 1_000_000.0) / sha1TotalTime)).append("\n\n");
        
        // MD5 Details
        info.append("🔐 MD5 HASH PERFORMANCE\n");
        info.append("Iterations: ").append(MD5_ITERATIONS).append("\n");
        info.append("Total Time: ").append(formatNanoTime(md5TotalTime)).append("\n");
        info.append("Average per Operation: ").append(formatNanoTime(md5TotalTime / MD5_ITERATIONS)).append("\n");
        info.append("Operations per Second: ").append(formatOpsPerSec(md5TotalTime, MD5_ITERATIONS)).append("\n");
        info.append("Operations per Millisecond: ").append(String.format(Locale.US, "%.2f", 
            (MD5_ITERATIONS * 1_000_000.0) / md5TotalTime)).append("\n\n");
        
        // AES Details
        if (aesTotalTime > 0) {
            info.append("🔐 AES ENCRYPTION PERFORMANCE\n");
            info.append("Iterations: ").append(AES_ITERATIONS).append("\n");
            info.append("Total Time: ").append(formatNanoTime(aesTotalTime)).append("\n");
            info.append("Average per Operation: ").append(formatNanoTime(aesTotalTime / AES_ITERATIONS)).append("\n");
            info.append("Operations per Second: ").append(formatOpsPerSec(aesTotalTime, AES_ITERATIONS)).append("\n\n");
        }
        
        // Matrix Multiplication Details
        if (matrixMultiplicationTime > 0) {
            info.append("🔢 MATRIX MULTIPLICATION PERFORMANCE\n");
            info.append("Matrix Size: ").append(MATRIX_SIZE).append("x").append(MATRIX_SIZE).append("\n");
            info.append("Total Time: ").append(formatNanoTime(matrixMultiplicationTime)).append("\n");
            info.append("Operations: ").append(MATRIX_SIZE * MATRIX_SIZE * MATRIX_SIZE).append(" (O(n³))\n\n");
        }
        
        // Sorting Details
        if (sortingTime > 0) {
            info.append("📊 SORTING PERFORMANCE\n");
            info.append("Array Size: ").append(SORT_ARRAY_SIZE).append(" elements\n");
            info.append("Total Time: ").append(formatNanoTime(sortingTime)).append("\n");
            info.append("Time per Element: ").append(formatNanoTime(sortingTime / SORT_ARRAY_SIZE)).append("\n\n");
        }
        
        // System Performance
        info.append("💻 SYSTEM PERFORMANCE\n");
        info.append("CPU Utilization: ").append(calculateCpuUtilization()).append("%\n");
        info.append("Memory Efficiency: ").append(calculateMemoryEfficiency()).append("%\n");
        info.append("Performance per Core: ").append(calculatePerformancePerCore()).append(" ops/sec\n");
        
        return info.toString();
    }
    
    private String calculateCpuUtilization() {
        // Simplified CPU utilization calculation
        long totalCryptoTime = sha1TotalTime + md5TotalTime;
        long totalTestTime = totalCryptoTime + loopOverheadTime;
        return String.format(Locale.US, "%.1f", (double) totalCryptoTime / totalTestTime * 100);
    }
    
    private String calculateMemoryEfficiency() {
        // Simplified memory efficiency calculation
        long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        return String.format(Locale.US, "%.1f", (double) usedMemory / totalMemory * 100);
    }
    
    private String calculatePerformancePerCore() {
        double totalOps = SHA1_ITERATIONS + (double) MD5_ITERATIONS;
        long totalTime = sha1TotalTime + md5TotalTime;
        double opsPerSec = (totalOps * 1_000_000_000.0) / totalTime;
        return String.format(Locale.US, "%.0f", opsPerSec / cpuCores);
    }
    
    // Utility methods
    private String formatNanoTime(long nanoTime) {
        if (nanoTime < 1_000) {
            return nanoTime + " ns";
        } else if (nanoTime < 1_000_000) {
            return String.format(Locale.US, "%.2f μs", nanoTime / 1_000.0);
        } else if (nanoTime < 1_000_000_000) {
            return String.format(Locale.US, "%.2f ms", nanoTime / 1_000_000.0);
        } else {
            return String.format(Locale.US, "%.2f s", nanoTime / 1_000_000_000.0);
        }
    }
    
    private String formatOpsPerSec(long totalTime, int iterations) {
        double opsPerSec = (iterations * 1_000_000_000.0) / totalTime;
        if (opsPerSec >= 1_000_000) {
            return String.format(Locale.US, "%.1fM ops/sec", opsPerSec / 1_000_000);
        } else if (opsPerSec >= 1_000) {
            return String.format(Locale.US, "%.1fK ops/sec", opsPerSec / 1_000);
        } else {
            return String.format(Locale.US, "%.0f ops/sec", opsPerSec);
        }
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format(Locale.US, "%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    private PerformanceCategory getPerformanceCategory(int score) {
        return PerformanceCategory.fromScore(score);
    }
    
    // Historical tracking methods
    public void saveToHistory(Context context) {
        // Save to local database
        try {
            PerformanceDatabaseHelper dbHelper = new PerformanceDatabaseHelper(context);
            AdvancedMetrics advancedMetrics = new AdvancedMetrics(context);
            dbHelper.insertBenchmarkResult(this, advancedMetrics);
            Log.d(TAG, "Benchmark results saved to local history");
        } catch (Exception e) {
            Log.e(TAG, "Failed to save benchmark results to local history", e);
        }

        // Save to Firebase Firestore
        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Map<String, Object> benchmarkData = new HashMap<>();
            benchmarkData.put("deviceModel", deviceModel);
            benchmarkData.put("androidVersion", androidVersion);
            benchmarkData.put("cpuCores", cpuCores);
            benchmarkData.put("totalMemory", totalMemory);
            benchmarkData.put("architecture", architecture);
            benchmarkData.put("benchmarkVersion", benchmarkVersion);
            benchmarkData.put("overallScore", overallScore);
            benchmarkData.put("cryptoScore", cryptoScore);
            benchmarkData.put("computationalScore", computationalScore);
            benchmarkData.put("memoryScore", memoryScore);
            benchmarkData.put("efficiencyScore", efficiencyScore);
            benchmarkData.put("stabilityScore", stabilityScore);
            benchmarkData.put("multiThreadingScore", multiThreadingScore);
            benchmarkData.put("sha1TotalTime", sha1TotalTime);
            benchmarkData.put("md5TotalTime", md5TotalTime);
            benchmarkData.put("aesTotalTime", aesTotalTime);
            benchmarkData.put("rsaTotalTime", rsaTotalTime);
            benchmarkData.put("loopOverheadTime", loopOverheadTime);
            benchmarkData.put("matrixMultiplicationTime", matrixMultiplicationTime);
            benchmarkData.put("sortingTime", sortingTime);
            benchmarkData.put("compressionTime", compressionTime);
            benchmarkData.put("memoryBandwidthTime", memoryBandwidthTime);
            benchmarkData.put("multiThreadedTime", multiThreadedTime);
            benchmarkData.put("timestamp", Timestamp.now());

            db.collection("benchmarks")
                .add(benchmarkData)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "Benchmark results saved to Firestore with ID: " + documentReference.getId()))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving benchmark results to Firestore", e));
        } catch (Exception e) {
            Log.e(TAG, "Failed to save benchmark results to Firestore", e);
        }
    }
    
    public static List<BenchmarkResult> getHistoricalResults(Context context, String deviceModel) {
        try {
            PerformanceDatabaseHelper dbHelper = new PerformanceDatabaseHelper(context);
            return dbHelper.getAllBenchmarkResults(deviceModel);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get historical results", e);
            return new ArrayList<>();
        }
    }
    
    public static List<BenchmarkResult> getRecentResults(Context context, String deviceModel, int days) {
        try {
            PerformanceDatabaseHelper dbHelper = new PerformanceDatabaseHelper(context);
            return dbHelper.getRecentBenchmarkResults(deviceModel, days);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get recent results", e);
            return new ArrayList<>();
        }
    }
    
    public static List<PerformanceTrend> getPerformanceTrends(Context context, String deviceModel, int days) {
        try {
            PerformanceDatabaseHelper dbHelper = new PerformanceDatabaseHelper(context);
            return dbHelper.getPerformanceTrends(deviceModel, days);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get performance trends", e);
            return new ArrayList<>();
        }
    }
    
    public static DeviceStatistics getDeviceStatistics(Context context, String deviceModel) {
        try {
            PerformanceDatabaseHelper dbHelper = new PerformanceDatabaseHelper(context);
            return dbHelper.getDeviceStatistics(deviceModel);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get device statistics", e);
            return null;
        }
    }
    
    // Getters
    public int getOverallScore() { return overallScore; }
    public int getCryptoScore() { return cryptoScore; }
    public int getEfficiencyScore() { return efficiencyScore; }
    public int getStabilityScore() { return stabilityScore; }
    public String getDeviceModel() { return deviceModel; }
    public String getAndroidVersion() { return androidVersion; }
    public int getCpuCores() { return cpuCores; }
    public long getTotalMemory() { return totalMemory; }
    public String getArchitecture() { return architecture; }
    public long getSha1TotalTime() { return sha1TotalTime; }
    public long getMd5TotalTime() { return md5TotalTime; }
    public long getAesTotalTime() { return aesTotalTime; }
    public long getRsaTotalTime() { return rsaTotalTime; }
    public long getLoopOverheadTime() { return loopOverheadTime; }
    public long getMatrixMultiplicationTime() { return matrixMultiplicationTime; }
    public long getSortingTime() { return sortingTime; }
    public long getCompressionTime() { return compressionTime; }
    public long getMemoryBandwidthTime() { return memoryBandwidthTime; }
    public long getMultiThreadedTime() { return multiThreadedTime; }
    public int getComputationalScore() { return computationalScore; }
    public int getMemoryScore() { return memoryScore; }
    public int getMultiThreadingScore() { return multiThreadingScore; }
    public String getBenchmarkVersion() { return benchmarkVersion; }
    public void setBenchmarkVersion(String benchmarkVersion) { this.benchmarkVersion = benchmarkVersion; }
}

