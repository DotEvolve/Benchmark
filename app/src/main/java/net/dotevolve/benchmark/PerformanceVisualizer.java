package net.dotevolve.benchmark;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Custom view for visualizing performance metrics with progress bars and charts
 */
public class PerformanceVisualizer extends View {
    
    private Paint backgroundPaint;
    private Paint progressPaint;
    private Paint textPaint;
    private Paint scorePaint;
    
    private int overallScore = 0;
    private int cryptoScore = 0;
    private int efficiencyScore = 0;
    private int stabilityScore = 0;
    
    private String deviceModel = "";
    private int cpuCores = 0;
    
    public PerformanceVisualizer(Context context) {
        super(context);
        init();
    }
    
    public PerformanceVisualizer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public PerformanceVisualizer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        // Background paint
        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.LTGRAY);
        backgroundPaint.setStyle(Paint.Style.FILL);
        
        // Progress paint
        progressPaint = new Paint();
        progressPaint.setStyle(Paint.Style.FILL);
        
        // Text paint
        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(24);
        textPaint.setAntiAlias(true);
        
        // Score paint
        scorePaint = new Paint();
        scorePaint.setColor(Color.BLUE);
        scorePaint.setTextSize(32);
        scorePaint.setAntiAlias(true);
        scorePaint.setFakeBoldText(true);
    }
    
    public void updateMetrics(PerformanceMetrics metrics) {
        this.overallScore = metrics.getOverallScore();
        this.cryptoScore = metrics.getCryptoScore();
        this.efficiencyScore = metrics.getEfficiencyScore();
        this.stabilityScore = metrics.getStabilityScore();
        this.deviceModel = metrics.getDeviceModel();
        this.cpuCores = metrics.getCpuCores();
        
        invalidate(); // Trigger redraw
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        int width = getWidth();
        int height = getHeight();
        int padding = 20;
        
        // Draw background
        canvas.drawRect(0, 0, width, height, backgroundPaint);
        
        // Draw title
        canvas.drawText("Performance Visualization", padding, 40, textPaint);
        
        // Draw score bars
        drawScoreBar(canvas, "Overall", overallScore, 60, width, Color.BLUE);
        drawScoreBar(canvas, "Crypto", cryptoScore, 100, width, Color.GREEN);
        drawScoreBar(canvas, "Efficiency", efficiencyScore, 140, width, Color.RED);
        drawScoreBar(canvas, "Stability", stabilityScore, 180, width, Color.MAGENTA);
        
        // Draw device info
        textPaint.setTextSize(16);
        canvas.drawText("Device: " + deviceModel, padding, 220, textPaint);
        canvas.drawText("CPU Cores: " + cpuCores, padding, 240, textPaint);
        
        // Draw performance category
        String category = getPerformanceCategory(overallScore);
        scorePaint.setColor(getCategoryColor(category));
        canvas.drawText("Category: " + category, padding, 280, scorePaint);
    }
    
    private void drawScoreBar(Canvas canvas, String label, int score, int y, int width, int color) {
        int barWidth = width - 40;
        int barHeight = 20;
        int x = 20;
        
        // Draw background bar
        backgroundPaint.setColor(Color.LTGRAY);
        canvas.drawRect(x, y, x + barWidth, y + barHeight, backgroundPaint);
        
        // Draw progress bar
        progressPaint.setColor(color);
        int progressWidth = (int) ((score / 100.0) * barWidth);
        canvas.drawRect(x, y, x + progressWidth, y + barHeight, progressPaint);
        
        // Draw label and score
        textPaint.setTextSize(16);
        canvas.drawText(label + ": " + score, x, y - 5, textPaint);
    }
    
    private String getPerformanceCategory(int score) {
        if (score >= 90) return "EXCELLENT";
        if (score >= 70) return "GOOD";
        if (score >= 50) return "AVERAGE";
        if (score >= 30) return "BELOW_AVERAGE";
        return "POOR";
    }
    
    private int getCategoryColor(String category) {
        switch (category) {
            case "EXCELLENT": return Color.GREEN;
            case "GOOD": return Color.BLUE;
            case "AVERAGE": return Color.YELLOW;
            case "BELOW_AVERAGE": return Color.parseColor("#FF8C00"); // Orange
            case "POOR": return Color.RED;
            default: return Color.GRAY;
        }
    }
}

