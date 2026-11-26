package net.dotevolve.benchmark.data.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import net.dotevolve.benchmark.data.model.BenchmarkResult;

import org.json.JSONObject;
import org.junit.Test;

public class BenchmarkApiClientTest {

    @Test
    public void testBuildPayloadFromBenchmarkResult_containsAllRequiredContractKeys() throws Exception {
        BenchmarkResult result = new BenchmarkResult();
        result.setId(101L);
        result.setClientRunId("550e8400-e29b-41d4-a716-446655440000");
        result.setClientDeviceId("  device-install-123  ");
        result.setRunType("full");
        result.setDeviceModel("Pixel 8 Pro");
        result.setDeviceManufacturer("  Google  ");
        result.setDeviceBrand(" google ");
        result.setAndroidVersion("Android 14");
        result.setCpuCores(8);
        result.setTotalMemory(536870912L);
        result.setArchitecture("arm64-v8a");
        result.setBenchmarkVersion("2.0.0");
        result.setOverallScore(84);
        result.setCryptoScore(81);
        result.setComputationalScore(88);
        result.setMemoryScore(79);
        result.setEfficiencyScore(85);
        result.setStabilityScore(90);
        result.setMultiThreadingScore(82);
        result.setSha1Time(142500000L);
        result.setMd5Time(98200000L);
        result.setAesTime(65400000L);
        result.setRsaTime(0L);
        result.setLoopTime(12400000L);
        result.setMatrixTime(215000000L);
        result.setSortTime(42100000L);
        result.setCompressionTime(188000000L);
        result.setMemoryBandwidthTime(74300000L);
        result.setMultiThreadedTime(195000000L);
        result.setTimestamp(1795700000000L);

        BenchmarkApiClient client = new BenchmarkApiClient("https://benchmark.dotevolve.net");
        JSONObject payload = client.buildPayload(result);

        assertNotNull(payload);
        assertEquals("550e8400-e29b-41d4-a716-446655440000", payload.getString("clientRunId"));
        assertEquals("device-install-123", payload.getString("clientDeviceId"));
        assertEquals("full", payload.getString("runType"));
        assertEquals("Pixel 8 Pro", payload.getString("deviceModel"));
        assertEquals("Google", payload.getString("deviceManufacturer"));
        assertEquals("google", payload.getString("deviceBrand"));
        assertEquals("Android 14", payload.getString("androidVersion"));
        assertEquals(8, payload.getInt("cpuCores"));
        assertEquals(536870912L, payload.getLong("totalMemory"));
        assertEquals("arm64-v8a", payload.getString("architecture"));
        assertEquals("2.0.0", payload.getString("benchmarkVersion"));
        assertEquals(84, payload.getInt("overallScore"));
        assertEquals(81, payload.getInt("cryptoScore"));
        assertEquals(88, payload.getInt("computationalScore"));
        assertEquals(79, payload.getInt("memoryScore"));
        assertEquals(85, payload.getInt("efficiencyScore"));
        assertEquals(90, payload.getInt("stabilityScore"));
        assertEquals(82, payload.getInt("multiThreadingScore"));
        assertEquals(142500000L, payload.getLong("sha1TotalTime"));
        assertEquals(98200000L, payload.getLong("md5TotalTime"));
        assertEquals(65400000L, payload.getLong("aesTotalTime"));
        assertEquals(0L, payload.getLong("rsaTotalTime"));
        assertEquals(12400000L, payload.getLong("loopOverheadTime"));
        assertEquals(215000000L, payload.getLong("matrixMultiplicationTime"));
        assertEquals(42100000L, payload.getLong("sortingTime"));
        assertEquals(188000000L, payload.getLong("compressionTime"));
        assertEquals(74300000L, payload.getLong("memoryBandwidthTime"));
        assertEquals(195000000L, payload.getLong("multiThreadedTime"));
        assertEquals(1795700000000L, payload.getLong("clientTimestampMs"));
    }

    @Test
    public void testApiResultClassification() {
        BenchmarkApiClient.ApiResult success = BenchmarkApiClient.ApiResult.success("remote-123", 201);
        assertEquals(BenchmarkApiClient.ApiResultStatus.SUCCESS, success.status);
        assertEquals("remote-123", success.remoteId);
        assertEquals(201, success.httpStatusCode);

        BenchmarkApiClient.ApiResult permanent = BenchmarkApiClient.ApiResult.permanentError(400, "Bad Request");
        assertEquals(BenchmarkApiClient.ApiResultStatus.PERMANENT_ERROR, permanent.status);
        assertEquals(400, permanent.httpStatusCode);
        assertNotNull(permanent.errorMessage);
        assertTrue(permanent.errorMessage.contains("Bad Request"));

        BenchmarkApiClient.ApiResult transientErr = BenchmarkApiClient.ApiResult.transientError(429, "Rate Limited");
        assertEquals(BenchmarkApiClient.ApiResultStatus.TRANSIENT_ERROR, transientErr.status);
        assertEquals(429, transientErr.httpStatusCode);
        assertNotNull(transientErr.errorMessage);
        assertTrue(transientErr.errorMessage.contains("Rate Limited"));
    }

    @Test
    public void testBuildPayloadFromBenchmarkResult_missingClientRunId_isDeterministic() throws Exception {
        BenchmarkResult result = new BenchmarkResult();
        result.setId(42L);
        result.setTimestamp(1795700000000L);
        result.setRunType("full");
        result.setDeviceModel("Pixel 8 Pro");

        BenchmarkApiClient client = new BenchmarkApiClient("https://benchmark-dev.dotevolve.net");
        String firstId = client.buildPayload(result).getString("clientRunId");
        String secondId = client.buildPayload(result).getString("clientRunId");

        assertNotNull(firstId);
        assertEquals(firstId, secondId);
    }

    @Test
    public void testBuildPayloadFromBenchmarkResult_omitsBlankOrNullManufacturerAndBrand() throws Exception {
        BenchmarkResult result = new BenchmarkResult();
        result.setClientRunId("550e8400-e29b-41d4-a716-446655440001");
        result.setRunType("full");
        result.setDeviceModel("TB310XU");
        result.setDeviceManufacturer(null);
        result.setDeviceBrand("   ");
        result.setAndroidVersion("Android 13");
        result.setCpuCores(8);
        result.setTotalMemory(536870912L);
        result.setArchitecture("arm64-v8a");
        result.setBenchmarkVersion("2.0.0");

        BenchmarkApiClient client = new BenchmarkApiClient("https://benchmark-dev.dotevolve.net");
        JSONObject payload = client.buildPayload(result);

        assertEquals("TB310XU", payload.getString("deviceModel"));
        assertFalse(payload.has("clientDeviceId"));
        assertFalse(payload.has("deviceManufacturer"));
        assertFalse(payload.has("deviceBrand"));
    }
}
