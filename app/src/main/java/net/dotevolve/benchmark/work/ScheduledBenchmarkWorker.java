package net.dotevolve.benchmark.work;
import net.dotevolve.benchmark.core.BenchmarkEngine;
import net.dotevolve.benchmark.core.PerformanceMetrics;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;

/**
 * Periodic worker that runs the benchmark in the background and saves results.
 */
public class ScheduledBenchmarkWorker extends Worker {

    public static final String UNIQUE_WORK_NAME = "daily_benchmark_work";
    private static final String TAG = "BenchmarkWorker";

    public ScheduledBenchmarkWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            // Run a lightweight benchmark in background (legacy or comprehensive with reduced iterations)
            BenchmarkEngine engine = new BenchmarkEngine(getApplicationContext());
            engine.runLegacyBenchmark();

            PerformanceMetrics metrics = engine.getMetrics();
            metrics.saveToHistory(getApplicationContext());

            Log.d(TAG, "Scheduled benchmark completed and saved.");
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Scheduled benchmark failed", e);
            return Result.retry();
        }
    }

    public static void scheduleDaily(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(true)
                .build();

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                ScheduledBenchmarkWorker.class,
                24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
        );
    }

    public static void cancel(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME);
    }
}


