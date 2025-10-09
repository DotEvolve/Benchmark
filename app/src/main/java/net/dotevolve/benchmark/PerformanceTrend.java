package net.dotevolve.benchmark;

/**
 * Data model for performance trends over time
 */
public class PerformanceTrend {
    private long id;
    private String deviceModel;
    private String trendDate;
    private double averageScore;
    private double scoreVariance;
    private int testCount;
    private String performanceTrend; // IMPROVING, DECLINING, STABLE
    
    // Constructors
    public PerformanceTrend() {}
    
    public PerformanceTrend(String deviceModel, String trendDate, double averageScore, 
                           double scoreVariance, int testCount, String performanceTrend) {
        this.deviceModel = deviceModel;
        this.trendDate = trendDate;
        this.averageScore = averageScore;
        this.scoreVariance = scoreVariance;
        this.testCount = testCount;
        this.performanceTrend = performanceTrend;
    }
    
    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    public String getDeviceModel() { return deviceModel; }
    public void setDeviceModel(String deviceModel) { this.deviceModel = deviceModel; }
    
    public String getTrendDate() { return trendDate; }
    public void setTrendDate(String trendDate) { this.trendDate = trendDate; }
    
    public double getAverageScore() { return averageScore; }
    public void setAverageScore(double averageScore) { this.averageScore = averageScore; }
    
    public double getScoreVariance() { return scoreVariance; }
    public void setScoreVariance(double scoreVariance) { this.scoreVariance = scoreVariance; }
    
    public int getTestCount() { return testCount; }
    public void setTestCount(int testCount) { this.testCount = testCount; }
    
    public String getPerformanceTrend() { return performanceTrend; }
    public void setPerformanceTrend(String performanceTrend) { this.performanceTrend = performanceTrend; }
    
    // Utility methods
    public String getFormattedAverageScore() {
        return String.format("%.1f", averageScore);
    }
    
    public String getFormattedVariance() {
        return String.format("%.2f", scoreVariance);
    }
    
    public String getTrendIcon() {
        switch (performanceTrend) {
            case "IMPROVING": return "📈";
            case "DECLINING": return "📉";
            case "STABLE": return "➡️";
            default: return "❓";
        }
    }
    
    public String getTrendDescription() {
        switch (performanceTrend) {
            case "IMPROVING": return "Performance is improving";
            case "DECLINING": return "Performance is declining";
            case "STABLE": return "Performance is stable";
            default: return "Unknown trend";
        }
    }
    
    @Override
    public String toString() {
        return "PerformanceTrend{" +
                "id=" + id +
                ", deviceModel='" + deviceModel + '\'' +
                ", trendDate='" + trendDate + '\'' +
                ", averageScore=" + averageScore +
                ", performanceTrend='" + performanceTrend + '\'' +
                '}';
    }
}
