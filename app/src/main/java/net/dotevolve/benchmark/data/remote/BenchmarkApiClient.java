package net.dotevolve.benchmark.data.remote;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.appcheck.AppCheckToken;
import com.google.firebase.appcheck.FirebaseAppCheck;

import net.dotevolve.benchmark.BuildConfig;
import net.dotevolve.benchmark.core.PerformanceMetrics;
import net.dotevolve.benchmark.data.model.BenchmarkResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * HTTPS client for uploading benchmark results to Cloudflare Worker / D1 API.
 * Endpoint: POST /api/v1/benchmarks
 */
public class BenchmarkApiClient {

    private static final String TAG = "BenchmarkApiClient";
    private static final int DEFAULT_TIMEOUT_MS = 10_000;

    public enum ApiResultStatus {
        SUCCESS,
        PERMANENT_ERROR,
        TRANSIENT_ERROR
    }

    public static class ApiResult {
        @NonNull
        public final ApiResultStatus status;
        @Nullable
        public final String remoteId;
        public final int httpStatusCode;
        @Nullable
        public final String errorMessage;

        private ApiResult(@NonNull ApiResultStatus status, @Nullable String remoteId, int httpStatusCode, @Nullable String errorMessage) {
            this.status = status;
            this.remoteId = remoteId;
            this.httpStatusCode = httpStatusCode;
            this.errorMessage = errorMessage;
        }

        public static ApiResult success(@Nullable String remoteId, int httpStatusCode) {
            return new ApiResult(ApiResultStatus.SUCCESS, remoteId, httpStatusCode, null);
        }

        public static ApiResult permanentError(int httpStatusCode, @Nullable String message) {
            return new ApiResult(ApiResultStatus.PERMANENT_ERROR, null, httpStatusCode, message);
        }

        public static ApiResult transientError(int httpStatusCode, @Nullable String message) {
            return new ApiResult(ApiResultStatus.TRANSIENT_ERROR, null, httpStatusCode, message);
        }

        @NonNull
        @Override
        public String toString() {
            return "ApiResult{" +
                    "status=" + status +
                    ", remoteId='" + remoteId + '\'' +
                    ", httpStatusCode=" + httpStatusCode +
                    ", errorMessage='" + errorMessage + '\'' +
                    '}';
        }
    }

    private final String baseUrl;

    public BenchmarkApiClient() {
        this(BuildConfig.BENCHMARK_API_BASE_URL);
    }

    public BenchmarkApiClient(String baseUrl) {
        this.baseUrl = baseUrl != null ? baseUrl : BuildConfig.BENCHMARK_API_BASE_URL;
    }

    public ApiResult uploadBenchmark(PerformanceMetrics metrics, String clientRunId) {
        try {
            JSONObject payload = buildPayload(metrics, clientRunId);
            return executeUpload(payload);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to serialize metrics to JSON", e);
            return ApiResult.permanentError(-1, e.getMessage());
        }
    }

    public ApiResult uploadBenchmark(BenchmarkResult result) {
        try {
            JSONObject payload = buildPayload(result);
            return executeUpload(payload);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to serialize BenchmarkResult to JSON", e);
            return ApiResult.permanentError(-1, e.getMessage());
        }
    }

    public JSONObject buildPayload(PerformanceMetrics metrics, String clientRunId) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("clientRunId", clientRunId);
        putOptionalTrimmedString(json, "clientDeviceId", metrics.getClientDeviceId());
        json.put("runType", metrics.getRunType());
        json.put("deviceModel", metrics.getDeviceModel());
        putOptionalTrimmedString(json, "deviceManufacturer", metrics.getDeviceManufacturer());
        putOptionalTrimmedString(json, "deviceBrand", metrics.getDeviceBrand());
        json.put("androidVersion", metrics.getAndroidVersion());
        json.put("cpuCores", metrics.getCpuCores());
        json.put("totalMemory", metrics.getTotalMemory());
        json.put("architecture", metrics.getArchitecture());
        json.put("benchmarkVersion", metrics.getBenchmarkVersion());

        json.put("overallScore", metrics.getOverallScore());
        json.put("cryptoScore", metrics.getCryptoScore());
        json.put("computationalScore", metrics.getComputationalScore());
        json.put("memoryScore", metrics.getMemoryScore());
        json.put("efficiencyScore", metrics.getEfficiencyScore());
        json.put("stabilityScore", metrics.getStabilityScore());
        json.put("multiThreadingScore", metrics.getMultiThreadingScore());

        json.put("sha1TotalTime", metrics.getSha1TotalTime());
        json.put("md5TotalTime", metrics.getMd5TotalTime());
        json.put("aesTotalTime", metrics.getAesTotalTime());
        json.put("rsaTotalTime", metrics.getRsaTotalTime());
        json.put("loopOverheadTime", metrics.getLoopOverheadTime());
        json.put("matrixMultiplicationTime", metrics.getMatrixMultiplicationTime());
        json.put("sortingTime", metrics.getSortingTime());
        json.put("compressionTime", metrics.getCompressionTime());
        json.put("memoryBandwidthTime", metrics.getMemoryBandwidthTime());
        json.put("multiThreadedTime", metrics.getMultiThreadedTime());

