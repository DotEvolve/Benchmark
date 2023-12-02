package in.rnetian.benchmark;

import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MainActivity extends AppCompatActivity {

    private InterstitialAd mInterstitialAd;
    private AdView mAdView;

    private TextView scorer;
    private TextView result;
    private String testString;
    private String HashValue;
    private String MD5Value;

    private int showAds = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        result = findViewById(R.id.textResult);
        scorer = findViewById(R.id.textScore);
        testString = getResources().getString(R.string.testString);

        Button mBeginButton = findViewById(R.id.begin_button);
        mBeginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                compute();
            }
        });

        String AdMobAppId = getString(R.string.admob_app_id);

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
                requestNewInterstitial();
            }
        });

        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        requestNewInterstitial();
    }

    @Override
    protected void onDestroy() {
        if (mAdView != null) {
            mAdView.destroy();
        }
        super.onDestroy();
    }

    private void requestNewInterstitial() {
        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(this,
                "ca-app-pub-5026356504898268/7109892159",
                adRequest,
                new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                mInterstitialAd = interstitialAd;
                Log.i("Ad Loaded", "onAdLoaded");
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.i("Failed to load: ", loadAdError.getMessage());
                mInterstitialAd = null;
            }
        });
    }

    public void compute() {

        String ttLongString;
        String ttLongStringMD5;

        String output;
        long ttLong;
        long ttLongMD5;
        long ttLongLOOP;

        output = "Calculating score...";
        result.setText(output);

        ttLongLOOP = System.nanoTime();
        Long ttLong2LOOP = System.nanoTime() - ttLongLOOP;

        ttLong = System.nanoTime();
        for (int i = 0; i < 100000; i++)
            computeSHAhash(testString);
        Long ttLong2 = System.nanoTime() - ttLong;
        ttLongString = ttLong2.toString();

        ttLongMD5 = System.nanoTime();
        for (int i = 0; i < 100000; i++)
            computeMD5hash(testString);
        Long ttLong2MD5 = System.nanoTime() - ttLongMD5;
        ttLongStringMD5 = ttLong2MD5.toString();

        Integer score = Math.round(ttLong2LOOP / 10000000);
        Integer score2 = Math.round(ttLong2 / 10000000);
        Integer score3 = Math.round(ttLong2MD5 / 10000000);
        Integer scoreAvg = (score + score2 + score3) / 3;
        String scoreString = scoreAvg.toString();

        output = "SHA1 hash: \n" + HashValue +
                "\nTime Taken: " + ttLongString;
        output += "\n\nMD5 hash: \n" + MD5Value +
                "\n\nTime Taken: " + ttLongStringMD5;

        result.setText(output);
        scorer.setText(scoreString);

        if (mInterstitialAd != null && showAds % 3 == 0) {
            mInterstitialAd.show(MainActivity.this);
        }

        showAds++;

        //showProgress(false);
    }

    public void computeSHAhash(String password) {

        MessageDigest mdSha1 = null;
        try {
            mdSha1 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e1) {
            Log.e("Benchmark", "Error initializing SHA1");
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
            Log.e("Benchmark", "Error initializing MD5");
        }

    }
}