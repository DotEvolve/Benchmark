package net.dotevolve.benchmark.core;
import net.dotevolve.benchmark.R;

import android.content.Context;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicInteger;

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
    private final int totalTests = 4; // SHA-1, MD5, AES, RSA
    
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
            
            Cipher cipher = Cipher.getInstance("AES/GCM/OAEPWITHSHA-256ANDMGF1PADDING");
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
        
        // Simple loop to measure overhead
        int dummy = 0;
        for (int i = 0; i < PerformanceMetrics.LOOP_ITERATIONS; i++) {
            dummy += i;
        }
        
        metrics.endLoopTiming();
        
        if (progressCallback != null) {
            progressCallback.onTestComplete("Loop Overhead", metrics.getLoopOverheadTime());
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

