package net.dotevolve.benchmark.core;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import net.dotevolve.benchmark.data.db.PerformanceDatabaseHelper;
import net.dotevolve.benchmark.data.model.BenchmarkResult;
import net.dotevolve.benchmark.data.model.DeviceStatistics;
import net.dotevolve.benchmark.data.model.PerformanceTrend;
import net.dotevolve.benchmark.data.remote.BenchmarkApiClient;
import net.dotevolve.benchmark.work.SyncBenchmarksWorker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Comprehensive performance metrics collection, exponential positive-skew scoring, and analysis.
 */
public class PerformanceMetrics {
    private static final String TAG = "PerformanceMetrics";

    /**
     * Concave exponential curve parameter k for positive-skewed scoring.
     * Maps normalized hardware ratios in [0, 1] to motivating [0, 100] scores while
     * preserving strict monotonicity and flagship headroom.
     */
    static final double SCORE_CURVE_K = 2.2;

    // Stage 1 calibration anchors (v2.0.0)
    static final double SHA512_OPS_MIN = 25_000.0;
    static final double SHA512_OPS_MAX = 1_200_000.0;
    static final double MD5_OPS_MIN = 40_000.0;
    static final double MD5_OPS_MAX = 2_500_000.0;
    static final double AES_OPS_MIN = 5_000.0;
    static final double AES_OPS_MAX = 250_000.0;

    static final double MATRIX_BEST_MS = 25.0;
    static final double MATRIX_WORST_MS = 1_800.0;
    static final double SORT_BEST_MS = 4.0;
    static final double SORT_WORST_MS = 120.0;
    static final double LOOP_BEST_MS = 8.0;
    static final double LOOP_WORST_MS = 250.0;

    static final double MEMORY_BW_MIN_MB_S = 1_000.0;
    static final double MEMORY_BW_MAX_MB_S = 28_000.0;
    static final double COMPRESSION_OPS_MIN = 60.0;
    static final double COMPRESSION_OPS_MAX = 1_500.0;

    static final double MT_PER_CORE_OPS_MIN = 15_000.0;
    static final double MT_PER_CORE_OPS_MAX = 400_000.0;

    static final double STABILITY_CV_BEST = 0.02;
    static final double STABILITY_CV_WORST = 0.45;

    // Timing data (nanoseconds)
    private long sha1TotalTime;
    private long md5TotalTime;
    private long aesTotalTime;
    private long rsaTotalTime; // Retained at 0 for DB/API schema compatibility
    private long loopOverheadTime;
    private long matrixMultiplicationTime;
    private long sortingTime;
    private long compressionTime;
    private long memoryBandwidthTime;
    private long multiThreadedTime;
    private long memoryBytesTransferred;

    // Active timer start timestamps (kept separate so aborted tests never corrupt *TotalTime)
    private long sha1StartTime;
    private long md5StartTime;
    private long aesStartTime;
    private long loopStartTime;
    private long matrixStartTime;
    private long sortStartTime;
    private long compressionStartTime;
    private long memoryStartTime;
    private long multiThreadedStartTime;

    private boolean sha1TimingActive;
    private boolean md5TimingActive;
    private boolean aesTimingActive;
    private boolean loopTimingActive;
    private boolean matrixTimingActive;
    private boolean sortTimingActive;
    private boolean compressionTimingActive;
    private boolean memoryTimingActive;
    private boolean multiThreadedTimingActive;

    // Iteration / workload constants
    static final int SHA1_ITERATIONS = 500_000;
    static final int MD5_ITERATIONS = 500_000;
    static final int AES_ITERATIONS = 50_000;
    static final int LOOP_ITERATIONS = 10_000_000;
    static final int MATRIX_SIZE = 512;
    static final int SORT_ARRAY_SIZE = 100_000;
    static final int COMPRESSION_ITERATIONS = 200;
    static final int MEMORY_TEST_SIZE = 24 * 1024 * 1024; // 24 MB capped working set per array

    // Primitive macro-batch sample buffers (zero autoboxing overhead)
    private final SampleBuffer sha1Samples = new SampleBuffer(64);
    private final SampleBuffer md5Samples = new SampleBuffer(64);
    private final SampleBuffer aesSamples = new SampleBuffer(64);
    private final SampleBuffer matrixSamples = new SampleBuffer(16);
    private final SampleBuffer sortSamples = new SampleBuffer(16);
    private final SampleBuffer compressionSamples = new SampleBuffer(64);

    // System information
    private String deviceModel;
    private String deviceManufacturer;
    private String deviceBrand;
    private String androidVersion;
    private int cpuCores;
    private long totalMemory;
    private String architecture;
    private String benchmarkVersion = "unknown";
    private String runType = "full";
    private String clientDeviceId;
    private volatile String lastClientRunId;
    private volatile String lastRemoteId;

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

