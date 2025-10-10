package net.dotevolve.benchmark.core;
import net.dotevolve.benchmark.data.db.PerformanceDatabaseHelper;
import net.dotevolve.benchmark.data.model.BenchmarkResult;
import net.dotevolve.benchmark.data.model.DeviceStatistics;
import net.dotevolve.benchmark.data.model.PerformanceTrend;
import net.dotevolve.benchmark.core.AdvancedMetrics;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
    
    // Iteration counts
    static final int SHA1_ITERATIONS = 100000;
    static final int MD5_ITERATIONS = 100000;
    static final int AES_ITERATIONS = 10000;
    private static final int RSA_ITERATIONS = 1000;
    static final int LOOP_ITERATIONS = 1000000;
    
    // Individual timing samples for statistical analysis
    private final List<Long> sha1Samples = new ArrayList<>();
    private final List<Long> md5Samples = new ArrayList<>();
    private final List<Long> aesSamples = new ArrayList<>();
    private final List<Long> rsaSamples = new ArrayList<>();
    
    // System information
    private String deviceModel;
    private String androidVersion;
    private int cpuCores;
    private long totalMemory;
    private String architecture;
    
    // Performance scores
    private int overallScore;
    private int cryptoScore;
    private int efficiencyScore;
    private int stabilityScore;
    
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
        architecture = Build.CPU_ABI;
        
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
    
    // Performance calculations
    public void calculateScores() {
        cryptoScore = calculateCryptoScore();
        efficiencyScore = calculateEfficiencyScore();
        stabilityScore = calculateStabilityScore();
        overallScore = (cryptoScore + efficiencyScore + stabilityScore) / 3;
        
        Log.d(TAG, "Scores - Overall: " + overallScore + 
              ", Crypto: " + cryptoScore + 
              ", Efficiency: " + efficiencyScore + 
              ", Stability: " + stabilityScore);
    }
    
    private int calculateCryptoScore() {
        // Based on operations per second
        double sha1OpsPerSec = (SHA1_ITERATIONS * 1_000_000_000.0) / sha1TotalTime;
        double md5OpsPerSec = (MD5_ITERATIONS * 1_000_000_000.0) / md5TotalTime;
        
        // Normalize to 0-100 scale (adjust thresholds based on testing)
        int sha1Score = Math.min(100, (int) (sha1OpsPerSec / 1000)); // 1000 ops/sec = 100 points
        int md5Score = Math.min(100, (int) (md5OpsPerSec / 1000));
        
        return (sha1Score + md5Score) / 2;
    }
    
    private int calculateEfficiencyScore() {
        // Based on time per operation efficiency
        double sha1Efficiency = (double) sha1TotalTime / SHA1_ITERATIONS;
        double md5Efficiency = (double) md5TotalTime / MD5_ITERATIONS;
        
        // Lower time per operation = higher efficiency
        int sha1EffScore = Math.max(0, 100 - (int) (sha1Efficiency / 1000)); // 1000ns = 0 points
        int md5EffScore = Math.max(0, 100 - (int) (md5Efficiency / 1000));
        
        return (sha1EffScore + md5EffScore) / 2;
    }
    
    private int calculateStabilityScore() {
        // Based on standard deviation of samples (lower deviation = higher stability)
        if (sha1Samples.size() < 2) return 50; // Default if no samples
        
        double sha1StdDev = calculateStandardDeviation(sha1Samples);
        double md5StdDev = calculateStandardDeviation(md5Samples);
        
        // Lower standard deviation = higher stability score
        int sha1Stability = Math.max(0, 100 - (int) (sha1StdDev / 1000));
        int md5Stability = Math.max(0, 100 - (int) (md5StdDev / 1000));
        
        return (sha1Stability + md5Stability) / 2;
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
        
        // Performance Scores
        result.append("📊 PERFORMANCE SCORES\n");
        result.append("Overall Score: ").append(overallScore).append("/100 (")
              .append(getPerformanceCategory(overallScore).name()).append(")\n");
        result.append("Crypto Performance: ").append(cryptoScore).append("/100\n");
        result.append("Efficiency: ").append(efficiencyScore).append("/100\n");
        result.append("Stability: ").append(stabilityScore).append("/100\n\n");
        
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
        double totalOps = SHA1_ITERATIONS + MD5_ITERATIONS;
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
        try {
            PerformanceDatabaseHelper dbHelper = new PerformanceDatabaseHelper(context);
            AdvancedMetrics advancedMetrics = new AdvancedMetrics(context);
            dbHelper.insertBenchmarkResult(this, advancedMetrics);
            Log.d(TAG, "Benchmark results saved to history");
        } catch (Exception e) {
            Log.e(TAG, "Failed to save benchmark results to history", e);
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
    public int getCpuCores() { return cpuCores; }
    public long getTotalMemory() { return totalMemory; }
    public long getSha1TotalTime() { return sha1TotalTime; }
    public long getMd5TotalTime() { return md5TotalTime; }
    public long getAesTotalTime() { return aesTotalTime; }
    public long getRsaTotalTime() { return rsaTotalTime; }
    public long getLoopOverheadTime() { return loopOverheadTime; }
}

