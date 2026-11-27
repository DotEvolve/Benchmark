package net.dotevolve.benchmark.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PerformanceMetricsTest {

    @Test
    public void applyExponentialSkew_respectsBoundsAndPositiveSkew() {
        assertEquals(0, PerformanceMetrics.applyExponentialSkew(0.0, PerformanceMetrics.SCORE_CURVE_K));
        assertEquals(0, PerformanceMetrics.applyExponentialSkew(-0.25, PerformanceMetrics.SCORE_CURVE_K));
        assertEquals(0, PerformanceMetrics.applyExponentialSkew(Double.NaN, PerformanceMetrics.SCORE_CURVE_K));
        assertEquals(100, PerformanceMetrics.applyExponentialSkew(1.0, PerformanceMetrics.SCORE_CURVE_K));
        assertEquals(100, PerformanceMetrics.applyExponentialSkew(1.5, PerformanceMetrics.SCORE_CURVE_K));

        int s20 = PerformanceMetrics.applyExponentialSkew(0.20, PerformanceMetrics.SCORE_CURVE_K);
        int s35 = PerformanceMetrics.applyExponentialSkew(0.35, PerformanceMetrics.SCORE_CURVE_K);
        int s50 = PerformanceMetrics.applyExponentialSkew(0.50, PerformanceMetrics.SCORE_CURVE_K);
        int s65 = PerformanceMetrics.applyExponentialSkew(0.65, PerformanceMetrics.SCORE_CURVE_K);
        int s85 = PerformanceMetrics.applyExponentialSkew(0.85, PerformanceMetrics.SCORE_CURVE_K);
        int s95 = PerformanceMetrics.applyExponentialSkew(0.95, PerformanceMetrics.SCORE_CURVE_K);

        // Positive skew: midpoint 0.50 maps well above linear 50 (~75)
        assertTrue("Expected s50 >= 70, was " + s50, s50 >= 70 && s50 <= 80);
        // Strictly monotonic progression
        assertTrue(s20 < s35);
        assertTrue(s35 < s50);
        assertTrue(s50 < s65);
        assertTrue(s65 < s85);
        assertTrue(s85 < s95);
        assertTrue(s95 <= 100);
    }

    @Test
    public void hardwareTiers_showMotivatingSeparationAndOrdering() {
        PerformanceMetrics entry = new PerformanceMetrics();
        entry.setTimingsForTest(
                4_500_000_000L, // SHA-512: ~111K ops/sec
                2_800_000_000L, // MD5: ~178K ops/sec
                3_000_000_000L, // AES: ~16.7K ops/sec
                95_000_000L,    // Loop: 95 ms
                650_000_000L,   // Matrix: 650 ms
                45_000_000L,    // Sort: 45 ms
                1_500_000_000L, // Compression: ~133 ops/sec
                48_000_000L,    // Memory: 144 MB in 48 ms = 3000 MB/s
                144L * 1024L * 1024L,
                2_600_000_000L, // MT speedup ~1.73x
                4
        );
        addUniformSamples(entry, 100_000L, 12_000L);
        entry.calculateScores();

        PerformanceMetrics midrange = new PerformanceMetrics();
        midrange.setTimingsForTest(
                1_800_000_000L, // SHA-512: ~278K ops/sec
                1_100_000_000L, // MD5: ~455K ops/sec
                900_000_000L,   // AES: ~55K ops/sec
                38_000_000L,    // Loop: 38 ms
                180_000_000L,   // Matrix: 180 ms
                18_000_000L,    // Sort: 18 ms
                600_000_000L,   // Compression: ~333 ops/sec
                20_000_000L,    // Memory: 144 MB in 20 ms = 7200 MB/s
                144L * 1024L * 1024L,
                680_000_000L,   // MT speedup ~2.65x
                8
        );
        addUniformSamples(midrange, 100_000L, 6_000L);
        midrange.calculateScores();

        PerformanceMetrics flagship = new PerformanceMetrics();
        flagship.setTimingsForTest(
                650_000_000L,   // SHA-512: ~769K ops/sec
                350_000_000L,   // MD5: ~1.43M ops/sec
                280_000_000L,   // AES: ~178K ops/sec
                14_000_000L,    // Loop: 14 ms
                55_000_000L,    // Matrix: 55 ms
                6_000_000L,     // Sort: 6 ms
                220_000_000L,   // Compression: ~909 ops/sec
                8_000_000L,     // Memory: 144 MB in 8 ms = 18000 MB/s
                144L * 1024L * 1024L,
                160_000_000L,   // MT speedup ~4.06x
                8
        );
        addUniformSamples(flagship, 100_000L, 2_500L);
        flagship.calculateScores();

        assertTrue("Entry overall should be >= 55, got " + entry.getOverallScore(), entry.getOverallScore() >= 55);
        assertTrue("Midrange overall should be GOOD (>= 70), got " + midrange.getOverallScore(), midrange.getOverallScore() >= 70);
        assertTrue("Flagship overall should be EXCELLENT (>= 90), got " + flagship.getOverallScore(), flagship.getOverallScore() >= 90);
        assertTrue(entry.getOverallScore() < midrange.getOverallScore());
        assertTrue(midrange.getOverallScore() < flagship.getOverallScore());
    }

    @Test
    public void memoryScore_reflectsActualBytesTransferred() {
        PerformanceMetrics lowBytes = new PerformanceMetrics();
        lowBytes.setTimingsForTest(1_000_000_000L, 500_000_000L, 500_000_000L, 30_000_000L,
                150_000_000L, 15_000_000L, 400_000_000L, 10_000_000L,
                24L * 1024L * 1024L, 400_000_000L, 8);
        lowBytes.calculateScores();

        PerformanceMetrics highBytes = new PerformanceMetrics();
        highBytes.setTimingsForTest(1_000_000_000L, 500_000_000L, 500_000_000L, 30_000_000L,
                150_000_000L, 15_000_000L, 400_000_000L, 10_000_000L,
                144L * 1024L * 1024L, 400_000_000L, 8);
        highBytes.calculateScores();

        assertTrue(highBytes.getMemoryScore() > lowBytes.getMemoryScore());
    }

    @Test
    public void multiThreadingScore_rewardsTrueSpeedupAndPenalizesNoSpeedup() {
        PerformanceMetrics noSpeedup = new PerformanceMetrics();
        noSpeedup.setTimingsForTest(1_000_000_000L, 500_000_000L, 500_000_000L, 30_000_000L,
                150_000_000L, 15_000_000L, 400_000_000L, 20_000_000L,
                144L * 1024L * 1024L, 1_100_000_000L, 8);
        noSpeedup.calculateScores();

        PerformanceMetrics strongSpeedup = new PerformanceMetrics();
        strongSpeedup.setTimingsForTest(1_000_000_000L, 500_000_000L, 500_000_000L, 30_000_000L,
                150_000_000L, 15_000_000L, 400_000_000L, 20_000_000L,
                144L * 1024L * 1024L, 220_000_000L, 8);
        strongSpeedup.calculateScores();

        assertEquals(0, noSpeedup.getMultiThreadingScore());
        assertTrue(strongSpeedup.getMultiThreadingScore() >= 90);
    }

    @Test
    public void resetForNewRun_clearsSamplesTimingsScoresAndRunIds() {
        PerformanceMetrics metrics = new PerformanceMetrics();
        metrics.setTimingsForTest(1_000_000_000L, 500_000_000L, 500_000_000L, 30_000_000L,
                150_000_000L, 15_000_000L, 400_000_000L, 20_000_000L,
                144L * 1024L * 1024L, 250_000_000L, 8);
        addUniformSamples(metrics, 100_000L, 5_000L);
        metrics.ensureClientRunId();
        metrics.calculateScores();

        assertTrue(metrics.getSampleCountForTest() > 0);
        assertTrue(metrics.getOverallScore() > 0);

        metrics.resetForNewRun();

        assertEquals(0, metrics.getSampleCountForTest());
        assertEquals(0L, metrics.getSha1TotalTime());
        assertEquals(0L, metrics.getMemoryBytesTransferred());
        assertEquals(0, metrics.getOverallScore());
        assertNull(metrics.getClientRunId());
        assertNull(metrics.getRemoteId());
        assertEquals(0L, metrics.getRsaTotalTime());
    }

    @Test
    public void abortedTiming_doesNotLeakNanoTimestampIntoTotalTime() {
        PerformanceMetrics metrics = new PerformanceMetrics();
        metrics.startSha1Timing();
        metrics.startMd5Timing();
        metrics.startAesTiming();
        metrics.startLoopTiming();
        metrics.startMatrixTiming();
        metrics.startSortTiming();
        metrics.startCompressionTiming();
        metrics.startMemoryTiming();
        metrics.startMultiThreadedTiming();

        assertEquals(0L, metrics.getSha1TotalTime());
        assertEquals(0L, metrics.getMd5TotalTime());
        assertEquals(0L, metrics.getAesTotalTime());
        assertEquals(0L, metrics.getLoopOverheadTime());
        assertEquals(0L, metrics.getMatrixMultiplicationTime());
        assertEquals(0L, metrics.getSortingTime());
        assertEquals(0L, metrics.getCompressionTime());
        assertEquals(0L, metrics.getMemoryBandwidthTime());
        assertEquals(0L, metrics.getMultiThreadedTime());

        // Calling end without active start should also remain 0
        metrics.resetForNewRun();
        metrics.endAesTiming();
        assertEquals(0L, metrics.getAesTotalTime());
    }

    private static void addUniformSamples(PerformanceMetrics metrics, long base, long jitter) {
        for (int i = 0; i < 20; i++) {
            long delta = (i % 2 == 0) ? jitter : -jitter;
            metrics.addSha1Sample(base + delta);
            metrics.addMd5Sample(base + delta);
            metrics.addAesSample(base + delta);
            metrics.addCompressionSample(base + delta);
        }
    }
}
