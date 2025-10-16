package net.dotevolve.benchmark.core;

import android.content.Context;
import android.os.BatteryManager;
import android.os.Debug;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Advanced metrics collection for thermal, battery, memory, and system analysis
 */
public class AdvancedMetrics {
    private static final String TAG = "AdvancedMetrics";
    
    // Thermal monitoring
    private double cpuTemperature = -1; // -1 means not available
    private boolean thermalThrottling = false;
    private double thermalThrottleThreshold = 80.0; // Celsius
    
    // Battery monitoring
    private int batteryLevel = -1; // -1 means not available
    private boolean isCharging = false;
    private int batteryHealth = -1; // -1 means not available
    
    // Memory monitoring
    private long totalMemory = 0;
    private long usedMemory = 0;
    private long freeMemory = 0;
    private long memoryUsage = 0;
    
    // System monitoring
    private int backgroundAppsCount = 0;
    private double cpuUsage = -1; // -1 means not available
    private long availableStorage = 0;
    private long totalStorage = 0;
    
    // Performance analysis
    private boolean performanceDegraded = false;
    private String degradationReason = "";
    private double performanceScore = 0.0;
    
    private Context context;
    
    public AdvancedMetrics(Context context) {
        this.context = context;
        collectAllMetrics();
    }
    
    private void collectAllMetrics() {
        collectThermalMetrics();
        collectBatteryMetrics();
        collectMemoryMetrics();
        collectSystemMetrics();
        analyzePerformance();
    }
    
    private void collectThermalMetrics() {
        try {
            // Try to read CPU temperature from thermal zones
            // This is device-specific and may not work on all devices
            String[] thermalZones = {
                "/sys/class/thermal/thermal_zone0/temp",
                "/sys/class/thermal/thermal_zone1/temp",
                "/sys/class/thermal/thermal_zone2/temp"
            };
            
            for (String zone : thermalZones) {
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(zone));
                    String tempStr = reader.readLine();
                    reader.close();
                    
                    if (tempStr != null && !tempStr.isEmpty()) {
                        double temp = Double.parseDouble(tempStr) / 1000.0; // Convert millidegrees to degrees
                        if (temp > 0 && temp < 150) { // Sanity check
                            cpuTemperature = Math.max(cpuTemperature, temp);
                        }
                    }
                } catch (Exception e) {
                    // This thermal zone doesn't exist or isn't readable
                    continue;
                }
            }
            
            // Check for thermal throttling
            if (cpuTemperature > 0) {
                thermalThrottling = cpuTemperature > thermalThrottleThreshold;
            }
            
