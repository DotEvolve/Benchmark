package net.dotevolve.benchmark;

import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.ui.AppBarConfiguration;

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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity"; // Define TAG constant
    // Placeholder for your actual test device ID
    protected static String TEST_DEVICE_HASHED_ID;

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    private TextView scorer;
    private TextView result;

    private String testString;
    private String HashValue;
    private String MD5Value;

    private final AtomicBoolean isMobileAdsInitializeCalled = new AtomicBoolean(false);
    private AdView adView; // This will be the programmatically created AdView
    private FrameLayout adContainerView; // Changed from AdView to FrameLayout
    private String BANNER_AD_UNIT_ID;
    private String INTERSTITIAL_AD_UNIT_ID;

    private InterstitialAd mInterstitialAd;

    private GoogleMobileAdsConsentManager googleMobileAdsConsentManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        TEST_DEVICE_HASHED_ID = getResources().getString(R.string.test_device_hashed_id);

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

    private void setupUI() {
        setSupportActionBar(binding.toolbar);

        result = findViewById(R.id.textResult);
        scorer = findViewById(R.id.textScore);

        adContainerView = findViewById(R.id.adContainerView);

        testString = getResources().getString(R.string.testString);

        Button mBeginButton = findViewById(R.id.button);
        mBeginButton.setOnClickListener(v -> compute());
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

        new Thread(() -> MobileAds.initialize(this, initializationStatus -> {
            Log.d(TAG, "Google Mobile Ads SDK Initialized.");
            runOnUiThread(this::loadBanner); // Programmatic banner
        })).start();

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
        adView.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, 360));

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
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
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
        super.onDestroy();
    }

    private void showInterstitial() {
        // Show the ad if it's ready. Otherwise restart the game.
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
        final boolean[] adIsLoading = {false};
        // Request a new ad if one isn't already loaded.
        if (mInterstitialAd != null) {
            return;
        }
        adIsLoading[0] = true;
        InterstitialAd.load(
                this,
                INTERSTITIAL_AD_UNIT_ID,
                new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        Log.d(TAG, "Ad was loaded.");
                        MainActivity.this.mInterstitialAd = interstitialAd;
                        adIsLoading[0] = false;
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
                        adIsLoading[0] = false;
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


    public void compute() {
        String methodTag = Objects.requireNonNull(new Object() {}.getClass().getEnclosingMethod()).getName();

        if (result == null || scorer == null) {
            Log.e(TAG, "compute() called, but result/scorer TextViews are null in MainActivity.");
            return;
        }

        String ttLongString;
        String ttLongStringMD5;
        String output;
        long ttLong;
        long ttLongMD5;
        long ttLongLOOP;

        output = "Calculating score...";
        result.setText(output);

        ttLongLOOP = System.nanoTime();
        long ttLong2LOOP = System.nanoTime() - ttLongLOOP;

        ttLong = System.nanoTime();
        for (int i = 0; i < 100000; i++)
            computeSHAHash(testString);
        long ttLong2 = System.nanoTime() - ttLong;
        ttLongString = Long.toString(ttLong2);

        ttLongMD5 = System.nanoTime();
        for (int i = 0; i < 100000; i++)
            computeMD5hash(testString);
        long ttLong2MD5 = System.nanoTime() - ttLongMD5;
        ttLongStringMD5 = Long.toString(ttLong2MD5);

        Integer score = Math.round((float) ttLong2LOOP / 10000000);
        Integer score2 = Math.round((float) ttLong2 / 10000000);
        Integer score3 = Math.round((float) ttLong2MD5 / 10000000);
        int scoreAvg = (score + score2 + score3) / 3;
        String scoreString = Integer.toString(scoreAvg);

        output = "SHA1 hash: \n" + HashValue +
                "\nTime Taken: " + ttLongString;
        output += "\n\nMD5 hash: \n" + MD5Value +
                "\n\nTime Taken: " + ttLongStringMD5;

        showInterstitial();

        result.setText(output);
        scorer.setText(scoreString);
    }

    public void computeSHAHash(String password) {
        MessageDigest mdSha1 = null;
        try {
            mdSha1 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e1) {
            Log.e(TAG, "Benchmark: Error initializing SHA1");
        }
        assert mdSha1 != null;
        mdSha1.update(password.getBytes(StandardCharsets.US_ASCII));
        byte[] data = mdSha1.digest();
        StringBuilder sb = new StringBuilder();
        String hex;
        hex = Base64.encodeToString(data, 0, data.length, 0);
        sb.append(hex);
        HashValue = sb.toString();
    }

    public void computeMD5hash(String password) {
        try {
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(password.getBytes());
            byte[] messageDigest = digest.digest();
            StringBuilder MD5Hash = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                StringBuilder h = new StringBuilder(
                        Integer.toHexString(0xFF & aMessageDigest));
                while (h.length() < 2)
                    h.insert(0, "0");
                MD5Hash.append(h);
            }
            MD5Value = MD5Hash.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Benchmark: Error initializing MD5");
        }
    }
}
