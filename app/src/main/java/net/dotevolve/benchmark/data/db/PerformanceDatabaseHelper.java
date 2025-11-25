package net.dotevolve.benchmark.data.db;
import net.dotevolve.benchmark.data.model.BenchmarkResult;
import net.dotevolve.benchmark.data.model.PerformanceTrend;
import net.dotevolve.benchmark.data.model.DeviceStatistics;
import net.dotevolve.benchmark.core.PerformanceMetrics;
import net.dotevolve.benchmark.core.AdvancedMetrics;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Database helper for storing and retrieving historical performance data
 */
public class PerformanceDatabaseHelper extends SQLiteOpenHelper {
    
    private static final String TAG = "PerformanceDB";
    private static final String DATABASE_NAME = "benchmark_performance.db";
    private static final int DATABASE_VERSION = 2;
    
    // Table names
    private static final String TABLE_BENCHMARKS = "benchmarks";
    private static final String TABLE_PERFORMANCE_TRENDS = "performance_trends";
    private static final String TABLE_DEVICE_INFO = "device_info";
    
    // Benchmarks table columns
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_DEVICE_MODEL = "device_model";
    private static final String COLUMN_ANDROID_VERSION = "android_version";
    private static final String COLUMN_CPU_CORES = "cpu_cores";
    private static final String COLUMN_TOTAL_MEMORY = "total_memory";
    private static final String COLUMN_ARCHITECTURE = "architecture";
    
    // Performance scores
    private static final String COLUMN_OVERALL_SCORE = "overall_score";
    private static final String COLUMN_CRYPTO_SCORE = "crypto_score";
    private static final String COLUMN_EFFICIENCY_SCORE = "efficiency_score";
    private static final String COLUMN_STABILITY_SCORE = "stability_score";
    private static final String COLUMN_COMPUTATIONAL_SCORE = "computational_score";
    private static final String COLUMN_MEMORY_SCORE = "memory_score";
    private static final String COLUMN_MULTI_THREAD_SCORE = "multi_thread_score";
    
    // Timing data
    private static final String COLUMN_SHA1_TIME = "sha1_time";
    private static final String COLUMN_MD5_TIME = "md5_time";
    private static final String COLUMN_AES_TIME = "aes_time";
    private static final String COLUMN_RSA_TIME = "rsa_time";
    private static final String COLUMN_LOOP_TIME = "loop_time";
    private static final String COLUMN_MATRIX_TIME = "matrix_time";
    private static final String COLUMN_SORT_TIME = "sort_time";
    private static final String COLUMN_COMPRESSION_TIME = "compression_time";
    private static final String COLUMN_MEMORY_BANDWIDTH_TIME = "memory_bandwidth_time";
    private static final String COLUMN_MULTI_THREAD_TIME = "multi_thread_time";
    
    // Advanced metrics
    private static final String COLUMN_CPU_TEMPERATURE = "cpu_temperature";
    private static final String COLUMN_BATTERY_LEVEL = "battery_level";
    private static final String COLUMN_MEMORY_USAGE = "memory_usage";
    private static final String COLUMN_THERMAL_THROTTLING = "thermal_throttling";
    private static final String COLUMN_BACKGROUND_APPS = "background_apps_count";
    
    // Performance trends table columns
    private static final String COLUMN_TREND_DATE = "trend_date";
    private static final String COLUMN_AVERAGE_SCORE = "average_score";
    private static final String COLUMN_SCORE_VARIANCE = "score_variance";
    private static final String COLUMN_TEST_COUNT = "test_count";
    private static final String COLUMN_PERFORMANCE_TREND = "performance_trend"; // IMPROVING, DECLINING, STABLE
    
    // Device info table columns
    private static final String COLUMN_DEVICE_ID = "device_id";
    private static final String COLUMN_FIRST_BENCHMARK = "first_benchmark";
    private static final String COLUMN_LAST_BENCHMARK = "last_benchmark";
    private static final String COLUMN_TOTAL_BENCHMARKS = "total_benchmarks";
    private static final String COLUMN_BEST_SCORE = "best_score";
    private static final String COLUMN_WORST_SCORE = "worst_score";
    