            Log.d(TAG, "CPU Temperature: " + cpuTemperature + "°C, Throttling: " + thermalThrottling);
            
        } catch (Exception e) {
            Log.w(TAG, "Could not collect thermal metrics", e);
        }
    }
    
    private void collectBatteryMetrics() {
        try {
            BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            if (batteryManager != null) {
                batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                isCharging = batteryManager.isCharging();
                batteryHealth = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS);
            }
            Log.d(TAG, "Battery Level: " + batteryLevel + "%, Charging: " + isCharging);
        } catch (Exception e) {
            Log.w(TAG, "Could not collect battery metrics", e);
        }
    }
    
    private void collectMemoryMetrics() {
        try {
            // Get memory info
            Debug.MemoryInfo memoryInfo = new Debug.MemoryInfo();
            Debug.getMemoryInfo(memoryInfo);
            
            Runtime runtime = Runtime.getRuntime();
            totalMemory = runtime.maxMemory();
            usedMemory = runtime.totalMemory() - runtime.freeMemory();
            freeMemory = runtime.freeMemory();
            memoryUsage = usedMemory;
            
            Log.d(TAG, "Memory - Total: " + formatBytes(totalMemory) + 
                      ", Used: " + formatBytes(usedMemory) + 
                      ", Free: " + formatBytes(freeMemory));
        } catch (Exception e) {
            Log.w(TAG, "Could not collect memory metrics", e);
        }
    }
    
    private void collectSystemMetrics() {
        try {
            // Count background apps (simplified)
            // In a real implementation, you'd use ActivityManager.getRunningAppProcesses()
            backgroundAppsCount = 0; // Placeholder
            
            // Get storage info
            java.io.File root = new java.io.File("/");
            totalStorage = root.getTotalSpace();
            availableStorage = root.getFreeSpace();
            
            Log.d(TAG, "Storage - Total: " + formatBytes(totalStorage) + 
                      ", Available: " + formatBytes(availableStorage));
        } catch (Exception e) {
            Log.w(TAG, "Could not collect system metrics", e);
        }
    }
    
    private void analyzePerformance() {
        performanceScore = 100.0; // Start with perfect score
        degradationReason = "";
        
        // Check thermal throttling
        if (thermalThrottling) {
            performanceScore -= 20;
            degradationReason += "Thermal throttling detected. ";
        }
        
        // Check low battery
        if (batteryLevel > 0 && batteryLevel < 20) {
            performanceScore -= 10;
            degradationReason += "Low battery level. ";
        }
        
        // Check high memory usage
        if (totalMemory > 0) {
            double memoryUsagePercent = (double) memoryUsage / totalMemory * 100;
            if (memoryUsagePercent > 90) {
                performanceScore -= 15;
                degradationReason += "High memory usage. ";
            }
        }
        
        // Check low storage
        if (totalStorage > 0) {
            double storageUsagePercent = (double) (totalStorage - availableStorage) / totalStorage * 100;
            if (storageUsagePercent > 95) {
                performanceScore -= 10;
                degradationReason += "Low storage space. ";
            }
        }
        
        // Check background apps
        if (backgroundAppsCount > 20) {
            performanceScore -= 5;
            degradationReason += "Many background apps running. ";
        }
        
        performanceDegraded = performanceScore < 80;
        
        Log.d(TAG, "Performance Analysis - Score: " + performanceScore + 
                  ", Degraded: " + performanceDegraded + 
                  ", Reason: " + degradationReason);
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    // Getters
    public double getCpuTemperature() { return cpuTemperature; }
    public boolean isThermalThrottling() { return thermalThrottling; }
    public int getBatteryLevel() { return batteryLevel; }
    public boolean isCharging() { return isCharging; }
    public int getBatteryHealth() { return batteryHealth; }
    public long getTotalMemory() { return totalMemory; }
    public long getUsedMemory() { return usedMemory; }
    public long getFreeMemory() { return freeMemory; }
    public long getMemoryUsage() { return memoryUsage; }
    public int getBackgroundAppsCount() { return backgroundAppsCount; }
    public double getCpuUsage() { return cpuUsage; }
    public long getAvailableStorage() { return availableStorage; }
    public long getTotalStorage() { return totalStorage; }
    public boolean isPerformanceDegraded() { return performanceDegraded; }
    public String getDegradationReason() { return degradationReason; }
    public double getPerformanceScore() { return performanceScore; }
    
    // Utility methods
    public String getFormattedCpuTemperature() {
        if (cpuTemperature == -1) return "N/A";
        return String.format("%.1f°C", cpuTemperature);
    }
    
    public String getFormattedBatteryLevel() {
        if (batteryLevel == -1) return "N/A";
        return batteryLevel + "%";
    }
    
    public String getFormattedMemoryUsage() {
        if (memoryUsage == -1) return "N/A";
        return formatBytes(memoryUsage);
    }
    
    public String getFormattedStorageUsage() {
        if (totalStorage == 0) return "N/A";
        long used = totalStorage - availableStorage;
        return formatBytes(used) + " / " + formatBytes(totalStorage);
    }
    
    public double getMemoryUsagePercent() {
        if (totalMemory == 0) return 0;
        return (double) memoryUsage / totalMemory * 100;
    }
    
    public double getStorageUsagePercent() {
        if (totalStorage == 0) return 0;
        return (double) (totalStorage - availableStorage) / totalStorage * 100;
    }
    
    public String getPerformanceStatus() {
        if (performanceScore >= 90) return "EXCELLENT";
        if (performanceScore >= 80) return "GOOD";
        if (performanceScore >= 70) return "FAIR";
        if (performanceScore >= 60) return "POOR";
        return "CRITICAL";
    }
    
    public String getDetailedAnalysis() {
        StringBuilder analysis = new StringBuilder();
        
        analysis.append("=== ADVANCED PERFORMANCE ANALYSIS ===\n\n");
        
        // Thermal analysis
        analysis.append("🌡️ THERMAL STATUS\n");
        analysis.append("CPU Temperature: ").append(getFormattedCpuTemperature()).append("\n");
        analysis.append("Thermal Throttling: ").append(thermalThrottling ? "YES" : "NO").append("\n");
        if (thermalThrottling) {
            analysis.append("⚠️ Warning: CPU is throttling due to high temperature\n");
        }
        analysis.append("\n");
        
        // Battery analysis
        analysis.append("🔋 BATTERY STATUS\n");
        analysis.append("Battery Level: ").append(getFormattedBatteryLevel()).append("\n");
        analysis.append("Charging: ").append(isCharging ? "YES" : "NO").append("\n");
        if (batteryLevel > 0 && batteryLevel < 20) {
            analysis.append("⚠️ Warning: Low battery may affect performance\n");
        }
        analysis.append("\n");
        
        // Memory analysis
        analysis.append("💾 MEMORY STATUS\n");
        analysis.append("Memory Usage: ").append(getFormattedMemoryUsage()).append("\n");
        analysis.append("Memory Usage: ").append(String.format("%.1f%%", getMemoryUsagePercent())).append("\n");
        if (getMemoryUsagePercent() > 90) {
            analysis.append("⚠️ Warning: High memory usage may slow down the system\n");
        }
        analysis.append("\n");
        
        // Storage analysis
        analysis.append("💿 STORAGE STATUS\n");
        analysis.append("Storage Usage: ").append(getFormattedStorageUsage()).append("\n");
        analysis.append("Storage Usage: ").append(String.format("%.1f%%", getStorageUsagePercent())).append("\n");
        if (getStorageUsagePercent() > 95) {
            analysis.append("⚠️ Warning: Low storage space may affect performance\n");
        }
        analysis.append("\n");
        
        // System analysis
        analysis.append("⚙️ SYSTEM STATUS\n");
        analysis.append("Background Apps: ").append(backgroundAppsCount).append("\n");
        analysis.append("Performance Score: ").append(String.format("%.1f/100", performanceScore)).append("\n");
        analysis.append("Status: ").append(getPerformanceStatus()).append("\n");
        
        if (performanceDegraded) {
            analysis.append("\n⚠️ PERFORMANCE ISSUES DETECTED:\n");
            analysis.append(degradationReason);
        }
        
        return analysis.toString();
    }
}