    private static final class SampleBuffer {
        private long[] values;
        private int size;

        SampleBuffer(int initialCapacity) {
            this.values = new long[initialCapacity];
        }

        void add(long sample) {
            if (size == values.length) {
                values = Arrays.copyOf(values, values.length * 2);
            }
            values[size++] = sample;
        }

        void clear() {
            size = 0;
        }

        int size() {
            return size;
        }

        double calculateCV() {
            if (size < 2) {
                return 0.0;
            }
            double sum = 0.0;
            for (int i = 0; i < size; i++) {
                sum += values[i];
            }
            double mean = sum / size;
            if (mean <= 0.0) {
                return 0.0;
            }
            double varianceSum = 0.0;
            for (int i = 0; i < size; i++) {
                double diff = values[i] - mean;
                varianceSum += diff * diff;
            }
            double stdDev = Math.sqrt(varianceSum / size);
            return stdDev / mean;
        }
    }

    public PerformanceMetrics(Context context) {
        initializeSystemInfo(context);
    }

    @VisibleForTesting
    PerformanceMetrics() {
        this.deviceModel = "TestDevice";
        this.deviceManufacturer = "TestOEM";
        this.deviceBrand = "test";
        this.androidVersion = "Android Test";
        this.cpuCores = Math.max(1, Runtime.getRuntime().availableProcessors());
        this.totalMemory = Runtime.getRuntime().maxMemory();
        this.architecture = "arm64-v8a";
        this.benchmarkVersion = BenchmarkEngine.ENGINE_VERSION;
    }

    private void initializeSystemInfo(Context context) {
        deviceModel = Build.MODEL != null ? Build.MODEL : "unknown";
        deviceManufacturer = Build.MANUFACTURER != null ? Build.MANUFACTURER : "unknown";
        deviceBrand = Build.BRAND != null ? Build.BRAND : "unknown";
        androidVersion = "Android " + (Build.VERSION.RELEASE != null ? Build.VERSION.RELEASE : "unknown");
        cpuCores = Math.max(1, Runtime.getRuntime().availableProcessors());
        totalMemory = Runtime.getRuntime().maxMemory();
        architecture = (Build.SUPPORTED_ABIS != null && Build.SUPPORTED_ABIS.length > 0)
                ? Build.SUPPORTED_ABIS[0]
                : "unknown";
        if (context != null) {
            clientDeviceId = DeviceIdentityProvider.getOrCreateClientDeviceId(context);
        }

        logDebug("System Info - Model: " + deviceModel +
                ", Manufacturer: " + deviceManufacturer +
                ", Brand: " + deviceBrand +
                ", Android: " + androidVersion +
                ", Cores: " + cpuCores +
                ", Memory: " + formatBytes(totalMemory));
    }

    /**
     * Clears all timings, sample buffers, scores, and run identifiers before a new benchmark execution.
     */
    public synchronized void resetForNewRun() {
        sha1TotalTime = 0L;
        md5TotalTime = 0L;
        aesTotalTime = 0L;
        rsaTotalTime = 0L;
        loopOverheadTime = 0L;
        matrixMultiplicationTime = 0L;
        sortingTime = 0L;
        compressionTime = 0L;
        memoryBandwidthTime = 0L;
        multiThreadedTime = 0L;
        memoryBytesTransferred = 0L;

        sha1StartTime = 0L;
        md5StartTime = 0L;
        aesStartTime = 0L;
        loopStartTime = 0L;
        matrixStartTime = 0L;
        sortStartTime = 0L;
        compressionStartTime = 0L;
        memoryStartTime = 0L;
        multiThreadedStartTime = 0L;

        sha1TimingActive = false;
        md5TimingActive = false;
        aesTimingActive = false;
        loopTimingActive = false;
        matrixTimingActive = false;
        sortTimingActive = false;
        compressionTimingActive = false;
        memoryTimingActive = false;
        multiThreadedTimingActive = false;

        sha1Samples.clear();
        md5Samples.clear();
        aesSamples.clear();
        matrixSamples.clear();
        sortSamples.clear();
        compressionSamples.clear();

        overallScore = 0;
        cryptoScore = 0;
        efficiencyScore = 0;
        stabilityScore = 0;
        computationalScore = 0;
        memoryScore = 0;
        multiThreadingScore = 0;

        resetRunIds();
    }

    // Timing methods
    public void startSha1Timing() {
        sha1TotalTime = 0L;
        sha1StartTime = System.nanoTime();
        sha1TimingActive = true;
    }

