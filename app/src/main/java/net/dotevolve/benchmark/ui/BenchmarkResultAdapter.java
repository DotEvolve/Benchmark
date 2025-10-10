package net.dotevolve.benchmark.ui;
import net.dotevolve.benchmark.R;
import net.dotevolve.benchmark.data.model.BenchmarkResult;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * RecyclerView adapter for displaying benchmark results
 */
public class BenchmarkResultAdapter extends RecyclerView.Adapter<BenchmarkResultAdapter.ResultViewHolder> {
    
    private List<BenchmarkResult> results;
    private OnResultClickListener clickListener;
    
    public interface OnResultClickListener {
        void onResultClick(BenchmarkResult result);
    }
    
    public BenchmarkResultAdapter(List<BenchmarkResult> results) {
        this.results = results;
    }
    
    public void setOnResultClickListener(OnResultClickListener listener) {
        this.clickListener = listener;
    }
    
    public void updateResults(List<BenchmarkResult> newResults) {
        this.results = newResults;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_benchmark_result, parent, false);
        return new ResultViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ResultViewHolder holder, int position) {
        BenchmarkResult result = results.get(position);
        holder.bind(result);
    }
    
    @Override
    public int getItemCount() {
        return results != null ? results.size() : 0;
    }
    
    class ResultViewHolder extends RecyclerView.ViewHolder {
        private TextView dateText;
        private TextView timeText;
        private TextView scoreText;
        private TextView categoryText;
        private TextView detailsText;
        
        public ResultViewHolder(@NonNull View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.dateText);
            timeText = itemView.findViewById(R.id.timeText);
            scoreText = itemView.findViewById(R.id.scoreText);
            categoryText = itemView.findViewById(R.id.categoryText);
            detailsText = itemView.findViewById(R.id.detailsText);
            
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        clickListener.onResultClick(results.get(position));
                    }
                }
            });
        }
        
        public void bind(BenchmarkResult result) {
            dateText.setText(result.getFormattedDate());
            timeText.setText(result.getFormattedTime());
            scoreText.setText(String.valueOf(result.getOverallScore()));
            categoryText.setText(result.getPerformanceCategory());
            
            // Build details string
            StringBuilder details = new StringBuilder();
            details.append("Crypto: ").append(result.getCryptoScore()).append(" | ");
            details.append("Efficiency: ").append(result.getEfficiencyScore()).append(" | ");
            details.append("Stability: ").append(result.getStabilityScore());
            
            if (result.hasAdvancedMetrics()) {
                details.append("\n");
                if (result.getCpuTemperature() != -1) {
                    details.append("CPU: ").append(result.getFormattedCpuTemperature()).append(" | ");
                }
                if (result.getBatteryLevel() != -1) {
                    details.append("Battery: ").append(result.getFormattedBatteryLevel()).append(" | ");
                }
                if (result.getMemoryUsage() != -1) {
                    details.append("Memory: ").append(result.getFormattedMemoryUsage());
                }
            }
            
            detailsText.setText(details.toString());
        }
    }
}
