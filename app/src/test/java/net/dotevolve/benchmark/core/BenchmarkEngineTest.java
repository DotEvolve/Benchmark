package net.dotevolve.benchmark.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class BenchmarkEngineTest {

    @Test
    public void runComprehensiveBenchmark_populatesAllTimingsAndBoundedScores() {
        PerformanceMetrics metrics = new PerformanceMetrics();
        BenchmarkEngine engine = new BenchmarkEngine(metrics, "this is just a test string");

        AtomicInteger progressCalls = new AtomicInteger(0);
        AtomicInteger testCompleteCalls = new AtomicInteger(0);
        AtomicReference<PerformanceMetrics> completedRef = new AtomicReference<>();

        engine.setProgressCallback(new BenchmarkEngine.BenchmarkProgressCallback() {
            @Override
            public void onProgressUpdate(int progress, String currentTest) {
                progressCalls.incrementAndGet();
            }

            @Override
            public void onTestComplete(String testName, long duration) {
                testCompleteCalls.incrementAndGet();
            }

            @Override
            public void onBenchmarkComplete(PerformanceMetrics result) {
                completedRef.set(result);
            }
        });

        engine.runComprehensiveBenchmark();

        assertEquals("2.0.0", BenchmarkEngine.ENGINE_VERSION);
        assertEquals("2.0.0", metrics.getBenchmarkVersion());
        assertEquals("full", metrics.getRunType());
        assertEquals(9, progressCalls.get());
        assertEquals(9, testCompleteCalls.get());
        assertEquals(100, engine.getCurrentProgress());
        assertNotNull(completedRef.get());

        assertTrue(metrics.getSha1TotalTime() > 0);
        assertTrue(metrics.getMd5TotalTime() > 0);
        assertTrue(metrics.getAesTotalTime() > 0);
        assertTrue(metrics.getLoopOverheadTime() > 0);
        assertTrue(metrics.getMatrixMultiplicationTime() > 0);
        assertTrue(metrics.getSortingTime() > 0);
        assertTrue(metrics.getCompressionTime() > 0);
        assertTrue(metrics.getMemoryBandwidthTime() > 0);
        assertTrue(metrics.getMemoryBytesTransferred() > 0);
        assertTrue(metrics.getMultiThreadedTime() > 0);
        assertEquals(0L, metrics.getRsaTotalTime());

        assertBoundedScore(metrics.getOverallScore());
        assertBoundedScore(metrics.getCryptoScore());
        assertBoundedScore(metrics.getComputationalScore());
        assertBoundedScore(metrics.getMemoryScore());
        assertBoundedScore(metrics.getEfficiencyScore());
        assertBoundedScore(metrics.getStabilityScore());
        assertBoundedScore(metrics.getMultiThreadingScore());

        String formatted = metrics.getFormattedResults();
        assertTrue(formatted.contains("SHA-512"));
        assertTrue(formatted.contains("AES-256"));
        assertTrue(formatted.contains("ALU / FP Loop"));
    }

    @Test
    public void runLegacyBenchmark_resetsPreviousFullRunState() {
        PerformanceMetrics metrics = new PerformanceMetrics();
        BenchmarkEngine engine = new BenchmarkEngine(metrics, "this is just a test string");

        engine.runComprehensiveBenchmark();
        int firstSampleCount = metrics.getSampleCountForTest();
        assertTrue(firstSampleCount > 100);

        engine.runLegacyBenchmark();
        assertEquals("legacy", metrics.getRunType());
        assertTrue(metrics.getSha1TotalTime() > 0);
        assertTrue(metrics.getMd5TotalTime() > 0);
        assertEquals(0L, metrics.getMatrixMultiplicationTime());
        assertEquals(0L, metrics.getMemoryBandwidthTime());
        // Only SHA-512 (40) + MD5 (40) samples remain after resetForNewRun()
        assertEquals(80, metrics.getSampleCountForTest());
        assertBoundedScore(metrics.getOverallScore());
    }

    private static void assertBoundedScore(int score) {
        assertTrue("Score out of bounds [0, 100]: " + score, score >= 0 && score <= 100);
    }
}
