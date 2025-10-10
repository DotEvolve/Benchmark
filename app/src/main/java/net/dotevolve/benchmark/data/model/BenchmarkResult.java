package net.dotevolve.benchmark.data.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import net.dotevolve.benchmark.core.PerformanceMetrics;
import net.dotevolve.benchmark.core.AdvancedMetrics;

/**
 * Data model for storing benchmark results
 */
public class BenchmarkResult implements Serializable {
    private long id;
    private long timestamp;
    private String deviceModel;
    private String androidVersion;
    private int cpuCores;
    private long totalMemory;
    private String architecture;
    
    // Performance scores
    private int overallScore;
    private int cryptoScore;
    private int efficiencyScore;
    private int stabilityScore;
    
    // Timing data
    private long sha1Time;
    private long md5Time;
    private long aesTime;
    private long rsaTime;
    private long loopTime;
    
    // Advanced metrics
    private double cpuTemperature = -1; // -1 means not available
    private int batteryLevel = -1; // -1 means not available
    private long memoryUsage = -1; // -1 means not available
    private boolean thermalThrottling = false;
    private int backgroundAppsCount = 0;
    
    // Constructors
    public BenchmarkResult() {}
    
    public BenchmarkResult(PerformanceMetrics metrics, AdvancedMetrics advancedMetrics) {
        this.timestamp = System.currentTimeMillis();
        this.deviceModel = metrics.getDeviceModel();
        this.cpuCores = metrics.getCpuCores();
        this.totalMemory = metrics.getTotalMemory();
        this.overallScore = metrics.getOverallScore();
        this.cryptoScore = metrics.getCryptoScore();
        this.efficiencyScore = metrics.getEfficiencyScore();
        this.stabilityScore = metrics.getStabilityScore();
        this.sha1Time = metrics.getSha1TotalTime();
        this.md5Time = metrics.getMd5TotalTime();
        this.aesTime = metrics.getAesTotalTime();
        this.rsaTime = metrics.getRsaTotalTime();
        this.loopTime = metrics.getLoopOverheadTime();
        
        if (advancedMetrics != null) {
            this.cpuTemperature = advancedMetrics.getCpuTemperature();
            this.batteryLevel = advancedMetrics.getBatteryLevel();
            this.memoryUsage = advancedMetrics.getMemoryUsage();
            this.thermalThrottling = advancedMetrics.isThermalThrottling();
            this.backgroundAppsCount = advancedMetrics.getBackgroundAppsCount();
        }
    }
    
    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public String getDeviceModel() { return deviceModel; }
    public void setDeviceModel(String deviceModel) { this.deviceModel = deviceModel; }
    
    public String getAndroidVersion() { return androidVersion; }
    public void setAndroidVersion(String androidVersion) { this.androidVersion = androidVersion; }
    
    public int getCpuCores() { return cpuCores; }
    public void setCpuCores(int cpuCores) { this.cpuCores = cpuCores; }
    
    public long getTotalMemory() { return totalMemory; }
    public void setTotalMemory(long totalMemory) { this.totalMemory = totalMemory; }
    
    public String getArchitecture() { return architecture; }
    public void setArchitecture(String architecture) { this.architecture = architecture; }
    
    public int getOverallScore() { return overallScore; }
    public void setOverallScore(int overallScore) { this.overallScore = overallScore; }
    
    public int getCryptoScore() { return cryptoScore; }
    public void setCryptoScore(int cryptoScore) { this.cryptoScore = cryptoScore; }
    
    public int getEfficiencyScore() { return efficiencyScore; }
    public void setEfficiencyScore(int efficiencyScore) { this.efficiencyScore = efficiencyScore; }
    
    public int getStabilityScore() { return stabilityScore; }
    public void setStabilityScore(int stabilityScore) { this.stabilityScore = stabilityScore; }
    
    public long getSha1Time() { return sha1Time; }
    public void setSha1Time(long sha1Time) { this.sha1Time = sha1Time; }
    
    public long getMd5Time() { return md5Time; }
    public void setMd5Time(long md5Time) { this.md5Time = md5Time; }
    
    public long getAesTime() { return aesTime; }
    public void setAesTime(long aesTime) { this.aesTime = aesTime; }
    
    public long getRsaTime() { return rsaTime; }
    public void setRsaTime(long rsaTime) { this.rsaTime = rsaTime; }
    
    public long getLoopTime() { return loopTime; }
    public void setLoopTime(long loopTime) { this.loopTime = loopTime; }
    
    public double getCpuTemperature() { return cpuTemperature; }
    public void setCpuTemperature(double cpuTemperature) { this.cpuTemperature = cpuTemperature; }
    
    public int getBatteryLevel() { return batteryLevel; }
    public void setBatteryLevel(int batteryLevel) { this.batteryLevel = batteryLevel; }
    
    public long getMemoryUsage() { return memoryUsage; }
    public void setMemoryUsage(long memoryUsage) { this.memoryUsage = memoryUsage; }
    
    public boolean isThermalThrottling() { return thermalThrottling; }
    public void setThermalThrottling(boolean thermalThrottling) { this.thermalThrottling = thermalThrottling; }
    
    public int getBackgroundAppsCount() { return backgroundAppsCount; }
    public void setBackgroundAppsCount(int backgroundAppsCount) { this.backgroundAppsCount = backgroundAppsCount; }
    
    // Utility methods
    public String getFormattedTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
    
    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
    
    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
    
    public String getPerformanceCategory() {
        if (overallScore >= 90) return "EXCELLENT";
        if (overallScore >= 70) return "GOOD";
        if (overallScore >= 50) return "AVERAGE";
        if (overallScore >= 30) return "BELOW_AVERAGE";
        return "POOR";
    }
    
    public String getFormattedMemory() {
        if (totalMemory < 1024) return totalMemory + " B";
        int exp = (int) (Math.log(totalMemory) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format(Locale.getDefault(), "%.1f %sB", totalMemory / Math.pow(1024, exp), pre);
    }
    
    public String getFormattedMemoryUsage() {
        if (memoryUsage == -1) return "N/A";
        if (memoryUsage < 1024) return memoryUsage + " B";
        int exp = (int) (Math.log(memoryUsage) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format(Locale.getDefault(), "%.1f %sB", memoryUsage / Math.pow(1024, exp), pre);
    }
    
    public String getFormattedCpuTemperature() {
        if (cpuTemperature == -1) return "N/A";
        return String.format(Locale.getDefault(), "%.1f°C", cpuTemperature);
    }
    
    public String getFormattedBatteryLevel() {
        if (batteryLevel == -1) return "N/A";
        return batteryLevel + "%";
    }
    
    public boolean hasAdvancedMetrics() {
        return cpuTemperature != -1 || batteryLevel != -1 || memoryUsage != -1;
    }
    
    @Override
    public String toString() {
        return "BenchmarkResult{" +
                "id=" + id +
                ", timestamp=" + getFormattedTimestamp() +
                ", deviceModel='" + deviceModel + '\'' +
                ", overallScore=" + overallScore +
                ", performanceCategory='" + getPerformanceCategory() + '\'' +
                '}';
    }
}
