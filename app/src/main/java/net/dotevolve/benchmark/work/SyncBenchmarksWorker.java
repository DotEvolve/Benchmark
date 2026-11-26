package net.dotevolve.benchmark.work;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.dotevolve.benchmark.data.db.PerformanceDatabaseHelper;
import net.dotevolve.benchmark.data.model.BenchmarkResult;
import net.dotevolve.benchmark.data.remote.BenchmarkApiClient;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * WorkManager worker for retrying pending benchmark uploads to Cloudflare API.
 */
public class SyncBenchmarksWorker extends Worker {

    public static final String UNIQUE_WORK_NAME = "sync_benchmarks_work";
    private static final String TAG = "SyncBenchmarksWorker";

    public SyncBenchmarksWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        PerformanceDatabaseHelper dbHelper = PerformanceDatabaseHelper.getInstance(context);
        List<BenchmarkResult> pending = dbHelper.getPendingSyncRuns(10);

        if (pending == null || pending.isEmpty()) {
            Log.d(TAG, "No pending benchmark runs to sync.");
            return Result.success();
        }

        Log.d(TAG, "Attempting to sync " + pending.size() + " pending benchmark runs.");
        BenchmarkApiClient client = new BenchmarkApiClient();
        boolean hasTransientError = false;

        for (BenchmarkResult result : pending) {
            if (result.getClientRunId() == null || result.getClientRunId().trim().isEmpty()) {
                String generatedId = java.util.UUID.randomUUID().toString();
                result.setClientRunId(generatedId);
                dbHelper.updateClientRunId(result.getId(), generatedId);
            }
            if (result.getDeviceManufacturer() == null || result.getDeviceManufacturer().trim().isEmpty()) {
                result.setDeviceManufacturer(Build.MANUFACTURER);
            }
            if (result.getDeviceBrand() == null || result.getDeviceBrand().trim().isEmpty()) {
                result.setDeviceBrand(Build.BRAND);
            }
            BenchmarkApiClient.ApiResult apiResult = client.uploadBenchmark(result);
            if (apiResult.status == BenchmarkApiClient.ApiResultStatus.SUCCESS) {
                dbHelper.markRunSynced(result.getId(), apiResult.remoteId);
                Log.d(TAG, "Successfully synced run " + result.getId() + " (remoteId=" + apiResult.remoteId + ")");
            } else if (apiResult.status == BenchmarkApiClient.ApiResultStatus.PERMANENT_ERROR) {
                dbHelper.markRunPermanentFailure(result.getId());
                Log.e(TAG, "Permanent failure syncing run " + result.getId() + ": " + apiResult.errorMessage);
            } else {
                hasTransientError = true;
                Log.w(TAG, "Transient failure syncing run " + result.getId() + ": " + apiResult.errorMessage);
            }
        }

        if (hasTransientError) {
            return Result.retry();
        }
        return Result.success();
    }

    public static void enqueue(Context context) {
        try {
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SyncBenchmarksWorker.class)
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                    .build();

            WorkManager.getInstance(context).enqueueUniqueWork(
                    UNIQUE_WORK_NAME,
                    ExistingWorkPolicy.APPEND_OR_REPLACE,
                    request
            );
            Log.d(TAG, "Enqueued SyncBenchmarksWorker for background upload");
        } catch (Exception e) {
            Log.e(TAG, "Failed to enqueue SyncBenchmarksWorker", e);
        }
    }
}
