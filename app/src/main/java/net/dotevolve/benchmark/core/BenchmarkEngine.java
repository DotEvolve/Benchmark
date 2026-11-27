package net.dotevolve.benchmark.core;

import android.content.Context;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import net.dotevolve.benchmark.R;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Deflater;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Enhanced benchmark engine (v2.0.0) with isolated kernel timing, warm-up,
 * anti-DCE blackhole sinks, macro-batch sampling, and exponential positive-skew scoring.
 */
public class BenchmarkEngine {
    private static final String TAG = "BenchmarkEngine";

    /**
     * Benchmark engine versioning follows MAJOR.MINOR.PATCH (semver):
     * - MAJOR: breaking test-suite/scoring changes that invalidate comparisons.
     * - MINOR: new tests or scoring tweaks that remain comparable within the major line.
     * - PATCH: bug fixes or perf tweaks that do not affect final scoring logic.
     */
    public static final String ENGINE_VERSION = "2.0.0";

    private static final int SHA1_BATCHES = 40;
    private static final int MD5_BATCHES = 40;
    private static final int AES_BATCHES = 25;
    private static final int COMPRESSION_BATCHES = 20;

    private static final int HASH_PAYLOAD_BYTES = 64;
    private static final int AES_PAYLOAD_BYTES = 1024;
    private static final int COMPRESSION_PAYLOAD_BYTES = 64 * 1024;

    private final PerformanceMetrics metrics;
    private final String testString;

    // Progress tracking
    private final AtomicInteger currentProgress = new AtomicInteger(0);
    private final int totalTests = 9; // SHA-512, MD5, AES, ALU/FP Loop, Matrix, Sort, Compression, Memory, MultiThread

    // Anti-DCE sink: written once at the end of each test from a local accumulator
    @SuppressWarnings("unused")
    private volatile long blackholeSink;

    public interface BenchmarkProgressCallback {
        void onProgressUpdate(int progress, String currentTest);
        void onTestComplete(String testName, long duration);
        void onBenchmarkComplete(PerformanceMetrics metrics);
    }

    private BenchmarkProgressCallback progressCallback;

    public BenchmarkEngine(Context context) {
        this.metrics = new PerformanceMetrics(context);
        this.metrics.setBenchmarkVersion(ENGINE_VERSION);
        this.testString = context.getResources().getString(R.string.testString);
    }

    @VisibleForTesting
    BenchmarkEngine(PerformanceMetrics metrics, String testString) {
        this.metrics = metrics;
        this.metrics.setBenchmarkVersion(ENGINE_VERSION);
        this.testString = (testString != null && !testString.isEmpty()) ? testString : "benchmark-test-payload";
    }

    public void setProgressCallback(BenchmarkProgressCallback callback) {
        this.progressCallback = callback;
    }

    public void runComprehensiveBenchmark() {
        logDebug("Starting comprehensive benchmark...");
        metrics.resetForNewRun();
        metrics.setRunType("full");
        currentProgress.set(0);

        try {
            // Untimed deterministic warm-up so ART JIT compiles hot loops before Test 1
            runWarmup();

            // Test 1: SHA-512 Hash Performance
            runSha1Benchmark();
            updateProgress(1, "SHA-512 Complete");

            // Test 2: MD5 Hash Performance
            runMd5Benchmark();
            updateProgress(2, "MD5 Complete");

            // Test 3: AES Encryption Performance
            runAesBenchmark();
            updateProgress(3, "AES Complete");

            // Test 4: CPU ALU & FP Loop Test
            runLoopOverheadTest();
            updateProgress(4, "ALU / FP Loop Complete");

            // Test 5: Matrix Multiplication (ikj cache-friendly)
            runMatrixMultiplicationBenchmark();
            updateProgress(5, "Matrix Multiplication Complete");

            // Test 6: Sorting Performance
            runSortingBenchmark();
            updateProgress(6, "Sorting Complete");

            // Test 7: Compression Performance
            runCompressionBenchmark();
            updateProgress(7, "Compression Complete");

            // Test 8: Memory Bandwidth
            runMemoryBandwidthBenchmark();
            updateProgress(8, "Memory Bandwidth Complete");

            // Test 9: Multi-threaded SHA-512 Performance
            runMultiThreadedBenchmark();
            updateProgress(9, "Multi-threaded Complete");

            // Calculate final scores
            metrics.calculateScores();

            logDebug("Benchmark complete. Overall Score: " + metrics.getOverallScore());

            if (progressCallback != null) {
                progressCallback.onBenchmarkComplete(metrics);
            }

        } catch (Exception e) {
            logError("Benchmark failed", e);
        }
    }

