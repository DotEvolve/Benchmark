package net.dotevolve.benchmark.core;
import net.dotevolve.benchmark.R;

import android.content.Context;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/**
 * Enhanced benchmark engine with comprehensive performance testing
 */
public class BenchmarkEngine {
    private static final String TAG = "BenchmarkEngine";
    
    private final PerformanceMetrics metrics;
    private final Context context;
    private final String testString;
    
    // Progress tracking
    private final AtomicInteger currentProgress = new AtomicInteger(0);
    private final int totalTests = 9; // SHA-1, MD5, AES, Loop, Matrix, Sort, Compression, Memory, MultiThread
    private final Random random = new Random();
    
    public interface BenchmarkProgressCallback {
        void onProgressUpdate(int progress, String currentTest);
        void onTestComplete(String testName, long duration);
        void onBenchmarkComplete(PerformanceMetrics metrics);
    }
    
    private BenchmarkProgressCallback progressCallback;
    
    public BenchmarkEngine(Context context) {
        this.context = context;
        this.metrics = new PerformanceMetrics(context);
        this.testString = context.getResources().getString(R.string.testString);
    }
    
    public void setProgressCallback(BenchmarkProgressCallback callback) {
        this.progressCallback = callback;
    }
    
    public void runComprehensiveBenchmark() {
        Log.d(TAG, "Starting comprehensive benchmark...");
        
        try {
            // Test 1: SHA-1 Hash Performance
            runSha1Benchmark();
            updateProgress(1, "SHA-1 Complete");
            
            // Test 2: MD5 Hash Performance  
            runMd5Benchmark();
            updateProgress(2, "MD5 Complete");
            
            // Test 3: AES Encryption Performance
            runAesBenchmark();
            updateProgress(3, "AES Complete");
            
            // Test 4: Loop Overhead Test
            runLoopOverheadTest();
            updateProgress(4, "Loop Overhead Complete");
            
            // Test 5: Matrix Multiplication
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
            
            // Test 9: Multi-threaded Performance
            runMultiThreadedBenchmark();
            updateProgress(9, "Multi-threaded Complete");
            
            // Calculate final scores
            metrics.calculateScores();
            
            Log.d(TAG, "Benchmark complete. Overall Score: " + metrics.getOverallScore());
            
            if (progressCallback != null) {
                progressCallback.onBenchmarkComplete(metrics);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Benchmark failed", e);
        }
    }
    
    private void runSha1Benchmark() {
        Log.d(TAG, "Running SHA-1 benchmark...");
        metrics.startSha1Timing();
        
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] inputBytes = testString.getBytes(StandardCharsets.UTF_8);
            
            for (int i = 0; i < PerformanceMetrics.SHA1_ITERATIONS; i++) {
                long startTime = System.nanoTime();
                sha1.update(inputBytes);
                byte[] hash = sha1.digest();
                long endTime = System.nanoTime();
                
                // Collect sample for statistical analysis
                metrics.addSha1Sample(endTime - startTime);
                
                // Reset digest for next iteration
                sha1.reset();
            }
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "SHA-1 algorithm not available", e);
        }
        
        metrics.endSha1Timing();
        
        if (progressCallback != null) {
            progressCallback.onTestComplete("SHA-1", metrics.getSha1TotalTime());
        }
    }
    
    private void runMd5Benchmark() {
        Log.d(TAG, "Running MD5 benchmark...");
        metrics.startMd5Timing();
        
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] inputBytes = testString.getBytes(StandardCharsets.UTF_8);
            
            for (int i = 0; i < PerformanceMetrics.MD5_ITERATIONS; i++) {
                long startTime = System.nanoTime();
                md5.update(inputBytes);
                byte[] hash = md5.digest();
                long endTime = System.nanoTime();
                
                // Collect sample for statistical analysis
                metrics.addMd5Sample(endTime - startTime);
                
                // Reset digest for next iteration
                md5.reset();
            }
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "MD5 algorithm not available", e);
        }
        
        metrics.endMd5Timing();
        
        if (progressCallback != null) {
            progressCallback.onTestComplete("MD5", metrics.getMd5TotalTime());
        }
    }
    
    private void runAesBenchmark() {
        Log.d(TAG, "Running AES benchmark...");
        metrics.startAesTiming();
        
        try {
            // Generate AES key
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            SecretKey secretKey = keyGen.generateKey();
            
            // Use AES/CBC/PKCS5Padding for better compatibility and performance testing
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            
            byte[] inputBytes = testString.getBytes(StandardCharsets.UTF_8);
            
            for (int i = 0; i < PerformanceMetrics.AES_ITERATIONS; i++) {
                long startTime = System.nanoTime();
                byte[] encrypted = cipher.doFinal(inputBytes);
                long endTime = System.nanoTime();
                
                // Collect sample for statistical analysis
                metrics.addAesSample(endTime - startTime);
            }
        } catch (Exception e) {
            Log.e(TAG, "AES benchmark failed", e);
        }
        
        metrics.endAesTiming();
        
        if (progressCallback != null) {
            progressCallback.onTestComplete("AES", metrics.getAesTotalTime());
        }
    }
    
    private void runLoopOverheadTest() {
        Log.d(TAG, "Running loop overhead test...");
        metrics.startLoopTiming();
        
        // More complex loop with multiple operations to measure overhead
        long dummy = 0;
        for (int i = 0; i < PerformanceMetrics.LOOP_ITERATIONS; i++) {
            dummy += i * 2;
            dummy -= i / 3;
            dummy ^= i;
        }
        
        metrics.endLoopTiming();
        
        if (progressCallback != null) {
            progressCallback.onTestComplete("Loop Overhead", metrics.getLoopOverheadTime());
        }
    }
    
    private void runMatrixMultiplicationBenchmark() {
        Log.d(TAG, "Running matrix multiplication benchmark...");
        metrics.startMatrixTiming();
        
        int size = PerformanceMetrics.MATRIX_SIZE;
        double[][] matrixA = new double[size][size];
        double[][] matrixB = new double[size][size];
        double[][] result = new double[size][size];
        
        // Initialize matrices with random values
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                matrixA[i][j] = random.nextDouble();
                matrixB[i][j] = random.nextDouble();
            }
        }
        
        // Perform matrix multiplication: C = A * B
        long startTime = System.nanoTime();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                result[i][j] = 0;
                for (int k = 0; k < size; k++) {
                    result[i][j] += matrixA[i][k] * matrixB[k][j];
                }
            }
        }
        long endTime = System.nanoTime();
        
        metrics.addMatrixSample(endTime - startTime);
        metrics.endMatrixTiming();
        
        if (progressCallback != null) {
            progressCallback.onTestComplete("Matrix Multiplication", metrics.getMatrixMultiplicationTime());
        }
    }
    
    private void runSortingBenchmark() {
        Log.d(TAG, "Running sorting benchmark...");
        metrics.startSortTiming();
        
        int size = PerformanceMetrics.SORT_ARRAY_SIZE;
        int[] array = new int[size];
        
        // Initialize array with random values
        for (int i = 0; i < size; i++) {
            array[i] = random.nextInt();
        }
        
        // Perform quicksort
        long startTime = System.nanoTime();
        quickSort(array, 0, size - 1);
        long endTime = System.nanoTime();
        
        metrics.addSortSample(endTime - startTime);
        metrics.endSortTiming();
        
        if (progressCallback != null) {
            progressCallback.onTestComplete("Sorting", metrics.getSortingTime());
        }
    }
    
    private void quickSort(int[] arr, int low, int high) {
        if (low < high) {
            int pi = partition(arr, low, high);
            quickSort(arr, low, pi - 1);
            quickSort(arr, pi + 1, high);
        }
    }
    
    private int partition(int[] arr, int low, int high) {
        int pivot = arr[high];
        int i = (low - 1);
        
        for (int j = low; j < high; j++) {
            if (arr[j] < pivot) {
                i++;
                int temp = arr[i];
                arr[i] = arr[j];
                arr[j] = temp;
            }
        }
        
        int temp = arr[i + 1];
        arr[i + 1] = arr[high];
        arr[high] = temp;
        
        return i + 1;
    }
    
    private void runCompressionBenchmark() {
        Log.d(TAG, "Running compression benchmark...");
        metrics.startCompressionTiming();
        
        // Create test data
        StringBuilder testData = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            testData.append(testString).append(i);
        }
        byte[] data = testData.toString().getBytes(StandardCharsets.UTF_8);
        
        for (int i = 0; i < PerformanceMetrics.COMPRESSION_ITERATIONS; i++) {
            long startTime = System.nanoTime();
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                GZIPOutputStream gzos = new GZIPOutputStream(baos);
                gzos.write(data);
                gzos.close();
                byte[] compressed = baos.toByteArray();
                long endTime = System.nanoTime();
                metrics.addCompressionSample(endTime - startTime);
            } catch (IOException e) {
                Log.e(TAG, "Compression failed", e);
            }
        }
        
        metrics.endCompressionTiming();
        
        if (progressCallback != null) {
            progressCallback.onTestComplete("Compression", metrics.getCompressionTime());
        }
    }
    
    private void runMemoryBandwidthBenchmark() {
        Log.d(TAG, "Running memory bandwidth benchmark...");
        metrics.startMemoryTiming();
        
        int size = PerformanceMetrics.MEMORY_TEST_SIZE / 4; // int array size
        int[] array1 = new int[size];
        int[] array2 = new int[size];
        
        // Initialize arrays
        for (int i = 0; i < size; i++) {
            array1[i] = random.nextInt();
            array2[i] = random.nextInt();
        }
        
        // Perform memory-intensive operations: copy, reverse, sum
        long startTime = System.nanoTime();
        
        // Sequential memory access pattern
        for (int i = 0; i < size; i++) {
            array2[i] = array1[i];
        }
        
        // Reverse copy
        for (int i = 0; i < size / 2; i++) {
            int temp = array1[i];
            array1[i] = array1[size - 1 - i];
            array1[size - 1 - i] = temp;
        }
        
        // Sum operation
        long sum = 0;
        for (int i = 0; i < size; i++) {
            sum += array1[i] + array2[i];
        }
        
        long endTime = System.nanoTime();
        metrics.endMemoryTiming();
        
        if (progressCallback != null) {
            progressCallback.onTestComplete("Memory Bandwidth", metrics.getMemoryBandwidthTime());
        }
    }
    
    private void runMultiThreadedBenchmark() {
        Log.d(TAG, "Running multi-threaded benchmark...");
        metrics.startMultiThreadedTiming();
        
        int numThreads = Math.min(metrics.getCpuCores(), 8); // Use up to 8 threads
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        
        // Each thread performs SHA-1 hashing
        int iterationsPerThread = PerformanceMetrics.SHA1_ITERATIONS / numThreads;
        
        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
                    byte[] inputBytes = (testString + threadId).getBytes(StandardCharsets.UTF_8);
                    
                    for (int i = 0; i < iterationsPerThread; i++) {
                        sha1.update(inputBytes);
                        byte[] hash = sha1.digest();
                        sha1.reset();
                    }
                } catch (NoSuchAlgorithmException e) {
                    Log.e(TAG, "SHA-1 algorithm not available in thread", e);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            latch.await();
        } catch (InterruptedException e) {
            Log.e(TAG, "Multi-threaded benchmark interrupted", e);
        }
        
        executor.shutdown();
        metrics.endMultiThreadedTiming();
        
        if (progressCallback != null) {
            progressCallback.onTestComplete("Multi-threaded", metrics.getMultiThreadedTime());
        }
    }
    
    private void updateProgress(int completed, String currentTest) {
        int progress = (completed * 100) / totalTests;
        currentProgress.set(progress);
        
        if (progressCallback != null) {
            progressCallback.onProgressUpdate(progress, currentTest);
        }
        
        Log.d(TAG, "Progress: " + progress + "% - " + currentTest);
    }
    
    // Legacy methods for backward compatibility
    public void runLegacyBenchmark() {
        Log.d(TAG, "Running legacy benchmark...");
        
        // Run only SHA-1 and MD5 for backward compatibility
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
}