    public PerformanceDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        createBenchmarksTable(db);
        createPerformanceTrendsTable(db);
        createDeviceInfoTable(db);
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
        // Handle database upgrades here
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BENCHMARKS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PERFORMANCE_TRENDS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DEVICE_INFO);
        onCreate(db);
    }
    
    private void createBenchmarksTable(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_BENCHMARKS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TIMESTAMP + " INTEGER NOT NULL, " +
                COLUMN_DEVICE_MODEL + " TEXT NOT NULL, " +
                COLUMN_ANDROID_VERSION + " TEXT, " +
                COLUMN_CPU_CORES + " INTEGER, " +
                COLUMN_TOTAL_MEMORY + " INTEGER, " +
                COLUMN_ARCHITECTURE + " TEXT, " +
                COLUMN_OVERALL_SCORE + " INTEGER NOT NULL, " +
                COLUMN_CRYPTO_SCORE + " INTEGER, " +
                COLUMN_EFFICIENCY_SCORE + " INTEGER, " +
                COLUMN_STABILITY_SCORE + " INTEGER, " +
                COLUMN_COMPUTATIONAL_SCORE + " INTEGER, " +
                COLUMN_MEMORY_SCORE + " INTEGER, " +
                COLUMN_MULTI_THREAD_SCORE + " INTEGER, " +
                COLUMN_SHA1_TIME + " INTEGER, " +
                COLUMN_MD5_TIME + " INTEGER, " +
                COLUMN_AES_TIME + " INTEGER, " +
                COLUMN_RSA_TIME + " INTEGER, " +
                COLUMN_LOOP_TIME + " INTEGER, " +
                COLUMN_MATRIX_TIME + " INTEGER, " +
                COLUMN_SORT_TIME + " INTEGER, " +
                COLUMN_COMPRESSION_TIME + " INTEGER, " +
                COLUMN_MEMORY_BANDWIDTH_TIME + " INTEGER, " +
                COLUMN_MULTI_THREAD_TIME + " INTEGER, " +
                COLUMN_CPU_TEMPERATURE + " REAL, " +
                COLUMN_BATTERY_LEVEL + " INTEGER, " +
                COLUMN_MEMORY_USAGE + " INTEGER, " +
                COLUMN_THERMAL_THROTTLING + " INTEGER DEFAULT 0, " +
                COLUMN_BACKGROUND_APPS + " INTEGER DEFAULT 0" +
                ")";
        db.execSQL(createTable);
        Log.d(TAG, "Created benchmarks table");
    }
    
    private void createPerformanceTrendsTable(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_PERFORMANCE_TRENDS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_DEVICE_MODEL + " TEXT NOT NULL, " +
                COLUMN_TREND_DATE + " TEXT NOT NULL, " +
                COLUMN_AVERAGE_SCORE + " REAL NOT NULL, " +
                COLUMN_SCORE_VARIANCE + " REAL, " +
                COLUMN_TEST_COUNT + " INTEGER NOT NULL, " +
                COLUMN_PERFORMANCE_TREND + " TEXT NOT NULL" +
                ")";
        db.execSQL(createTable);
        Log.d(TAG, "Created performance trends table");
    }
    
    private void createDeviceInfoTable(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_DEVICE_INFO + " (" +
                COLUMN_DEVICE_ID + " TEXT PRIMARY KEY, " +
                COLUMN_DEVICE_MODEL + " TEXT NOT NULL, " +
                COLUMN_FIRST_BENCHMARK + " INTEGER NOT NULL, " +
                COLUMN_LAST_BENCHMARK + " INTEGER NOT NULL, " +
                COLUMN_TOTAL_BENCHMARKS + " INTEGER DEFAULT 0, " +
                COLUMN_BEST_SCORE + " INTEGER DEFAULT 0, " +
                COLUMN_WORST_SCORE + " INTEGER DEFAULT 100" +
                ")";
        db.execSQL(createTable);
        Log.d(TAG, "Created device info table");
    }
    
    // Insert benchmark results
    public void insertBenchmarkResult(PerformanceMetrics metrics, AdvancedMetrics advancedMetrics) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        
        long timestamp = System.currentTimeMillis();
        
        values.put(COLUMN_TIMESTAMP, timestamp);
        values.put(COLUMN_DEVICE_MODEL, metrics.getDeviceModel());
        values.put(COLUMN_ANDROID_VERSION, metrics.getAndroidVersion());
        values.put(COLUMN_CPU_CORES, metrics.getCpuCores());
        values.put(COLUMN_TOTAL_MEMORY, metrics.getTotalMemory());
        values.put(COLUMN_ARCHITECTURE, metrics.getArchitecture());
        
        values.put(COLUMN_OVERALL_SCORE, metrics.getOverallScore());
        values.put(COLUMN_CRYPTO_SCORE, metrics.getCryptoScore());
        values.put(COLUMN_EFFICIENCY_SCORE, metrics.getEfficiencyScore());
        values.put(COLUMN_STABILITY_SCORE, metrics.getStabilityScore());
        values.put(COLUMN_COMPUTATIONAL_SCORE, metrics.getComputationalScore());
        values.put(COLUMN_MEMORY_SCORE, metrics.getMemoryScore());
        values.put(COLUMN_MULTI_THREAD_SCORE, metrics.getMultiThreadingScore());
        
        values.put(COLUMN_SHA1_TIME, metrics.getSha1TotalTime());
        values.put(COLUMN_MD5_TIME, metrics.getMd5TotalTime());
        values.put(COLUMN_AES_TIME, metrics.getAesTotalTime());
        values.put(COLUMN_RSA_TIME, metrics.getRsaTotalTime());
        values.put(COLUMN_LOOP_TIME, metrics.getLoopOverheadTime());
        values.put(COLUMN_MATRIX_TIME, metrics.getMatrixMultiplicationTime());
        values.put(COLUMN_SORT_TIME, metrics.getSortingTime());
        values.put(COLUMN_COMPRESSION_TIME, metrics.getCompressionTime());
        values.put(COLUMN_MEMORY_BANDWIDTH_TIME, metrics.getMemoryBandwidthTime());
        values.put(COLUMN_MULTI_THREAD_TIME, metrics.getMultiThreadedTime());
        
        if (advancedMetrics != null) {
            values.put(COLUMN_CPU_TEMPERATURE, advancedMetrics.getCpuTemperature());
            values.put(COLUMN_BATTERY_LEVEL, advancedMetrics.getBatteryLevel());
            values.put(COLUMN_MEMORY_USAGE, advancedMetrics.getMemoryUsage());
            values.put(COLUMN_THERMAL_THROTTLING, advancedMetrics.isThermalThrottling() ? 1 : 0);
            values.put(COLUMN_BACKGROUND_APPS, advancedMetrics.getBackgroundAppsCount());
        }
        
        long result = db.insert(TABLE_BENCHMARKS, null, values);
        db.close();
        
        if (result != -1) {
            updateDeviceInfo(metrics.getDeviceModel(), timestamp, metrics.getOverallScore());
            updatePerformanceTrends(metrics.getDeviceModel(), timestamp, metrics.getOverallScore());
        }
    }
    
    // Get all benchmark results for a device
    public List<BenchmarkResult> getAllBenchmarkResults(String deviceModel) {
        List<BenchmarkResult> results = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        String query = "SELECT * FROM " + TABLE_BENCHMARKS + 
                      " WHERE " + COLUMN_DEVICE_MODEL + " = ? " +
                      " ORDER BY " + COLUMN_TIMESTAMP + " DESC";
        
        Cursor cursor = db.rawQuery(query, new String[]{deviceModel});
        
        if (cursor.moveToFirst()) {
            do {
                BenchmarkResult result = createBenchmarkResultFromCursor(cursor);
                results.add(result);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        db.close();
        return results;
    }
    
    // Get recent benchmark results (last N days)
    public List<BenchmarkResult> getRecentBenchmarkResults(String deviceModel, int days) {
        List<BenchmarkResult> results = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        long cutoffTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L);
        
        String query = "SELECT * FROM " + TABLE_BENCHMARKS + 
                      " WHERE " + COLUMN_DEVICE_MODEL + " = ? AND " + COLUMN_TIMESTAMP + " > ? " +
                      " ORDER BY " + COLUMN_TIMESTAMP + " DESC";
        
        Cursor cursor = db.rawQuery(query, new String[]{deviceModel, String.valueOf(cutoffTime)});
        
        if (cursor.moveToFirst()) {
            do {
                BenchmarkResult result = createBenchmarkResultFromCursor(cursor);
                results.add(result);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        db.close();
        return results;
    }
    
    // Get performance trends
    public List<PerformanceTrend> getPerformanceTrends(String deviceModel, int days) {
        List<PerformanceTrend> trends = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        long cutoffTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L);
        
        String query = "SELECT * FROM " + TABLE_PERFORMANCE_TRENDS + 
                      " WHERE " + COLUMN_DEVICE_MODEL + " = ? AND " + COLUMN_TREND_DATE + " > ? " +
                      " ORDER BY " + COLUMN_TREND_DATE + " ASC";
        
        Cursor cursor = db.rawQuery(query, new String[]{deviceModel, String.valueOf(cutoffTime)});
        
        if (cursor.moveToFirst()) {
            do {
                PerformanceTrend trend = createPerformanceTrendFromCursor(cursor);
                trends.add(trend);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        db.close();
        return trends;
    }
    
    // Get device statistics
    public DeviceStatistics getDeviceStatistics(String deviceModel) {
        SQLiteDatabase db = this.getReadableDatabase();
        
        String query = "SELECT * FROM " + TABLE_DEVICE_INFO + 
                      " WHERE " + COLUMN_DEVICE_MODEL + " = ?";
        
        Cursor cursor = db.rawQuery(query, new String[]{deviceModel});
        
        DeviceStatistics stats = null;
        if (cursor.moveToFirst()) {
            stats = createDeviceStatisticsFromCursor(cursor);
        }
        
        cursor.close();
        db.close();
        return stats;
    }
    
    // Update device info
    private void updateDeviceInfo(String deviceModel, long timestamp, int score) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        // Check if device exists
        String query = "SELECT * FROM " + TABLE_DEVICE_INFO + 
                      " WHERE " + COLUMN_DEVICE_MODEL + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{deviceModel});
        
        if (cursor.moveToFirst()) {
            // Update existing device
            ContentValues values = new ContentValues();
            values.put(COLUMN_LAST_BENCHMARK, timestamp);
            values.put(COLUMN_TOTAL_BENCHMARKS, cursor.getInt(cursor.getColumnIndex(COLUMN_TOTAL_BENCHMARKS)) + 1);
            
            int bestScore = cursor.getInt(cursor.getColumnIndex(COLUMN_BEST_SCORE));
            int worstScore = cursor.getInt(cursor.getColumnIndex(COLUMN_WORST_SCORE));
            
            if (score > bestScore) values.put(COLUMN_BEST_SCORE, score);
            if (score < worstScore) values.put(COLUMN_WORST_SCORE, score);
            
            db.update(TABLE_DEVICE_INFO, values, COLUMN_DEVICE_MODEL + " = ?", new String[]{deviceModel});
        } else {
            // Insert new device
            ContentValues values = new ContentValues();
            values.put(COLUMN_DEVICE_ID, deviceModel + "_" + timestamp);
            values.put(COLUMN_DEVICE_MODEL, deviceModel);
            values.put(COLUMN_FIRST_BENCHMARK, timestamp);
            values.put(COLUMN_LAST_BENCHMARK, timestamp);
            values.put(COLUMN_TOTAL_BENCHMARKS, 1);
            values.put(COLUMN_BEST_SCORE, score);
            values.put(COLUMN_WORST_SCORE, score);
            
            db.insert(TABLE_DEVICE_INFO, null, values);
        }
        
        cursor.close();
        db.close();
    }
    
    // Update performance trends (simplified - would need more complex logic for real trends)
    private void updatePerformanceTrends(String deviceModel, long timestamp, int score) {
        // This is a simplified implementation
        // In a real app, you'd want more sophisticated trend analysis
        SQLiteDatabase db = this.getWritableDatabase();

        SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
        String date = sdf.format(new Date(timestamp));
        
        // Check if trend exists for this date
        String query = "SELECT * FROM " + TABLE_PERFORMANCE_TRENDS + 
                      " WHERE " + COLUMN_DEVICE_MODEL + " = ? AND " + COLUMN_TREND_DATE + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{deviceModel, date});
        
        if (cursor.moveToFirst()) {
            // Update existing trend
            int testCount = cursor.getInt(cursor.getColumnIndex(COLUMN_TEST_COUNT)) + 1;
            double currentAvg = cursor.getDouble(cursor.getColumnIndex(COLUMN_AVERAGE_SCORE));
            double newAvg = (currentAvg * (testCount - 1) + score) / testCount;
            
            ContentValues values = new ContentValues();
            values.put(COLUMN_AVERAGE_SCORE, newAvg);
            values.put(COLUMN_TEST_COUNT, testCount);
            values.put(COLUMN_PERFORMANCE_TREND, calculateTrend(newAvg, currentAvg));
            
            db.update(TABLE_PERFORMANCE_TRENDS, values, 
                     COLUMN_DEVICE_MODEL + " = ? AND " + COLUMN_TREND_DATE + " = ?", 
                     new String[]{deviceModel, date});
        } else {
            // Insert new trend
            ContentValues values = new ContentValues();
            values.put(COLUMN_DEVICE_MODEL, deviceModel);
            values.put(COLUMN_TREND_DATE, date);
            values.put(COLUMN_AVERAGE_SCORE, score);
            values.put(COLUMN_SCORE_VARIANCE, 0.0);
            values.put(COLUMN_TEST_COUNT, 1);
            values.put(COLUMN_PERFORMANCE_TREND, "STABLE");
            
            db.insert(TABLE_PERFORMANCE_TRENDS, null, values);
        }
        
        cursor.close();
        db.close();
    }
    
    private String calculateTrend(double newAvg, double oldAvg) {
        double change = newAvg - oldAvg;
        if (change > 2) return "IMPROVING";
        if (change < -2) return "DECLINING";
        return "STABLE";
    }
    
    // Helper methods to create objects from cursor
    private BenchmarkResult createBenchmarkResultFromCursor(Cursor cursor) {
        BenchmarkResult result = new BenchmarkResult();
        result.setId(cursor.getLong(cursor.getColumnIndex(COLUMN_ID)));
        result.setTimestamp(cursor.getLong(cursor.getColumnIndex(COLUMN_TIMESTAMP)));
        result.setDeviceModel(cursor.getString(cursor.getColumnIndex(COLUMN_DEVICE_MODEL)));
        result.setAndroidVersion(cursor.getString(cursor.getColumnIndex(COLUMN_ANDROID_VERSION)));
        result.setCpuCores(cursor.getInt(cursor.getColumnIndex(COLUMN_CPU_CORES)));
        result.setTotalMemory(cursor.getLong(cursor.getColumnIndex(COLUMN_TOTAL_MEMORY)));
        result.setArchitecture(cursor.getString(cursor.getColumnIndex(COLUMN_ARCHITECTURE)));
        result.setOverallScore(cursor.getInt(cursor.getColumnIndex(COLUMN_OVERALL_SCORE)));
        result.setCryptoScore(cursor.getInt(cursor.getColumnIndex(COLUMN_CRYPTO_SCORE)));
        result.setEfficiencyScore(cursor.getInt(cursor.getColumnIndex(COLUMN_EFFICIENCY_SCORE)));
        result.setStabilityScore(cursor.getInt(cursor.getColumnIndex(COLUMN_STABILITY_SCORE)));
        result.setComputationalScore(cursor.getInt(cursor.getColumnIndex(COLUMN_COMPUTATIONAL_SCORE)));
        result.setMemoryScore(cursor.getInt(cursor.getColumnIndex(COLUMN_MEMORY_SCORE)));
        result.setMultiThreadingScore(cursor.getInt(cursor.getColumnIndex(COLUMN_MULTI_THREAD_SCORE)));
        result.setSha1Time(cursor.getLong(cursor.getColumnIndex(COLUMN_SHA1_TIME)));
        result.setMd5Time(cursor.getLong(cursor.getColumnIndex(COLUMN_MD5_TIME)));
        result.setAesTime(cursor.getLong(cursor.getColumnIndex(COLUMN_AES_TIME)));
        result.setRsaTime(cursor.getLong(cursor.getColumnIndex(COLUMN_RSA_TIME)));
        result.setLoopTime(cursor.getLong(cursor.getColumnIndex(COLUMN_LOOP_TIME)));
        result.setMatrixTime(cursor.getLong(cursor.getColumnIndex(COLUMN_MATRIX_TIME)));
        result.setSortTime(cursor.getLong(cursor.getColumnIndex(COLUMN_SORT_TIME)));
        result.setCompressionTime(cursor.getLong(cursor.getColumnIndex(COLUMN_COMPRESSION_TIME)));
        result.setMemoryBandwidthTime(cursor.getLong(cursor.getColumnIndex(COLUMN_MEMORY_BANDWIDTH_TIME)));
        result.setMultiThreadedTime(cursor.getLong(cursor.getColumnIndex(COLUMN_MULTI_THREAD_TIME)));
        
        // Advanced metrics
        if (!cursor.isNull(cursor.getColumnIndex(COLUMN_CPU_TEMPERATURE))) {
            result.setCpuTemperature(cursor.getDouble(cursor.getColumnIndex(COLUMN_CPU_TEMPERATURE)));
        }
        if (!cursor.isNull(cursor.getColumnIndex(COLUMN_BATTERY_LEVEL))) {
            result.setBatteryLevel(cursor.getInt(cursor.getColumnIndex(COLUMN_BATTERY_LEVEL)));
        }
        if (!cursor.isNull(cursor.getColumnIndex(COLUMN_MEMORY_USAGE))) {
            result.setMemoryUsage(cursor.getLong(cursor.getColumnIndex(COLUMN_MEMORY_USAGE)));
        }
        result.setThermalThrottling(cursor.getInt(cursor.getColumnIndex(COLUMN_THERMAL_THROTTLING)) == 1);
        result.setBackgroundAppsCount(cursor.getInt(cursor.getColumnIndex(COLUMN_BACKGROUND_APPS)));
        
        return result;
    }
    
    private PerformanceTrend createPerformanceTrendFromCursor(Cursor cursor) {
        PerformanceTrend trend = new PerformanceTrend();
        trend.setId(cursor.getLong(cursor.getColumnIndex(COLUMN_ID)));
        trend.setDeviceModel(cursor.getString(cursor.getColumnIndex(COLUMN_DEVICE_MODEL)));
        trend.setTrendDate(cursor.getString(cursor.getColumnIndex(COLUMN_TREND_DATE)));
        trend.setAverageScore(cursor.getDouble(cursor.getColumnIndex(COLUMN_AVERAGE_SCORE)));
        trend.setScoreVariance(cursor.getDouble(cursor.getColumnIndex(COLUMN_SCORE_VARIANCE)));
        trend.setTestCount(cursor.getInt(cursor.getColumnIndex(COLUMN_TEST_COUNT)));
        trend.setPerformanceTrend(cursor.getString(cursor.getColumnIndex(COLUMN_PERFORMANCE_TREND)));
        return trend;
    }
    
    private DeviceStatistics createDeviceStatisticsFromCursor(Cursor cursor) {
        DeviceStatistics stats = new DeviceStatistics();
        stats.setDeviceId(cursor.getString(cursor.getColumnIndex(COLUMN_DEVICE_ID)));
        stats.setDeviceModel(cursor.getString(cursor.getColumnIndex(COLUMN_DEVICE_MODEL)));
        stats.setFirstBenchmark(cursor.getLong(cursor.getColumnIndex(COLUMN_FIRST_BENCHMARK)));
        stats.setLastBenchmark(cursor.getLong(cursor.getColumnIndex(COLUMN_LAST_BENCHMARK)));
        stats.setTotalBenchmarks(cursor.getInt(cursor.getColumnIndex(COLUMN_TOTAL_BENCHMARKS)));
        stats.setBestScore(cursor.getInt(cursor.getColumnIndex(COLUMN_BEST_SCORE)));
        stats.setWorstScore(cursor.getInt(cursor.getColumnIndex(COLUMN_WORST_SCORE)));
        return stats;
    }
}
