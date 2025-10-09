package net.dotevolve.benchmark;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Data model for device statistics and summary information
 */
public class DeviceStatistics {
    private String deviceId;
    private String deviceModel;
    private long firstBenchmark;
    private long lastBenchmark;
    private int totalBenchmarks;
    private int bestScore;
    private int worstScore;
    
    // Constructors
    public DeviceStatistics() {}
    
    public DeviceStatistics(String deviceId, String deviceModel, long firstBenchmark, 
                           long lastBenchmark, int totalBenchmarks, int bestScore, int worstScore) {
        this.deviceId = deviceId;
        this.deviceModel = deviceModel;
        this.firstBenchmark = firstBenchmark;
        this.lastBenchmark = lastBenchmark;
        this.totalBenchmarks = totalBenchmarks;
        this.bestScore = bestScore;
        this.worstScore = worstScore;
    }
    
    // Getters and Setters
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    
    public String getDeviceModel() { return deviceModel; }
    public void setDeviceModel(String deviceModel) { this.deviceModel = deviceModel; }
    
    public long getFirstBenchmark() { return firstBenchmark; }
    public void setFirstBenchmark(long firstBenchmark) { this.firstBenchmark = firstBenchmark; }
    
    public long getLastBenchmark() { return lastBenchmark; }
    public void setLastBenchmark(long lastBenchmark) { this.lastBenchmark = lastBenchmark; }
    
    public int getTotalBenchmarks() { return totalBenchmarks; }
    public void setTotalBenchmarks(int totalBenchmarks) { this.totalBenchmarks = totalBenchmarks; }
    
    public int getBestScore() { return bestScore; }
    public void setBestScore(int bestScore) { this.bestScore = bestScore; }
    
    public int getWorstScore() { return worstScore; }
    public void setWorstScore(int worstScore) { this.worstScore = worstScore; }
    
    // Utility methods
    public String getFormattedFirstBenchmark() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new Date(firstBenchmark));
    }
    
    public String getFormattedLastBenchmark() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new Date(lastBenchmark));
    }
    
    public long getDaysSinceFirstBenchmark() {
        return (System.currentTimeMillis() - firstBenchmark) / (24 * 60 * 60 * 1000);
    }
    
    public long getDaysSinceLastBenchmark() {
        return (System.currentTimeMillis() - lastBenchmark) / (24 * 60 * 60 * 1000);
    }
    
    public int getScoreRange() {
        return bestScore - worstScore;
    }
    
    public double getAverageScore() {
        if (totalBenchmarks == 0) return 0;
        return (bestScore + worstScore) / 2.0; // Simplified - would need all scores for real average
    }
    
    public String getPerformanceConsistency() {
        int range = getScoreRange();
        if (range <= 10) return "Very Consistent";
        if (range <= 20) return "Consistent";
        if (range <= 30) return "Moderately Consistent";
        if (range <= 40) return "Variable";
        return "Highly Variable";
    }
    
    public String getBestScoreCategory() {
        if (bestScore >= 90) return "EXCELLENT";
        if (bestScore >= 70) return "GOOD";
        if (bestScore >= 50) return "AVERAGE";
        if (bestScore >= 30) return "BELOW_AVERAGE";
        return "POOR";
    }
    
    public String getWorstScoreCategory() {
        if (worstScore >= 90) return "EXCELLENT";
        if (worstScore >= 70) return "GOOD";
        if (worstScore >= 50) return "AVERAGE";
        if (worstScore >= 30) return "BELOW_AVERAGE";
        return "POOR";
    }
    
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        
        summary.append("📊 DEVICE PERFORMANCE SUMMARY\n\n");
        summary.append("Device: ").append(deviceModel).append("\n");
        summary.append("Total Benchmarks: ").append(totalBenchmarks).append("\n");
        summary.append("First Test: ").append(getFormattedFirstBenchmark()).append("\n");
        summary.append("Last Test: ").append(getFormattedLastBenchmark()).append("\n");
        summary.append("Days Since First: ").append(getDaysSinceFirstBenchmark()).append("\n");
        summary.append("Days Since Last: ").append(getDaysSinceLastBenchmark()).append("\n\n");
        
        summary.append("🎯 PERFORMANCE RANGE\n");
        summary.append("Best Score: ").append(bestScore).append("/100 (").append(getBestScoreCategory()).append(")\n");
        summary.append("Worst Score: ").append(worstScore).append("/100 (").append(getWorstScoreCategory()).append(")\n");
        summary.append("Score Range: ").append(getScoreRange()).append(" points\n");
        summary.append("Consistency: ").append(getPerformanceConsistency()).append("\n");
        
        return summary.toString();
    }
    
    @Override
    public String toString() {
        return "DeviceStatistics{" +
                "deviceId='" + deviceId + '\'' +
                ", deviceModel='" + deviceModel + '\'' +
                ", totalBenchmarks=" + totalBenchmarks +
                ", bestScore=" + bestScore +
                ", worstScore=" + worstScore +
                '}';
    }
}
