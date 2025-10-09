package net.dotevolve.benchmark;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

/**
 * Activity for displaying detailed benchmark result information
 */
public class ResultDetailActivity extends AppCompatActivity {
    
    private MaterialToolbar toolbar;
    private TextView detailText;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_detail);
        
        initializeViews();
        setupToolbar();
        displayResult();
    }
    
    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        detailText = findViewById(R.id.detailText);
    }
    
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Benchmark Details");
        }
    }
    
    private void displayResult() {
        BenchmarkResult result = (BenchmarkResult) getIntent().getSerializableExtra("benchmark_result");
        if (result == null) {
            detailText.setText("No result data available");
            return;
        }
        
        StringBuilder details = new StringBuilder();
        details.append("=== BENCHMARK RESULT DETAILS ===\n\n");
        
        details.append("📅 TIMESTAMP\n");
        details.append("Date: ").append(result.getFormattedDate()).append("\n");
        details.append("Time: ").append(result.getFormattedTime()).append("\n\n");
        
        details.append("📱 DEVICE INFORMATION\n");
        details.append("Model: ").append(result.getDeviceModel()).append("\n");
        details.append("Android: ").append(result.getAndroidVersion()).append("\n");
        details.append("CPU Cores: ").append(result.getCpuCores()).append("\n");
        details.append("Memory: ").append(result.getFormattedMemory()).append("\n\n");
        
        details.append("🎯 PERFORMANCE SCORES\n");
        details.append("Overall Score: ").append(result.getOverallScore()).append("/100 (")
                .append(result.getPerformanceCategory()).append(")\n");
        details.append("Crypto Performance: ").append(result.getCryptoScore()).append("/100\n");
        details.append("Efficiency: ").append(result.getEfficiencyScore()).append("/100\n");
        details.append("Stability: ").append(result.getStabilityScore()).append("/100\n\n");
        
        details.append("⏱️ TIMING RESULTS\n");
        details.append("SHA-1 Time: ").append(formatNanoTime(result.getSha1Time())).append("\n");
        details.append("MD5 Time: ").append(formatNanoTime(result.getMd5Time())).append("\n");
        details.append("AES Time: ").append(formatNanoTime(result.getAesTime())).append("\n");
        details.append("RSA Time: ").append(formatNanoTime(result.getRsaTime())).append("\n");
        details.append("Loop Time: ").append(formatNanoTime(result.getLoopTime())).append("\n\n");
        
        if (result.hasAdvancedMetrics()) {
            details.append("🔍 ADVANCED METRICS\n");
            details.append("CPU Temperature: ").append(result.getFormattedCpuTemperature()).append("\n");
            details.append("Battery Level: ").append(result.getFormattedBatteryLevel()).append("\n");
            details.append("Memory Usage: ").append(result.getFormattedMemoryUsage()).append("\n");
            details.append("Thermal Throttling: ").append(result.isThermalThrottling() ? "YES" : "NO").append("\n");
            details.append("Background Apps: ").append(result.getBackgroundAppsCount()).append("\n");
        }
        
        detailText.setText(details.toString());
    }
    
    private String formatNanoTime(long nanoTime) {
        if (nanoTime < 1_000) {
            return nanoTime + " ns";
        } else if (nanoTime < 1_000_000) {
            return String.format("%.2f μs", nanoTime / 1_000.0);
        } else if (nanoTime < 1_000_000_000) {
            return String.format("%.2f ms", nanoTime / 1_000_000.0);
        } else {
            return String.format("%.2f s", nanoTime / 1_000_000_000.0);
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