    private void runWarmup() {
        long acc = 0L;
        try {
            MessageDigest sha512 = MessageDigest.getInstance("SHA-512");
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] hashInput = buildDeterministicPayload(testString, HASH_PAYLOAD_BYTES, 0x517CC1B727220A95L);
            for (int i = 0; i < 500; i++) {
                sha512.update(hashInput);
                acc ^= sha512.digest()[0];
                sha512.reset();

                md5.update(hashInput);
                acc ^= md5.digest()[0];
                md5.reset();
            }

            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            SecretKey key = keyGen.generateKey();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] warmupIv = new byte[12];
            new SecureRandom().nextBytes(warmupIv);
            byte[] aesInput = buildDeterministicPayload(testString, 256, 0x9E3779B97F4A7C15L);
            byte[] aesOut = new byte[256 + 16];
            for (int i = 0; i < 100; i++) {
                incrementNonce(warmupIv);
                cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, warmupIv));
                int outLen = cipher.doFinal(aesInput, 0, aesInput.length, aesOut, 0);
                acc ^= aesOut[outLen - 1];
            }
        } catch (Exception ignored) {
            // Warm-up fallback: continue cleanly if provider differs
        }

        // Warm up ALU/FP, matrix ikj, and sorting JIT paths
        double fp = 1.0001;
        long intAcc = 1L;
        for (int i = 1; i <= 50_000; i++) {
            intAcc = (intAcc * 31L) ^ (i + 7L);
            fp = fp * 1.000001 + 0.25;
        }
        acc ^= intAcc ^ Double.doubleToRawLongBits(fp);

        int[] sortWarmup = new int[2000];
        long seed = 0x123456789ABCDEF0L;
        for (int i = 0; i < sortWarmup.length; i++) {
            seed = xorshift64(seed);
            sortWarmup[i] = (int) seed;
        }
        Arrays.sort(sortWarmup);
        acc ^= sortWarmup[0];

        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION);
        try {
            byte[] sample = buildCompressionPayload(testString, 4096);
            byte[] out = new byte[4096];
            for (int i = 0; i < 5; i++) {
                deflater.reset();
                deflater.setInput(sample);
                deflater.finish();
                while (!deflater.finished()) {
                    acc += deflater.deflate(out);
                }
            }
        } finally {
            deflater.end();
        }

        blackholeSink = acc;
    }

    private void runSha1Benchmark() {
        logDebug("Running SHA-512 benchmark...");
        byte[] inputBytes = buildDeterministicPayload(testString, HASH_PAYLOAD_BYTES, 0x517CC1B727220A95L);
        int totalIterations = PerformanceMetrics.SHA1_ITERATIONS;
        int batchSize = totalIterations / SHA1_BATCHES;
        long localAcc = 0L;

        try {
            MessageDigest sha512 = MessageDigest.getInstance("SHA-512");
            metrics.startSha1Timing();

            int completed = 0;
            while (completed < totalIterations) {
                int count = Math.min(batchSize, totalIterations - completed);
                long batchStart = System.nanoTime();
                for (int i = 0; i < count; i++) {
                    sha512.update(inputBytes);
                    byte[] hash = sha512.digest();
                    localAcc ^= hash[0];
                    sha512.reset();
                }
                long batchEnd = System.nanoTime();
                metrics.addSha1Sample(Math.max(1L, batchEnd - batchStart));
                completed += count;
            }

            metrics.endSha1Timing();
        } catch (NoSuchAlgorithmException e) {
            logError("SHA-512 algorithm not available", e);
        }

        blackholeSink = localAcc;

        if (progressCallback != null) {
            progressCallback.onTestComplete("SHA-512", metrics.getSha1TotalTime());
        }
    }

    private void runMd5Benchmark() {
        logDebug("Running MD5 benchmark...");
        byte[] inputBytes = buildDeterministicPayload(testString, HASH_PAYLOAD_BYTES, 0x9E3779B97F4A7C15L);
        int totalIterations = PerformanceMetrics.MD5_ITERATIONS;
        int batchSize = totalIterations / MD5_BATCHES;
        long localAcc = 0L;

        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            metrics.startMd5Timing();

            int completed = 0;
            while (completed < totalIterations) {
                int count = Math.min(batchSize, totalIterations - completed);
                long batchStart = System.nanoTime();
                for (int i = 0; i < count; i++) {
                    md5.update(inputBytes);
                    byte[] hash = md5.digest();
                    localAcc ^= hash[0];
                    md5.reset();
                }
                long batchEnd = System.nanoTime();
                metrics.addMd5Sample(Math.max(1L, batchEnd - batchStart));
                completed += count;
            }

            metrics.endMd5Timing();
        } catch (NoSuchAlgorithmException e) {
            logError("MD5 algorithm not available", e);
        }

        blackholeSink = localAcc;

        if (progressCallback != null) {
            progressCallback.onTestComplete("MD5", metrics.getMd5TotalTime());
        }
    }

    private void runAesBenchmark() {
        logDebug("Running AES-256 benchmark...");
        byte[] inputBytes = buildDeterministicPayload(testString, AES_PAYLOAD_BYTES, 0xBF58476D1CE4E5B9L);
        int totalIterations = PerformanceMetrics.AES_ITERATIONS;
        int batchSize = totalIterations / AES_BATCHES;
        long localAcc = 0L;

        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            SecretKey secretKey = keyGen.generateKey();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            byte[] outputBuffer = new byte[AES_PAYLOAD_BYTES + 16];

            metrics.startAesTiming();

            int completed = 0;
            while (completed < totalIterations) {
                int count = Math.min(batchSize, totalIterations - completed);
                long batchStart = System.nanoTime();
                for (int i = 0; i < count; i++) {
                    incrementNonce(iv);
                    cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));
                    int outLen = cipher.doFinal(inputBytes, 0, inputBytes.length, outputBuffer, 0);
                    localAcc ^= outputBuffer[outLen - 1];
                }
                long batchEnd = System.nanoTime();
                metrics.addAesSample(Math.max(1L, batchEnd - batchStart));
                completed += count;
            }

            metrics.endAesTiming();
        } catch (Exception e) {
            logError("AES benchmark failed", e);
        }

        blackholeSink = localAcc;

        if (progressCallback != null) {
            progressCallback.onTestComplete("AES-256", metrics.getAesTotalTime());
        }
    }

    private void runLoopOverheadTest() {
        logDebug("Running ALU / FP loop benchmark...");
        long intAcc = 0x9E3779B97F4A7C15L;
        double fpAcc = 1.125;

        metrics.startLoopTiming();
        for (int i = 1; i <= PerformanceMetrics.LOOP_ITERATIONS; i++) {
            intAcc += (i * 3L) ^ Long.rotateLeft(intAcc, 5);
            intAcc -= (i / 3L);
            if ((i & 3) == 0) {
                fpAcc = fpAcc * 1.0000001 + (i & 255) * 0.0001;
            }
        }
        metrics.endLoopTiming();

        blackholeSink = intAcc ^ Double.doubleToRawLongBits(fpAcc);

        if (progressCallback != null) {
            progressCallback.onTestComplete("ALU / FP Loop", metrics.getLoopOverheadTime());
        }
    }

    private void runMatrixMultiplicationBenchmark() {
        logDebug("Running matrix multiplication benchmark (ikj)...");
        int size = PerformanceMetrics.MATRIX_SIZE;
        double[][] matrixA = new double[size][size];
        double[][] matrixB = new double[size][size];
        double[][] result = new double[size][size];

        // Pre-fill outside the timer using XorShift64
        long seed = 0x2545F4914F6CDD1DL;
        for (int i = 0; i < size; i++) {
            double[] rowA = matrixA[i];
            double[] rowB = matrixB[i];
            for (int j = 0; j < size; j++) {
                seed = xorshift64(seed);
                rowA[j] = ((seed >>> 11) & 0xFFFF) * 0.0001;
                seed = xorshift64(seed);
                rowB[j] = ((seed >>> 11) & 0xFFFF) * 0.0001;
            }
        }

        // Timed cache-friendly ikj matrix multiplication: C = A * B
        metrics.startMatrixTiming();
        long startTime = System.nanoTime();
        for (int i = 0; i < size; i++) {
            double[] rowA = matrixA[i];
            double[] rowC = result[i];
            for (int k = 0; k < size; k++) {
                double a = rowA[k];
                double[] rowB = matrixB[k];
                for (int j = 0; j < size; j++) {
                    rowC[j] += a * rowB[j];
                }
            }
        }
        long endTime = System.nanoTime();
        metrics.endMatrixTiming();
        metrics.addMatrixSample(Math.max(1L, endTime - startTime));

        double trace = 0.0;
        for (int i = 0; i < size; i++) {
            trace += result[i][i];
        }
        blackholeSink = Double.doubleToRawLongBits(trace);

        if (progressCallback != null) {
            progressCallback.onTestComplete("Matrix Multiplication", metrics.getMatrixMultiplicationTime());
        }
    }

    private void runSortingBenchmark() {
        logDebug("Running sorting benchmark...");
        int size = PerformanceMetrics.SORT_ARRAY_SIZE;
        int[] array = new int[size];

        // Pre-fill outside timer using deterministic XorShift64
        long seed = 0xD1B54A32D192ED03L;
        for (int i = 0; i < size; i++) {
            seed = xorshift64(seed);
            array[i] = Long.hashCode(seed);
        }

        metrics.startSortTiming();
        long startTime = System.nanoTime();
        Arrays.sort(array);
        long endTime = System.nanoTime();
        metrics.endSortTiming();
        metrics.addSortSample(Math.max(1L, endTime - startTime));

        blackholeSink = ((long) array[0] << 32) ^ (array[size - 1] & 0xFFFFFFFFL);

        if (progressCallback != null) {
            progressCallback.onTestComplete("Sorting", metrics.getSortingTime());
        }
    }

    private void runCompressionBenchmark() {
        logDebug("Running compression benchmark...");
        byte[] data = buildCompressionPayload(testString, COMPRESSION_PAYLOAD_BYTES);
        byte[] deflateBuffer = new byte[8192];
        int totalIterations = PerformanceMetrics.COMPRESSION_ITERATIONS;
        int batchSize = totalIterations / COMPRESSION_BATCHES;
        long localAcc = 0L;

        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION);
        try {
            metrics.startCompressionTiming();

            int completed = 0;
            while (completed < totalIterations) {
                int count = Math.min(batchSize, totalIterations - completed);
                long batchStart = System.nanoTime();
                for (int i = 0; i < count; i++) {
                    deflater.reset();
                    deflater.setInput(data);
                    deflater.finish();
                    while (!deflater.finished()) {
                        localAcc += deflater.deflate(deflateBuffer);
                    }
                }
                long batchEnd = System.nanoTime();
                metrics.addCompressionSample(Math.max(1L, batchEnd - batchStart));
                completed += count;
            }

            metrics.endCompressionTiming();
        } finally {
            deflater.end();
        }

        blackholeSink = localAcc;

        if (progressCallback != null) {
            progressCallback.onTestComplete("Compression", metrics.getCompressionTime());
        }
    }

    private void runMemoryBandwidthBenchmark() {
        logDebug("Running memory bandwidth benchmark...");
        int workingSetBytes = computeSafeMemoryWorkingSetBytes();
        int elementCount = workingSetBytes / 4;
        int[] src = new int[elementCount];
        int[] dst = new int[elementCount];

        // Pre-fill outside timer
        long seed = 0x94D049BB133111EBL;
        for (int i = 0; i < elementCount; i++) {
            seed = xorshift64(seed);
            src[i] = (int) seed;
        }

        long sum = 0L;
        metrics.startMemoryTiming();

        // Pass 1: STREAM Copy (reads src + writes dst = 2 * workingSetBytes)
        System.arraycopy(src, 0, dst, 0, elementCount);

        // Pass 2: STREAM Scale (reads src + writes dst = 2 * workingSetBytes)
        for (int i = 0; i < elementCount; i++) {
            dst[i] = src[i] * 3 + 1;
        }

        // Pass 3: STREAM Triad (reads src + reads dst + writes dst = 3 * workingSetBytes)
        for (int i = 0; i < elementCount; i++) {
            dst[i] += src[i];
        }

        // Pass 4: STREAM Sum / Reduction (reads dst = 1 * workingSetBytes)
        for (int i = 0; i < elementCount; i++) {
            sum += dst[i];
        }

        metrics.endMemoryTiming();
        metrics.setMemoryBytesTransferred(8L * elementCount * 4L);
        blackholeSink = sum;

        if (progressCallback != null) {
            progressCallback.onTestComplete("Memory Bandwidth", metrics.getMemoryBandwidthTime());
        }
    }

    private void runMultiThreadedBenchmark() {
        logDebug("Running multi-threaded SHA-512 benchmark...");
        int numThreads = Math.max(1, Math.min(8, metrics.getCpuCores()));
        byte[] basePayload = buildDeterministicPayload(testString, HASH_PAYLOAD_BYTES, 0x517CC1B727220A95L);
        int totalIterations = PerformanceMetrics.SHA1_ITERATIONS;
        int baseIterations = totalIterations / numThreads;
        int remainder = totalIterations % numThreads;

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch readyLatch = new CountDownLatch(numThreads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);
        long[] threadAccumulators = new long[numThreads];

        for (int t = 0; t < numThreads; t++) {
            final int threadIndex = t;
            final int threadIterations = baseIterations + (t < remainder ? 1 : 0);
            final byte[] threadPayload = Arrays.copyOf(basePayload, basePayload.length);
            threadPayload[0] = (byte) (threadPayload[0] ^ threadIndex);

            executor.submit(() -> {
                long localAcc = 0L;
                try {
                    MessageDigest sha512 = MessageDigest.getInstance("SHA-512");
                    readyLatch.countDown();
                    startGate.await();
                    for (int i = 0; i < threadIterations; i++) {
                        sha512.update(threadPayload);
                        byte[] hash = sha512.digest();
                        localAcc ^= hash[0];
                        sha512.reset();
                    }
                    threadAccumulators[threadIndex] = localAcc;
                } catch (NoSuchAlgorithmException e) {
                    logError("SHA-512 algorithm not available in thread", e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logError("Multi-threaded worker interrupted", e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        try {
            readyLatch.await();
            metrics.startMultiThreadedTiming();
            startGate.countDown();
            doneLatch.await();
            metrics.endMultiThreadedTiming();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logError("Multi-threaded benchmark interrupted", e);
            metrics.endMultiThreadedTiming();
        } finally {
            executor.shutdown();
            try {
                executor.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }

        long combinedAcc = 0L;
        for (long acc : threadAccumulators) {
            combinedAcc ^= acc;
        }
        blackholeSink = combinedAcc;

        if (progressCallback != null) {
            progressCallback.onTestComplete("Multi-threaded", metrics.getMultiThreadedTime());
        }
    }

    private static int computeSafeMemoryWorkingSetBytes() {
        long maxHeap = Runtime.getRuntime().maxMemory();
        long tenthHeap = maxHeap > 0 ? maxHeap / 10L : PerformanceMetrics.MEMORY_TEST_SIZE;
        long cappedLong = Math.max(1024L * 1024L, Math.min(PerformanceMetrics.MEMORY_TEST_SIZE, tenthHeap));
        int capped = (int) cappedLong;
        return (capped / 4) * 4;
    }

    private static byte[] buildDeterministicPayload(String seedText, int lengthBytes, long initialSeed) {
        byte[] seedBytes = seedText.getBytes(StandardCharsets.UTF_8);
        byte[] payload = new byte[lengthBytes];
        long state = initialSeed;
        for (int i = 0; i < lengthBytes; i++) {
            state = xorshift64(state);
            byte seedByte = seedBytes.length > 0 ? seedBytes[i % seedBytes.length] : (byte) i;
            payload[i] = (byte) (seedByte ^ (byte) (state >>> 24));
        }
        return payload;
    }

    private static byte[] buildCompressionPayload(String seedText, int lengthBytes) {
        byte[] seedBytes = seedText.getBytes(StandardCharsets.UTF_8);
        byte[] payload = new byte[lengthBytes];
        long state = 0xA0761D6478BD642FL;
        for (int i = 0; i < lengthBytes; i++) {
            // Blend repetitive structured tokens with pseudo-random bytes for realistic ~2.5-3.5x compression
            if ((i & 31) < 24 && seedBytes.length > 0) {
                payload[i] = seedBytes[i % seedBytes.length];
            } else {
                state = xorshift64(state);
                payload[i] = (byte) (state >>> 16);
            }
        }
        return payload;
    }

    private static void incrementNonce(byte[] iv) {
        for (int i = iv.length - 1; i >= 0; i--) {
            if (++iv[i] != 0) {
                break;
            }
        }
    }

    private static long xorshift64(long x) {
        x ^= (x << 13);
        x ^= (x >>> 7);
        x ^= (x << 17);
        return x;
    }

    private void updateProgress(int completed, String currentTest) {
        int progress = (completed * 100) / totalTests;
        currentProgress.set(progress);

        if (progressCallback != null) {
            progressCallback.onProgressUpdate(progress, currentTest);
        }

        logDebug("Progress: " + progress + "% - " + currentTest);
    }

    // Legacy methods for backward compatibility
    public void runLegacyBenchmark() {
        logDebug("Running legacy benchmark...");
        metrics.resetForNewRun();
        metrics.setRunType("legacy");
        runWarmup();

        // Run only SHA-512 and MD5 for backward compatibility
        runSha1Benchmark();
        runMd5Benchmark();

        metrics.calculateScores();

        if (progressCallback != null) {
            progressCallback.onBenchmarkComplete(metrics);
        }
    }

    // Getters
    public PerformanceMetrics getMetrics() {
        return metrics;
    }

    public int getCurrentProgress() {
        return currentProgress.get();
    }

    public int getTotalTests() {
        return totalTests;
    }

    private static void logDebug(String message) {
        try {
            Log.d(TAG, message);
        } catch (RuntimeException ignored) {
            // Ignored in local JVM unit tests where android.util.Log is not mocked
        }
    }

    private static void logError(String message, Throwable error) {
        try {
            Log.e(TAG, message, error);
        } catch (RuntimeException ignored) {
            // Ignored in local JVM unit tests where android.util.Log is not mocked
        }
    }
}