    public void endSha1Timing() {
        if (sha1TimingActive) {
            sha1TotalTime = Math.max(1L, System.nanoTime() - sha1StartTime);
            sha1TimingActive = false;
            logDebug("SHA-512 Total Time: " + formatNanoTime(sha1TotalTime));
        }
    }

    public void startMd5Timing() {
        md5TotalTime = 0L;
        md5StartTime = System.nanoTime();
        md5TimingActive = true;
    }

    public void endMd5Timing() {
        if (md5TimingActive) {
            md5TotalTime = Math.max(1L, System.nanoTime() - md5StartTime);
            md5TimingActive = false;
            logDebug("MD5 Total Time: " + formatNanoTime(md5TotalTime));
        }
    }

    public void startAesTiming() {
        aesTotalTime = 0L;
        aesStartTime = System.nanoTime();
        aesTimingActive = true;
    }

    public void endAesTiming() {
        if (aesTimingActive) {
            aesTotalTime = Math.max(1L, System.nanoTime() - aesStartTime);
            aesTimingActive = false;
            logDebug("AES Total Time: " + formatNanoTime(aesTotalTime));
        }
    }

    public void startLoopTiming() {
        loopOverheadTime = 0L;
        loopStartTime = System.nanoTime();
        loopTimingActive = true;
    }

    public void endLoopTiming() {
        if (loopTimingActive) {
            loopOverheadTime = Math.max(1L, System.nanoTime() - loopStartTime);
            loopTimingActive = false;
            logDebug("Loop ALU/FP Time: " + formatNanoTime(loopOverheadTime));
        }
    }

    public void startMatrixTiming() {
        matrixMultiplicationTime = 0L;
        matrixStartTime = System.nanoTime();
        matrixTimingActive = true;
    }

    public void endMatrixTiming() {
        if (matrixTimingActive) {
            matrixMultiplicationTime = Math.max(1L, System.nanoTime() - matrixStartTime);
            matrixTimingActive = false;
            logDebug("Matrix Multiplication Time: " + formatNanoTime(matrixMultiplicationTime));
        }
    }

    public void startSortTiming() {
        sortingTime = 0L;
        sortStartTime = System.nanoTime();
        sortTimingActive = true;
    }

    public void endSortTiming() {
        if (sortTimingActive) {
            sortingTime = Math.max(1L, System.nanoTime() - sortStartTime);
            sortTimingActive = false;
            logDebug("Sorting Time: " + formatNanoTime(sortingTime));
        }
    }

    public void startCompressionTiming() {
        compressionTime = 0L;
        compressionStartTime = System.nanoTime();
        compressionTimingActive = true;
    }

    public void endCompressionTiming() {
        if (compressionTimingActive) {
            compressionTime = Math.max(1L, System.nanoTime() - compressionStartTime);
            compressionTimingActive = false;
            logDebug("Compression Time: " + formatNanoTime(compressionTime));
        }
    }

    public void startMemoryTiming() {
        memoryBandwidthTime = 0L;
        memoryStartTime = System.nanoTime();
        memoryTimingActive = true;
    }

    public void endMemoryTiming() {
        if (memoryTimingActive) {
            memoryBandwidthTime = Math.max(1L, System.nanoTime() - memoryStartTime);
            memoryTimingActive = false;
            logDebug("Memory Bandwidth Time: " + formatNanoTime(memoryBandwidthTime));
        }
    }

    public void startMultiThreadedTiming() {
        multiThreadedTime = 0L;
        multiThreadedStartTime = System.nanoTime();
        multiThreadedTimingActive = true;
    }

    public void endMultiThreadedTiming() {
        if (multiThreadedTimingActive) {
            multiThreadedTime = Math.max(1L, System.nanoTime() - multiThreadedStartTime);
            multiThreadedTimingActive = false;
            logDebug("Multi-threaded Time: " + formatNanoTime(multiThreadedTime));
        }
    }

    // Macro-batch sample collection for statistical stability analysis
    public void addSha1Sample(long time) {
        sha1Samples.add(time);
    }

    public void addMd5Sample(long time) {
        md5Samples.add(time);
    }

    public void addAesSample(long time) {
        aesSamples.add(time);
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

        int weighted = (int) Math.round(
                cryptoScore * 0.25 +
                computationalScore * 0.30 +
                memoryScore * 0.20 +
                efficiencyScore * 0.15 +
                stabilityScore * 0.05 +
                multiThreadingScore * 0.05
        );
        overallScore = Math.max(0, Math.min(100, weighted));

        logDebug("Scores - Overall: " + overallScore +
                ", Crypto: " + cryptoScore +
                ", Computational: " + computationalScore +
                ", Memory: " + memoryScore +
                ", Efficiency: " + efficiencyScore +
                ", Stability: " + stabilityScore +
                ", MultiThreading: " + multiThreadingScore);
    }

