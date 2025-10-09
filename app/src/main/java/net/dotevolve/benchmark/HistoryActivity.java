package net.dotevolve.benchmark;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for viewing historical performance data and trends
 */
public class HistoryActivity extends AppCompatActivity implements BenchmarkResultAdapter.OnResultClickListener {
    
    private static final String TAG = "HistoryActivity";
    
    private MaterialToolbar toolbar;
    private TextView deviceStatsText;
    private TextView trendsText;
    private TextView analysisText;
    private Spinner timeRangeSpinner;
    private RecyclerView resultsRecyclerView;
    
    private BenchmarkResultAdapter adapter;
    private List<BenchmarkResult> currentResults = new ArrayList<>();
    private String deviceModel;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        
        deviceModel = android.os.Build.MODEL;
        
        initializeViews();
        setupToolbar();
        setupSpinner();
        setupRecyclerView();
        loadData();
    }
    
    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        deviceStatsText = findViewById(R.id.deviceStatsText);
        trendsText = findViewById(R.id.trendsText);
        analysisText = findViewById(R.id.analysisText);
        timeRangeSpinner = findViewById(R.id.timeRangeSpinner);
        resultsRecyclerView = findViewById(R.id.resultsRecyclerView);
    }
    
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Performance History");
        }
    }
    
    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.time_range_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timeRangeSpinner.setAdapter(adapter);
        
        timeRangeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                int days = getDaysFromPosition(position);
                loadRecentResults(days);
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
    
    private void setupRecyclerView() {
        adapter = new BenchmarkResultAdapter(currentResults);
        adapter.setOnResultClickListener(this);
        resultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        resultsRecyclerView.setAdapter(adapter);
    }
    
    private void loadData() {
        loadDeviceStatistics();
        loadPerformanceTrends();
        loadRecentResults(7); // Default to last 7 days
    }
    
    private void loadDeviceStatistics() {
        new Thread(() -> {
            try {
                DeviceStatistics stats = PerformanceMetrics.getDeviceStatistics(this, deviceModel);
                runOnUiThread(() -> {
                    if (stats != null) {
                        deviceStatsText.setText(stats.getSummary());
                    } else {
                        deviceStatsText.setText("No statistics available. Run a benchmark first!");
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed to load device statistics", e);
                runOnUiThread(() -> deviceStatsText.setText("Error loading statistics"));
            }
        }).start();
    }
    
    private void loadPerformanceTrends() {
        new Thread(() -> {
            try {
                List<PerformanceTrend> trends = PerformanceMetrics.getPerformanceTrends(this, deviceModel, 30);
                runOnUiThread(() -> {
                    if (trends != null && !trends.isEmpty()) {
                        StringBuilder trendsText = new StringBuilder();
                        for (PerformanceTrend trend : trends) {
                            trendsText.append(trend.getTrendIcon()).append(" ")
                                    .append(trend.getTrendDate()).append(": ")
                                    .append(trend.getFormattedAverageScore()).append("/100 (")
                                    .append(trend.getPerformanceTrend()).append(")\n");
                        }
                        this.trendsText.setText(trendsText.toString());
                    } else {
                        this.trendsText.setText("No trend data available. Run more benchmarks!");
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed to load performance trends", e);
                runOnUiThread(() -> trendsText.setText("Error loading trends"));
            }
        }).start();
    }
    
    private void loadRecentResults(int days) {
        new Thread(() -> {
            try {
                List<BenchmarkResult> results;
                if (days == -1) {
                    results = PerformanceMetrics.getHistoricalResults(this, deviceModel);
                } else {
                    results = PerformanceMetrics.getRecentResults(this, deviceModel, days);
                }
                
                runOnUiThread(() -> {
                    currentResults.clear();
                    if (results != null) {
                        currentResults.addAll(results);
                    }
                    adapter.updateResults(currentResults);
                    loadPerformanceAnalysis();
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed to load recent results", e);
                runOnUiThread(() -> {
                    currentResults.clear();
                    adapter.updateResults(currentResults);
                });
            }
        }).start();
    }
    
    private void loadPerformanceAnalysis() {
        if (currentResults.isEmpty()) {
            analysisText.setText("No data available for analysis");
            return;
        }
        
        // Calculate basic analysis
        int totalResults = currentResults.size();
        int totalScore = 0;
        int bestScore = 0;
        int worstScore = 100;
        int excellentCount = 0;
        int goodCount = 0;
        int averageCount = 0;
        int belowAverageCount = 0;
        int poorCount = 0;
        
        for (BenchmarkResult result : currentResults) {
            int score = result.getOverallScore();
            totalScore += score;
            bestScore = Math.max(bestScore, score);
            worstScore = Math.min(worstScore, score);
            
            String category = result.getPerformanceCategory();
            switch (category) {
                case "EXCELLENT": excellentCount++; break;
                case "GOOD": goodCount++; break;
                case "AVERAGE": averageCount++; break;
                case "BELOW_AVERAGE": belowAverageCount++; break;
                case "POOR": poorCount++; break;
            }
        }
        
        double averageScore = (double) totalScore / totalResults;
        int scoreRange = bestScore - worstScore;
        
        StringBuilder analysis = new StringBuilder();
        analysis.append("📊 PERFORMANCE ANALYSIS\n\n");
        analysis.append("Total Tests: ").append(totalResults).append("\n");
        analysis.append("Average Score: ").append(String.format("%.1f", averageScore)).append("/100\n");
        analysis.append("Best Score: ").append(bestScore).append("/100\n");
        analysis.append("Worst Score: ").append(worstScore).append("/100\n");
        analysis.append("Score Range: ").append(scoreRange).append(" points\n\n");
        
        analysis.append("📈 PERFORMANCE DISTRIBUTION\n");
        analysis.append("Excellent (90+): ").append(excellentCount).append(" (").append(String.format("%.1f", (double) excellentCount / totalResults * 100)).append("%)\n");
        analysis.append("Good (70-89): ").append(goodCount).append(" (").append(String.format("%.1f", (double) goodCount / totalResults * 100)).append("%)\n");
        analysis.append("Average (50-69): ").append(averageCount).append(" (").append(String.format("%.1f", (double) averageCount / totalResults * 100)).append("%)\n");
        analysis.append("Below Average (30-49): ").append(belowAverageCount).append(" (").append(String.format("%.1f", (double) belowAverageCount / totalResults * 100)).append("%)\n");
        analysis.append("Poor (0-29): ").append(poorCount).append(" (").append(String.format("%.1f", (double) poorCount / totalResults * 100)).append("%)\n\n");
        
        // Performance consistency analysis
        if (scoreRange <= 10) {
            analysis.append("🎯 Performance is very consistent!");
        } else if (scoreRange <= 20) {
            analysis.append("✅ Performance is generally consistent");
        } else if (scoreRange <= 30) {
            analysis.append("⚠️ Performance shows moderate variation");
        } else {
            analysis.append("🔍 Performance shows significant variation - investigate potential causes");
        }
        
        analysisText.setText(analysis.toString());
    }
    
    private int getDaysFromPosition(int position) {
        switch (position) {
            case 0: return 7;
            case 1: return 30;
            case 2: return 90;
            case 3: return -1; // All time
            default: return 7;
        }
    }
    
    @Override
    public void onResultClick(BenchmarkResult result) {
        // Show detailed result dialog
        Intent intent = new Intent(this, ResultDetailActivity.class);
        intent.putExtra("benchmark_result", result);
        startActivity(intent);
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
