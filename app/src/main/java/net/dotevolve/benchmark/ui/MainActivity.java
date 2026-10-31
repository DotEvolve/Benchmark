package net.dotevolve.benchmark.ui;
import net.dotevolve.benchmark.R;
import net.dotevolve.benchmark.core.BenchmarkEngine;
import net.dotevolve.benchmark.core.PerformanceMetrics;
import net.dotevolve.benchmark.work.ScheduledBenchmarkWorker;
import net.dotevolve.benchmark.ads.GoogleMobileAdsConsentManager;
import net.dotevolve.benchmark.BuildConfig;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Dialog;
import android.content.SharedPreferences;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.view.ViewGroup;
import android.content.Intent;
import android.content.ClipData;
import android.content.ClipboardManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import net.dotevolve.benchmark.databinding.ActivityMainBinding;

import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity"; // Define TAG constant
    // Placeholder for your actual test device ID
    protected static String TEST_DEVICE_HASHED_ID;

    private ActivityMainBinding binding;

    private TextView scorer;
    private TextView result;

    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

    private final AtomicBoolean isMobileAdsInitializeCalled = new AtomicBoolean(false);
    private AdView adView; // This will be the programmatically created AdView
    private FrameLayout adContainerView; // Changed from AdView to FrameLayout
    private String BANNER_AD_UNIT_ID;
    private String INTERSTITIAL_AD_UNIT_ID;

    private InterstitialAd mInterstitialAd;

    private GoogleMobileAdsConsentManager googleMobileAdsConsentManager;
    
    // Enhanced metrics system
    private BenchmarkEngine benchmarkEngine;
    private PerformanceMetrics currentMetrics;
    private boolean isBenchmarkRunning = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupWindowInsets();

        TEST_DEVICE_HASHED_ID = getResources().getString(R.string.test_device_hashed_id);

        // Initialize enhanced metrics system
        benchmarkEngine = new BenchmarkEngine(this);
        setupBenchmarkCallbacks();

        setupUI();
        setupAds();

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Starting AdInspector...");
            MobileAds.openAdInspector(
                    this,
                    error -> {});
        } else {
            // Code to execute in release mode
            Log.d(TAG, "Release build - AdInspector not opened automatically.");
        }
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, 0);
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            View adContainer = findViewById(R.id.adContainerView);
            if (adContainer != null && adContainer.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) adContainer.getLayoutParams();
                mlp.bottomMargin = bars.bottom + (int) (8 * getResources().getDisplayMetrics().density);
                adContainer.setLayoutParams(mlp);
            }
            if (binding.actionShare.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams fabLp = (ViewGroup.MarginLayoutParams) binding.actionShare.getLayoutParams();
                fabLp.bottomMargin = bars.bottom + (int) (72 * getResources().getDisplayMetrics().density);
                binding.actionShare.setLayoutParams(fabLp);
            }
            return insets;
        });
    }

    private void setupUI() {
        setSupportActionBar(binding.toolbar);

        result = findViewById(R.id.textResult);
        scorer = findViewById(R.id.textScore);

        adContainerView = findViewById(R.id.adContainerView);

        Button mBeginButton = findViewById(R.id.button);
        mBeginButton.setOnClickListener(v -> {
            if (!isBenchmarkRunning) {
                runEnhancedBenchmark();
            }
        });
        
        binding.actionShare.setOnClickListener(v -> {
            if (currentMetrics != null) {
                showDetailedMetricsDialog();
            } else {
                Toast.makeText(this, "Run benchmark first to view detailed metrics", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void setupBenchmarkCallbacks() {
        benchmarkEngine.setProgressCallback(new BenchmarkEngine.BenchmarkProgressCallback() {
            @Override
            public void onProgressUpdate(int progress, String currentTest) {
                runOnUiThread(() -> {
                    if (result != null) {
                        result.setText("Running " + currentTest + "...\nProgress: " + progress + "%");
                    }
                });
            }
            
            @Override
            public void onTestComplete(String testName, long duration) {
                runOnUiThread(() -> {
                    if (result != null) {
                        result.append("\n✓ " + testName + " completed in " + formatNanoTime(duration));
                    }
                });
            }
            
            @Override
            public void onBenchmarkComplete(PerformanceMetrics metrics) {
                // Save to historical database in background thread (fixes main thread ANR)
                backgroundExecutor.execute(() -> metrics.saveToHistory(MainActivity.this));

                runOnUiThread(() -> {
                    isBenchmarkRunning = false;
                    currentMetrics = metrics;
                    
                    displayEnhancedResults();
                    showInterstitial();
                });
            }
        });
    }
    
    private void runEnhancedBenchmark() {
        isBenchmarkRunning = true;
        
        if (result != null) {
            result.setText("""
                    🚀 Starting Enhanced Performance Benchmark...
                    
                    This will test:
                    • SHA-1 Hash Performance
                    • MD5 Hash Performance
                    • AES Encryption Performance
                    • Matrix Multiplication
                    • Sorting Algorithms
                    • Data Compression
                    • Memory Bandwidth
                    • Multi-threaded Performance
                    • System Overhead Analysis
                    
                    Please wait...""");
        }
        
        if (scorer != null) {
            scorer.setText("...");
        }
        
        // Run benchmark in background thread via executor
        backgroundExecutor.execute(() -> benchmarkEngine.runComprehensiveBenchmark());
    }
    
    private void displayEnhancedResults() {
        if (currentMetrics == null) return;
        
        // Display detailed results
        if (result != null) {
            result.setText(currentMetrics.getFormattedResults());
        }
        
        // Display overall score
        if (scorer != null) {
            scorer.setText(String.valueOf(currentMetrics.getOverallScore()));
        }
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

    private void setupAds() {
        Log.d(TAG, "Google Mobile Ads SDK Version: " + MobileAds.getVersion());

        BANNER_AD_UNIT_ID = getString(R.string.admob_banner_ad_unit_id);
        INTERSTITIAL_AD_UNIT_ID = getString(R.string.admob_interstitial_ad_unit_id);

        // Since adContainerView is now FrameLayout, this check is still valid.
        // The findViewById will return a FrameLayout, which matches the (new) type of adContainerView.
        View container = binding.getRoot().findViewById(R.id.adContainerView);
        if (container instanceof FrameLayout) {
             adContainerView = (FrameLayout) container;
        } else {
             Log.e(TAG, "adContainerView not found in binding or is not a FrameLayout. Check layout IDs.");
        }

        googleMobileAdsConsentManager =
                GoogleMobileAdsConsentManager.getInstance(getApplicationContext());
        googleMobileAdsConsentManager.gatherConsent(
                this,
                consentError -> {
                    if (consentError != null) {
                        // Consent not obtained in current session.
                        Log.w(TAG,
                                String.format("%s: %s", consentError.getErrorCode(), consentError.getMessage()));
                    }

                    if (googleMobileAdsConsentManager.canRequestAds()) {
                        initializeMobileAdsSdk();
                    }

                    if (googleMobileAdsConsentManager.isPrivacyOptionsRequired()) {
                        // Regenerate the options menu to include a privacy setting.
                        invalidateOptionsMenu();
                    }
                });

        // This sample attempts to load ads using consent obtained in the previous session.
        if (googleMobileAdsConsentManager.canRequestAds()) {
            initializeMobileAdsSdk();
        }
    }

    private void initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return;
        }

        MobileAds.setRequestConfiguration(
                new RequestConfiguration.Builder()
                        .setTestDeviceIds(Collections.singletonList(TEST_DEVICE_HASHED_ID))
                        .build());

        backgroundExecutor.execute(() -> MobileAds.initialize(this, initializationStatus -> {
            Log.d(TAG, "Google Mobile Ads SDK Initialized.");
            runOnUiThread(this::loadBanner); // Programmatic banner
        }));

        requestNewInterstitial(); // Load interstitial after initialization
    }

    private void loadBanner() {
        if (adContainerView == null) {
            Log.e(TAG, "adContainerView is null. Cannot load banner.");
             // Attempt to find it by ID if it wasn't available via binding directly earlier
            View potentialContainer = findViewById(R.id.adContainerView);
            if (potentialContainer instanceof FrameLayout) {
                // Corrected cast to FrameLayout
                adContainerView = (FrameLayout) potentialContainer;
            } else {
                Log.e(TAG, "Fallback: adContainerView still not found or not a FrameLayout.");
                return; // Critical: cannot proceed to load banner
            }
        }

        // Destroy existing adView if it exists
        if (adView != null) {
            adView.destroy();
        }
        adContainerView.removeAllViews(); // Clear previous ads

        adView = new AdView(this);
        adView.setAdUnitId(BANNER_AD_UNIT_ID);

        // Use actual display width instead of hardcoded value to avoid misalignment
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int adWidthPixels = adContainerView.getWidth();
        if (adWidthPixels == 0) {
            adWidthPixels = displayMetrics.widthPixels;
        }
        int adWidth = (int) (adWidthPixels / displayMetrics.density);
        adView.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth));

        adContainerView.addView(adView);

        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
        Log.d(TAG, "Banner ad loading requested.");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_toggle_schedule) {
            toggleSchedule();
            return true;
        } else if (id == R.id.action_toggle_dark_mode) {
            toggleDarkMode();
            return true;
        } else if (id == R.id.action_history) {
            Intent intent = new Intent(this, HistoryActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleSchedule() {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean enabled = prefs.getBoolean("schedule_enabled", false);
        if (enabled) {
            ScheduledBenchmarkWorker.cancel(this);
        } else {
            ScheduledBenchmarkWorker.scheduleDaily(this);
        }
        prefs.edit().putBoolean("schedule_enabled", !enabled).apply();
        Toast.makeText(this, !enabled ? "Daily benchmark scheduled" : "Daily benchmark canceled", Toast.LENGTH_SHORT).show();
    }

    private void toggleDarkMode() {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean dark = prefs.getBoolean("dark_mode", false);
        boolean newValue = !dark;
        prefs.edit().putBoolean("dark_mode", newValue).apply();
        AppCompatDelegate.setDefaultNightMode(
                newValue ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
        Toast.makeText(this, newValue ? "Dark mode on" : "Dark mode off", Toast.LENGTH_SHORT).show();
    }
    
    private void showDetailedMetricsDialog() {
        if (currentMetrics == null) return;
        
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.metrics_detail_dialog);
        dialog.setTitle("Detailed Performance Metrics");
        
        // Update dialog content
        TextView overallScore = dialog.findViewById(R.id.overallScore);
        TextView cryptoScore = dialog.findViewById(R.id.cryptoScore);
        TextView efficiencyScore = dialog.findViewById(R.id.efficiencyScore);
        TextView stabilityScore = dialog.findViewById(R.id.stabilityScore);
        TextView deviceInfo = dialog.findViewById(R.id.deviceInfo);
        TextView timingInfo = dialog.findViewById(R.id.timingInfo);
        TextView analysisInfo = dialog.findViewById(R.id.analysisInfo);
        PerformanceVisualizer visualizer = dialog.findViewById(R.id.performanceVisualizer);
        
        if (overallScore != null) overallScore.setText(String.valueOf(currentMetrics.getOverallScore()));
        if (cryptoScore != null) cryptoScore.setText(String.valueOf(currentMetrics.getCryptoScore()));
        if (efficiencyScore != null) efficiencyScore.setText(String.valueOf(currentMetrics.getEfficiencyScore()));
        if (stabilityScore != null) stabilityScore.setText(String.valueOf(currentMetrics.getStabilityScore()));
        
        if (deviceInfo != null) {
            deviceInfo.setText("Device: " + currentMetrics.getDeviceModel() + "\n" +
                              "CPU Cores: " + currentMetrics.getCpuCores() + "\n" +
                              "Max Memory: " + formatBytes(currentMetrics.getTotalMemory()));
        }
        
        if (timingInfo != null) {
            timingInfo.setText(currentMetrics.getDetailedTimingInfo());
        }
        
        if (analysisInfo != null) {
            analysisInfo.setText("Performance Category: " + getPerformanceCategory(currentMetrics.getOverallScore()) + "\n" +
                                "Algorithm Efficiency: SHA-1 vs MD5 ratio analysis\n" +
                                "System Utilization: CPU and memory efficiency metrics");
        }
        
        if (visualizer != null) {
            visualizer.updateMetrics(currentMetrics);
        }
        
        // Set up action buttons
        Button exportButton = dialog.findViewById(R.id.exportButton);
        Button shareButton = dialog.findViewById(R.id.shareButton);
        
        if (exportButton != null) {
            exportButton.setOnClickListener(v -> {
                exportResults();
                dialog.dismiss();
            });
        }
        
        if (shareButton != null) {
            shareButton.setOnClickListener(v -> {
                shareResults();
                dialog.dismiss();
            });
        }
        
        dialog.show();
    }
    
    public void exportResults() {
        if (currentMetrics == null) {
            Toast.makeText(this, "No benchmark results to export", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String exportText = generateExportText();
        
        // Copy to clipboard
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Benchmark Results", exportText);
        clipboard.setPrimaryClip(clip);
        
        // Also try to share
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, exportText);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "CPU Benchmark Results - " + currentMetrics.getDeviceModel());
        
        startActivity(Intent.createChooser(shareIntent, "Share Benchmark Results"));
        
        Toast.makeText(this, "Results copied to clipboard and ready to share", Toast.LENGTH_LONG).show();
    }
    
    private String generateExportText() {

        return "=== CPU BENCHMARK RESULTS ===\n" +
                "Generated: " + new java.util.Date() + "\n\n" +
                "DEVICE INFORMATION\n" +
                "Model: " + currentMetrics.getDeviceModel() + "\n" +
                "CPU Cores: " + currentMetrics.getCpuCores() + "\n" +
                "Max Memory: " + formatBytes(currentMetrics.getTotalMemory()) + "\n\n" +
                "PERFORMANCE SCORES\n" +
                "Overall Score: " + currentMetrics.getOverallScore() + "/100\n" +
                "Crypto Performance: " + currentMetrics.getCryptoScore() + "/100\n" +
                "Efficiency: " + currentMetrics.getEfficiencyScore() + "/100\n" +
                "Stability: " + currentMetrics.getStabilityScore() + "/100\n\n" +
                "DETAILED TIMING\n" +
                currentMetrics.getDetailedTimingInfo() + "\n" +
                "PERFORMANCE ANALYSIS\n" +
                "Category: " + getPerformanceCategory(currentMetrics.getOverallScore()) + "\n" +
                "Algorithm Comparison: SHA-1 vs MD5 efficiency analysis\n" +
                "System Utilization: CPU and memory efficiency metrics\n\n" +
                "Generated by CPU Benchmark App v10.0.0\n" +
                "https://play.google.com/store/apps/details?id=net.dotevolve.benchmark";
    }
    
    public void shareResults() {
        if (currentMetrics == null) {
            Toast.makeText(this, "No benchmark results to share", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String shareText = generateShareText();
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "My CPU Benchmark Results - " + currentMetrics.getOverallScore() + "/100");
        
        startActivity(Intent.createChooser(shareIntent, "Share Benchmark Results"));
    }
    
    private String generateShareText() {

        return "🚀 CPU Benchmark Results!\n\n" +
                "📱 Device: " + currentMetrics.getDeviceModel() + "\n" +
                "🎯 Overall Score: " + currentMetrics.getOverallScore() + "/100 (" +
                getPerformanceCategory(currentMetrics.getOverallScore()) + ")\n" +
                "🔐 Crypto Performance: " + currentMetrics.getCryptoScore() + "/100\n" +
                "⚡ Efficiency: " + currentMetrics.getEfficiencyScore() + "/100\n" +
                "🎯 Stability: " + currentMetrics.getStabilityScore() + "/100\n\n" +
                "Tested with CPU Benchmark App!\n" +
                "Download: https://play.google.com/store/apps/details?id=net.dotevolve.benchmark";
    }
    
    private String getPerformanceCategory(int score) {
        if (score >= 90) return "EXCELLENT";
        if (score >= 70) return "GOOD";
        if (score >= 50) return "AVERAGE";
        if (score >= 30) return "BELOW_AVERAGE";
        return "POOR";
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    // Add onSupportNavigateUp for NavController integration
    @Override
    public boolean onSupportNavigateUp() {
        return true;
    }

    @Override
    public void onPause() {
        if (adView != null) {
            adView.pause();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adView != null) {
            adView.resume();
        }
    }

    @Override
    public void onDestroy() {
        if (adView != null) {
            adView.destroy();
        }
        backgroundExecutor.shutdown();
        super.onDestroy();
    }

    private void showInterstitial() {
        // Show the ad if it's ready. Otherwise, restart the app.
        if (mInterstitialAd != null) {
            mInterstitialAd.show(this);
        } else {
            Log.d(TAG, "The interstitial ad is still loading.");
            if (googleMobileAdsConsentManager.canRequestAds()) {
                requestNewInterstitial();
            }
        }
    }

    public void requestNewInterstitial() {
        // Request a new ad if one isn't already loaded.
        if (mInterstitialAd != null) {
            return;
        }
        InterstitialAd.load(
                this,
                INTERSTITIAL_AD_UNIT_ID,
                new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        Log.d(TAG, "Ad was loaded.");
                        MainActivity.this.mInterstitialAd = interstitialAd;
                        if (BuildConfig.DEBUG)
                            Toast.makeText(MainActivity.this, "Ad Loaded", Toast.LENGTH_SHORT).show();

                        interstitialAd.setFullScreenContentCallback(
                                new FullScreenContentCallback() {
                                    @Override
                                    public void onAdDismissedFullScreenContent() {
                                        // Called when fullscreen content is dismissed.
                                        Log.d(TAG, "The ad was dismissed.");
                                        // Make sure to set your reference to null so you don't
                                        // show it a second time.
                                        MainActivity.this.mInterstitialAd = null;
                                    }

                                    @Override
                                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                                        // Called when fullscreen content failed to show.
                                        Log.d(TAG, "The ad failed to show.");
                                        // Make sure to set your reference to null so you don't
                                        // show it a second time.
                                        MainActivity.this.mInterstitialAd = null;
                                    }

                                    @Override
                                    public void onAdShowedFullScreenContent() {
                                        // Called when fullscreen content is shown.
                                        Log.d(TAG, "The ad was shown.");
                                    }

                                    @Override
                                    public void onAdImpression() {
                                        // Called when an impression is recorded for an ad.
                                        Log.d(TAG, "The ad recorded an impression.");
                                    }

                                    @Override
                                    public void onAdClicked() {
                                        // Called when ad is clicked.
                                        Log.d(TAG, "The ad was clicked.");
                                    }
                                });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.d(TAG, loadAdError.getMessage());
                        mInterstitialAd = null;
                        String error =
                                String.format(
                                        java.util.Locale.US,
                                        "domain: %s, code: %d, message: %s",
                                        loadAdError.getDomain(),
                                        loadAdError.getCode(),
                                        loadAdError.getMessage());
                        if (BuildConfig.DEBUG)
                            Toast.makeText(MainActivity.this,
                                            "onAdFailedToLoad() with error: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