    private int calculateCryptoScore() {
        double sum = 0.0;
        int count = 0;

        if (sha1TotalTime > 0) {
            double sha1OpsPerSec = (SHA1_ITERATIONS * 1_000_000_000.0) / sha1TotalTime;
            sum += applyExponentialSkew(normalizeLogThroughput(sha1OpsPerSec, SHA512_OPS_MIN, SHA512_OPS_MAX), SCORE_CURVE_K);
            count++;
        }
        if (md5TotalTime > 0) {
            double md5OpsPerSec = (MD5_ITERATIONS * 1_000_000_000.0) / md5TotalTime;
            sum += applyExponentialSkew(normalizeLogThroughput(md5OpsPerSec, MD5_OPS_MIN, MD5_OPS_MAX), SCORE_CURVE_K);
            count++;
        }
        if (aesTotalTime > 0) {
            double aesOpsPerSec = (AES_ITERATIONS * 1_000_000_000.0) / aesTotalTime;
            sum += applyExponentialSkew(normalizeLogThroughput(aesOpsPerSec, AES_OPS_MIN, AES_OPS_MAX), SCORE_CURVE_K);
            count++;
        }

        return count > 0 ? (int) Math.round(sum / count) : 50;
    }

    private int calculateEfficiencyScore() {
        double ratioSum = 0.0;
        int count = 0;

        if (sha1TotalTime > 0) {
            double sha1OpsPerSec = (SHA1_ITERATIONS * 1_000_000_000.0) / sha1TotalTime;
            ratioSum += normalizeLogThroughput(sha1OpsPerSec, SHA512_OPS_MIN, SHA512_OPS_MAX);
            count++;
        }
        if (md5TotalTime > 0) {
            double md5OpsPerSec = (MD5_ITERATIONS * 1_000_000_000.0) / md5TotalTime;
            ratioSum += normalizeLogThroughput(md5OpsPerSec, MD5_OPS_MIN, MD5_OPS_MAX);
            count++;
        }
        if (aesTotalTime > 0) {
            double aesOpsPerSec = (AES_ITERATIONS * 1_000_000_000.0) / aesTotalTime;
            ratioSum += normalizeLogThroughput(aesOpsPerSec, AES_OPS_MIN, AES_OPS_MAX);
            count++;
        }
        if (loopOverheadTime > 0) {
            double loopMs = loopOverheadTime / 1_000_000.0;
            ratioSum += normalizeLogLatency(loopMs, LOOP_BEST_MS, LOOP_WORST_MS);
            count++;
        }
        if (multiThreadedTime > 0) {
            int effectiveCores = Math.max(1, Math.min(cpuCores, 8));
            double mtOpsPerCore = ((SHA1_ITERATIONS * 1_000_000_000.0) / multiThreadedTime) / effectiveCores;
            ratioSum += normalizeLogThroughput(mtOpsPerCore, MT_PER_CORE_OPS_MIN, MT_PER_CORE_OPS_MAX);
            count++;
        }

        if (count == 0) {
            return 50;
        }
        return applyExponentialSkew(ratioSum / count, SCORE_CURVE_K);
    }

    private int calculateStabilityScore() {
        double cvSum = 0.0;
        int cvCount = 0;

        if (sha1Samples.size() >= 2) {
            cvSum += sha1Samples.calculateCV();
            cvCount++;
        }
        if (md5Samples.size() >= 2) {
            cvSum += md5Samples.calculateCV();
            cvCount++;
        }
        if (aesSamples.size() >= 2) {
            cvSum += aesSamples.calculateCV();
            cvCount++;
        }
        if (compressionSamples.size() >= 2) {
            cvSum += compressionSamples.calculateCV();
            cvCount++;
        }

        if (cvCount == 0) {
            return sha1TotalTime > 0 ? 75 : 50;
        }

        double meanCv = cvSum / cvCount;
        double ratio = clamp01((STABILITY_CV_WORST - meanCv) / (STABILITY_CV_WORST - STABILITY_CV_BEST));
        return applyExponentialSkew(ratio, SCORE_CURVE_K);
    }