        json.put("clientTimestampMs", System.currentTimeMillis());
        return json;
    }

    public JSONObject buildPayload(BenchmarkResult result) throws JSONException {
        JSONObject json = new JSONObject();
        String clientRunId = result.getClientRunId();
        if (clientRunId == null || clientRunId.trim().isEmpty()) {
            String seed = "benchmark-run-" + result.getId() + "-" + result.getTimestamp();
            clientRunId = java.util.UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString();
        }
        json.put("clientRunId", clientRunId);
        putOptionalTrimmedString(json, "clientDeviceId", result.getClientDeviceId());
        json.put("runType", result.getRunType());
        json.put("deviceModel", result.getDeviceModel());
        putOptionalTrimmedString(json, "deviceManufacturer", result.getDeviceManufacturer());
        putOptionalTrimmedString(json, "deviceBrand", result.getDeviceBrand());
        json.put("androidVersion", result.getAndroidVersion());
        json.put("cpuCores", result.getCpuCores());
        json.put("totalMemory", result.getTotalMemory());
        json.put("architecture", result.getArchitecture());
        json.put("benchmarkVersion", result.getBenchmarkVersion());

        json.put("overallScore", result.getOverallScore());
        json.put("cryptoScore", result.getCryptoScore());
        json.put("computationalScore", result.getComputationalScore());
        json.put("memoryScore", result.getMemoryScore());
        json.put("efficiencyScore", result.getEfficiencyScore());
        json.put("stabilityScore", result.getStabilityScore());
        json.put("multiThreadingScore", result.getMultiThreadingScore());

        json.put("sha1TotalTime", result.getSha1Time());
        json.put("md5TotalTime", result.getMd5Time());
        json.put("aesTotalTime", result.getAesTime());
        json.put("rsaTotalTime", result.getRsaTime());
        json.put("loopOverheadTime", result.getLoopTime());
        json.put("matrixMultiplicationTime", result.getMatrixTime());
        json.put("sortingTime", result.getSortTime());
        json.put("compressionTime", result.getCompressionTime());
        json.put("memoryBandwidthTime", result.getMemoryBandwidthTime());
        json.put("multiThreadedTime", result.getMultiThreadedTime());

        json.put("clientTimestampMs", result.getTimestamp() > 0 ? result.getTimestamp() : System.currentTimeMillis());
        return json;
    }

    private static void putOptionalTrimmedString(JSONObject json, String key, @Nullable String value) throws JSONException {
        if (value == null) {
            return;
        }
        String trimmed = value.trim();
        if (!trimmed.isEmpty()) {
            json.put(key, trimmed);
        }
    }

    public ApiResult executeUpload(JSONObject payload) {
        HttpURLConnection connection = null;
        try {
            String fullUrl = baseUrl.replaceAll("/+$", "") + "/api/v1/benchmarks";
            URL url = new URL(fullUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(DEFAULT_TIMEOUT_MS);
            connection.setReadTimeout(DEFAULT_TIMEOUT_MS);
            connection.setDoOutput(true);

            attachAppCheckToken(connection);

            byte[] jsonBytes = payload.toString().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(jsonBytes.length);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonBytes);
                os.flush();
            }

            int statusCode = connection.getResponseCode();
            if (statusCode == HttpURLConnection.HTTP_CREATED || statusCode == HttpURLConnection.HTTP_OK) {
                String responseBody = readStream(connection.getInputStream());
                String remoteId = parseRemoteId(responseBody, payload.optString("clientRunId", null));
                Log.d(TAG, "Uploaded benchmark successfully: status=" + statusCode + ", id=" + remoteId);
                return ApiResult.success(remoteId, statusCode);
            } else if (statusCode == 400 || statusCode == 401 || statusCode == 403 || statusCode == 422) {
                String errorBody = readStream(connection.getErrorStream());
                Log.e(TAG, "Permanent API error: status=" + statusCode + ", body=" + errorBody);
                return ApiResult.permanentError(statusCode, errorBody);
            } else if (statusCode == 429 || (statusCode >= 500 && statusCode <= 599)) {
                String errorBody = readStream(connection.getErrorStream());
                Log.w(TAG, "Transient API error: status=" + statusCode + ", body=" + errorBody);
                return ApiResult.transientError(statusCode, errorBody);
            } else {
                String errorBody = readStream(connection.getErrorStream());
                Log.w(TAG, "Unexpected API status: " + statusCode + ", body=" + errorBody);
                return ApiResult.transientError(statusCode, errorBody);
            }
        } catch (IOException e) {
            Log.w(TAG, "Network I/O error uploading benchmark: " + e.getMessage());
            return ApiResult.transientError(-1, e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error uploading benchmark", e);
            return ApiResult.transientError(-1, e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void attachAppCheckToken(HttpURLConnection connection) {
        try {
            FirebaseAppCheck appCheck = FirebaseAppCheck.getInstance();
            Task<AppCheckToken> tokenTask = appCheck.getAppCheckToken(false);
            AppCheckToken token = Tasks.await(tokenTask, 3, TimeUnit.SECONDS);
            if (token != null && token.getToken() != null && !token.getToken().isEmpty()) {
                connection.setRequestProperty("X-Firebase-AppCheck", token.getToken());
            }
        } catch (Throwable t) {
            Log.d(TAG, "Firebase App Check token not attached (non-fatal): " + t.getMessage());
        }
    }

    private String parseRemoteId(String responseBody, String fallbackId) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            return fallbackId;
        }
        try {
            JSONObject json = new JSONObject(responseBody);
            if (json.has("id")) {
                return json.getString("id");
            }
            if (json.has("clientRunId")) {
                return json.getString("clientRunId");
            }
        } catch (JSONException ignored) {}
        return fallbackId;
    }

    private String readStream(InputStream is) {
        if (is == null) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString().trim();
        } catch (IOException e) {
            return "";
        }
    }
}