    private int calculateComputationalScore() {
        if (matrixMultiplicationTime <= 0 && sortingTime <= 0 && loopOverheadTime <= 0) {
            return 50;
        }

        if (matrixMultiplicationTime > 0 && sortingTime > 0 && loopOverheadTime > 0) {
            double matrixScore = applyExponentialSkew(
                    normalizeLogLatency(matrixMultiplicationTime / 1_000_000.0, MATRIX_BEST_MS, MATRIX_WORST_MS),
                    SCORE_CURVE_K
            );
            double sortScore = applyExponentialSkew(
                    normalizeLogLatency(sortingTime / 1_000_000.0, SORT_BEST_MS, SORT_WORST_MS),
                    SCORE_CURVE_K
            );
            double loopScore = applyExponentialSkew(
                    normalizeLogLatency(loopOverheadTime / 1_000_000.0, LOOP_BEST_MS, LOOP_WORST_MS),
                    SCORE_CURVE_K
            );
            return (int) Math.round(matrixScore * 0.45 + sortScore * 0.35 + loopScore * 0.20);
        }

        double sum = 0.0;
        int count = 0;
        if (matrixMultiplicationTime > 0) {
            sum += applyExponentialSkew(
                    normalizeLogLatency(matrixMultiplicationTime / 1_000_000.0, MATRIX_BEST_MS, MATRIX_WORST_MS),
                    SCORE_CURVE_K
            );
            count++;
        }
        if (sortingTime > 0) {
            sum += applyExponentialSkew(
                    normalizeLogLatency(sortingTime / 1_000_000.0, SORT_BEST_MS, SORT_WORST_MS),
                    SCORE_CURVE_K
            );
            count++;
        }
        if (loopOverheadTime > 0) {
            sum += applyExponentialSkew(
                    normalizeLogLatency(loopOverheadTime / 1_000_000.0, LOOP_BEST_MS, LOOP_WORST_MS),
                    SCORE_CURVE_K
            );
            count++;
        }
        return count > 0 ? (int) Math.round(sum / count) : 50;
    }

    private int calculateMemoryScore() {
        if (memoryBandwidthTime <= 0 || compressionTime <= 0) {
            return 50;
        }

        double bandwidthMbPerSec = calculateMemoryBandwidthMbPerSec();
        double bandwidthScore = applyExponentialSkew(
                normalizeLogThroughput(bandwidthMbPerSec, MEMORY_BW_MIN_MB_S, MEMORY_BW_MAX_MB_S),
                SCORE_CURVE_K
        );

        double compressionOpsPerSec = (COMPRESSION_ITERATIONS * 1_000_000_000.0) / compressionTime;
        double compressionScore = applyExponentialSkew(
                normalizeLogThroughput(compressionOpsPerSec, COMPRESSION_OPS_MIN, COMPRESSION_OPS_MAX),
                SCORE_CURVE_K
        );

        return (int) Math.round((bandwidthScore + compressionScore) / 2.0);
    }

    private int calculateMultiThreadingScore() {
        if (multiThreadedTime <= 0 || sha1TotalTime <= 0) {
            return 50;
        }

        int effectiveCores = Math.max(1, Math.min(cpuCores, 8));
        if (effectiveCores <= 1) {
            return applyExponentialSkew(0.5, SCORE_CURVE_K);
        }

        double speedup = (double) sha1TotalTime / multiThreadedTime;
        double maxExpectedSpeedup = Math.max(1.5, effectiveCores * 0.65);
        double ratio = clamp01((speedup - 1.0) / (maxExpectedSpeedup - 1.0));
        return applyExponentialSkew(ratio, SCORE_CURVE_K);
    }

    private double calculateMemoryBandwidthMbPerSec() {
        if (memoryBandwidthTime <= 0) {
            return 0.0;
        }
        long bytes = memoryBytesTransferred > 0 ? memoryBytesTransferred : MEMORY_TEST_SIZE;
        return (bytes * 1_000_000_000.0) / (memoryBandwidthTime * 1024.0 * 1024.0);
    }

    // Two-stage normalization & exponential skew helpers
    @VisibleForTesting
    static double normalizeLogThroughput(double value, double min, double max) {
        if (Double.isNaN(value) || value <= min || min <= 0.0 || max <= min) {
            return 0.0;
        }
        if (value >= max) {
            return 1.0;
        }
        return clamp01(Math.log(value / min) / Math.log(max / min));
    }

    @VisibleForTesting
    static double normalizeLogLatency(double latencyMs, double bestMs, double worstMs) {
        if (Double.isNaN(latencyMs) || latencyMs <= 0.0 || bestMs <= 0.0 || worstMs <= bestMs) {
            return 0.0;
        }
        if (latencyMs <= bestMs) {
            return 1.0;
        }
        if (latencyMs >= worstMs) {
            return 0.0;
        }
        return clamp01(Math.log(worstMs / latencyMs) / Math.log(worstMs / bestMs));
    }

    @VisibleForTesting
    static int applyExponentialSkew(double normalized, double k) {
        if (Double.isNaN(normalized)) {
            return 0;
        }
        double clamped = clamp01(normalized);
        if (k <= 1e-9) {
            return (int) Math.round(clamped * 100.0);
        }
        double skewed = (1.0 - Math.exp(-k * clamped)) / (1.0 - Math.exp(-k));
        return (int) Math.max(0L, Math.min(100L, Math.round(skewed * 100.0)));
    }

    private static double clamp01(double value) {
        if (Double.isNaN(value) || value <= 0.0) {
            return 0.0;
        }
        return Math.min(1.0, value);
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
        if (sha1TotalTime > 0) {
            result.append("SHA-512 (").append(SHA1_ITERATIONS).append(" iterations):\n");
            result.append("  Total Time: ").append(formatNanoTime(sha1TotalTime)).append("\n");
            result.append("  Time per Op: ").append(formatNanoTime(sha1TotalTime / SHA1_ITERATIONS)).append("\n");
            result.append("  Operations/sec: ").append(formatOpsPerSec(sha1TotalTime, SHA1_ITERATIONS)).append("\n\n");
        }

        if (md5TotalTime > 0) {
            result.append("MD5 (").append(MD5_ITERATIONS).append(" iterations):\n");
            result.append("  Total Time: ").append(formatNanoTime(md5TotalTime)).append("\n");
            result.append("  Time per Op: ").append(formatNanoTime(md5TotalTime / MD5_ITERATIONS)).append("\n");
            result.append("  Operations/sec: ").append(formatOpsPerSec(md5TotalTime, MD5_ITERATIONS)).append("\n\n");
        }

        if (aesTotalTime > 0) {
            result.append("AES-256 (").append(AES_ITERATIONS).append(" iterations):\n");
            result.append("  Total Time: ").append(formatNanoTime(aesTotalTime)).append("\n");
            result.append("  Operations/sec: ").append(formatOpsPerSec(aesTotalTime, AES_ITERATIONS)).append("\n\n");
        }

        if (loopOverheadTime > 0) {
            result.append("ALU / FP Loop (").append(LOOP_ITERATIONS).append(" iterations):\n");
            result.append("  Total Time: ").append(formatNanoTime(loopOverheadTime)).append("\n\n");
        }

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
            long bytes = memoryBytesTransferred > 0 ? memoryBytesTransferred : MEMORY_TEST_SIZE;
            result.append("Memory Bandwidth (").append(formatBytes(bytes)).append(" transferred):\n");
            result.append("  Total Time: ").append(formatNanoTime(memoryBandwidthTime)).append("\n");
            result.append("  Throughput: ").append(String.format(Locale.US, "%.0f MB/s", calculateMemoryBandwidthMbPerSec())).append("\n\n");
        }

        if (multiThreadedTime > 0) {
            result.append("Multi-threaded SHA-512:\n");
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
        if (sha1TotalTime > 0 && md5TotalTime > 0) {
            result.append("Algorithm Comparison:\n");
            double shaVsMd5 = (double) sha1TotalTime / md5TotalTime;
            result.append("  SHA-512 vs MD5 ratio: ").append(String.format(Locale.US, "%.2fx", shaVsMd5)).append("\n");
        }

        if (sha1Samples.size() > 1) {
            double consistency = Math.max(0.0, Math.min(100.0, (1.0 - sha1Samples.calculateCV()) * 100.0));
            result.append("  SHA-512 consistency: ").append(String.format(Locale.US, "%.1f%%", consistency)).append("\n");
        }

        if (md5Samples.size() > 1) {
            double consistency = Math.max(0.0, Math.min(100.0, (1.0 - md5Samples.calculateCV()) * 100.0));
            result.append("  MD5 consistency: ").append(String.format(Locale.US, "%.1f%%", consistency)).append("\n");
        }

        return result.toString();
    }

    public String getDetailedTimingInfo() {
        StringBuilder info = new StringBuilder();

        info.append("=== DETAILED TIMING INFORMATION ===\n\n");

        // SHA-512 Details
        if (sha1TotalTime > 0) {
            info.append("🔐 SHA-512 HASH PERFORMANCE\n");
            info.append("Iterations: ").append(SHA1_ITERATIONS).append("\n");
            info.append("Total Time: ").append(formatNanoTime(sha1TotalTime)).append("\n");
            info.append("Average per Operation: ").append(formatNanoTime(sha1TotalTime / SHA1_ITERATIONS)).append("\n");
            info.append("Operations per Second: ").append(formatOpsPerSec(sha1TotalTime, SHA1_ITERATIONS)).append("\n");
            info.append("Operations per Millisecond: ").append(String.format(Locale.US, "%.2f",
                    (SHA1_ITERATIONS * 1_000_000.0) / sha1TotalTime)).append("\n\n");
        }

        // MD5 Details
        if (md5TotalTime > 0) {
            info.append("🔐 MD5 HASH PERFORMANCE\n");
            info.append("Iterations: ").append(MD5_ITERATIONS).append("\n");
            info.append("Total Time: ").append(formatNanoTime(md5TotalTime)).append("\n");
            info.append("Average per Operation: ").append(formatNanoTime(md5TotalTime / MD5_ITERATIONS)).append("\n");
            info.append("Operations per Second: ").append(formatOpsPerSec(md5TotalTime, MD5_ITERATIONS)).append("\n");
            info.append("Operations per Millisecond: ").append(String.format(Locale.US, "%.2f",
                    (MD5_ITERATIONS * 1_000_000.0) / md5TotalTime)).append("\n\n");
        }

        // AES Details
        if (aesTotalTime > 0) {
            info.append("🔐 AES-256 ENCRYPTION PERFORMANCE\n");
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
            info.append("Operations: ").append((long) MATRIX_SIZE * MATRIX_SIZE * MATRIX_SIZE).append(" (O(n³))\n\n");
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
        long totalCryptoTime = sha1TotalTime + md5TotalTime;
        long totalTestTime = totalCryptoTime + loopOverheadTime;
        if (totalTestTime <= 0) {
            return "0.0";
        }
        return String.format(Locale.US, "%.1f", (double) totalCryptoTime / totalTestTime * 100);
    }

    private String calculateMemoryEfficiency() {
        if (totalMemory <= 0) {
            return "0.0";
        }
        long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        return String.format(Locale.US, "%.1f", (double) usedMemory / totalMemory * 100);
    }

    private String calculatePerformancePerCore() {
        long totalTime = sha1TotalTime + md5TotalTime;
        if (totalTime <= 0 || cpuCores <= 0) {
            return "0";
        }
        double totalOps = SHA1_ITERATIONS + (double) MD5_ITERATIONS;
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
        if (totalTime <= 0) {
            return "0 ops/sec";
        }
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

    private static void logDebug(String message) {
        try {
            Log.d(TAG, message);
        } catch (RuntimeException ignored) {
            // Ignored in local JVM unit tests where android.util.Log is not mocked
        }
    }

    public synchronized void resetRunIds() {
        this.lastClientRunId = null;
        this.lastRemoteId = null;
    }

    public synchronized String ensureClientRunId() {
        if (lastClientRunId == null || lastClientRunId.trim().isEmpty()) {
            lastClientRunId = UUID.randomUUID().toString();
        }
        return lastClientRunId;
    }

    // Historical tracking methods
    public void saveToHistory(Context context) {
        long localId = -1;
        String clientRunId = ensureClientRunId();
        if (clientDeviceId == null && context != null) {
            clientDeviceId = DeviceIdentityProvider.getOrCreateClientDeviceId(context);
        }

        // 1. Save to local database (offline-first guarantee)
        try {
            PerformanceDatabaseHelper dbHelper = PerformanceDatabaseHelper.getInstance(context);
            AdvancedMetrics advancedMetrics = new AdvancedMetrics(context);
            localId = dbHelper.insertBenchmarkResult(this, advancedMetrics, clientRunId, runType);
            Log.d(TAG, "Benchmark results saved to local history with id: " + localId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to save benchmark results to local history", e);
        }

        // 2. Upload full runs to Cloudflare API (or enqueue sync on failure)
        if ("full".equals(runType)) {
            try {
                BenchmarkApiClient apiClient = new BenchmarkApiClient();
                BenchmarkApiClient.ApiResult apiResult = apiClient.uploadBenchmark(this, clientRunId);
                if (apiResult.status == BenchmarkApiClient.ApiResultStatus.SUCCESS) {
                    this.lastRemoteId = apiResult.remoteId;
                    if (localId != -1) {
                        PerformanceDatabaseHelper.getInstance(context).markRunSynced(localId, apiResult.remoteId);
                    }
                    Log.d(TAG, "Benchmark uploaded directly to Cloudflare API: " + apiResult.remoteId);
                } else if (apiResult.status == BenchmarkApiClient.ApiResultStatus.PERMANENT_ERROR) {
                    if (localId != -1) {
                        PerformanceDatabaseHelper.getInstance(context).markRunPermanentFailure(localId);
                    }
                    Log.e(TAG, "Permanent error uploading benchmark: " + apiResult.errorMessage);
                } else {
                    Log.w(TAG, "Transient error uploading benchmark (" + apiResult.errorMessage + "), enqueuing sync worker");
                    SyncBenchmarksWorker.enqueue(context);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to upload benchmark directly, enqueuing sync worker", e);
                SyncBenchmarksWorker.enqueue(context);
            }
        } else {
            Log.d(TAG, "Skipping remote API upload for non-full run: " + runType);
        }
    }

    public static List<BenchmarkResult> getHistoricalResults(Context context, String deviceModel) {
        try {
            PerformanceDatabaseHelper dbHelper = PerformanceDatabaseHelper.getInstance(context);
            return dbHelper.getAllBenchmarkResults(deviceModel);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get historical results", e);
            return new ArrayList<>();
        }
    }

    public static List<BenchmarkResult> getRecentResults(Context context, String deviceModel, int days) {
        try {
            PerformanceDatabaseHelper dbHelper = PerformanceDatabaseHelper.getInstance(context);
            return dbHelper.getRecentBenchmarkResults(deviceModel, days);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get recent results", e);
            return new ArrayList<>();
        }
    }

    public static List<PerformanceTrend> getPerformanceTrends(Context context, String deviceModel, int days) {
        try {
            PerformanceDatabaseHelper dbHelper = PerformanceDatabaseHelper.getInstance(context);
            return dbHelper.getPerformanceTrends(deviceModel, days);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get performance trends", e);
            return new ArrayList<>();
        }
    }

    public static DeviceStatistics getDeviceStatistics(Context context, String deviceModel) {
        try {
            PerformanceDatabaseHelper dbHelper = PerformanceDatabaseHelper.getInstance(context);
            return dbHelper.getDeviceStatistics(deviceModel);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get device statistics", e);
            return null;
        }
    }

    // Getters & test setters
    public int getOverallScore() { return overallScore; }
    public int getCryptoScore() { return cryptoScore; }
    public int getEfficiencyScore() { return efficiencyScore; }
    public int getStabilityScore() { return stabilityScore; }
    public String getDeviceModel() { return deviceModel; }
    public String getDeviceManufacturer() { return deviceManufacturer; }
    public String getDeviceBrand() { return deviceBrand; }
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
    public long getMemoryBytesTransferred() { return memoryBytesTransferred; }
    public void setMemoryBytesTransferred(long memoryBytesTransferred) {
        this.memoryBytesTransferred = Math.max(0L, memoryBytesTransferred);
    }
    public int getComputationalScore() { return computationalScore; }
    public int getMemoryScore() { return memoryScore; }
    public int getMultiThreadingScore() { return multiThreadingScore; }
    public String getBenchmarkVersion() { return benchmarkVersion; }
    public void setBenchmarkVersion(String benchmarkVersion) { this.benchmarkVersion = benchmarkVersion; }
    public String getRunType() { return runType != null ? runType : "full"; }
    public void setRunType(String runType) { this.runType = runType; }
    public String getClientDeviceId() { return clientDeviceId; }
    public void setClientDeviceId(String clientDeviceId) { this.clientDeviceId = clientDeviceId; }
    public String getClientRunId() { return lastClientRunId; }
    public String getRemoteId() { return lastRemoteId; }
    public String getWebRunId() {
        return (lastRemoteId != null && !lastRemoteId.trim().isEmpty()) ? lastRemoteId : lastClientRunId;
    }

    @VisibleForTesting
    void setTimingsForTest(
            long sha1TotalTime,
            long md5TotalTime,
            long aesTotalTime,
            long loopOverheadTime,
            long matrixMultiplicationTime,
            long sortingTime,
            long compressionTime,
            long memoryBandwidthTime,
            long memoryBytesTransferred,
            long multiThreadedTime,
            int cpuCores
    ) {
        this.sha1TotalTime = sha1TotalTime;
        this.md5TotalTime = md5TotalTime;
        this.aesTotalTime = aesTotalTime;
        this.loopOverheadTime = loopOverheadTime;
        this.matrixMultiplicationTime = matrixMultiplicationTime;
        this.sortingTime = sortingTime;
        this.compressionTime = compressionTime;
        this.memoryBandwidthTime = memoryBandwidthTime;
        this.memoryBytesTransferred = memoryBytesTransferred;
        this.multiThreadedTime = multiThreadedTime;
        this.cpuCores = Math.max(1, cpuCores);
    }

    @VisibleForTesting
    int getSampleCountForTest() {
        return sha1Samples.size() + md5Samples.size() + aesSamples.size()
                + matrixSamples.size() + sortSamples.size() + compressionSamples.size();
    }
}
