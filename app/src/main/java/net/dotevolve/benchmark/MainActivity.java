package net.dotevolve.benchmark;

import android.os.Bundle;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.ui.AppBarConfiguration;

import net.dotevolve.benchmark.databinding.ActivityMainBinding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MainActivity extends AppCompatActivity {

    private static final Logger logger = LoggerFactory.getLogger(MainActivity.class);

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding; // Declare binding
    private TextView scorer;
    private TextView result;
    private String testString;
    private String HashValue;
    private String MD5Value;

    private int showAds = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        result = findViewById(R.id.textResult); // Or use binding.contentMain.textResult if content_main is given an ID and bound
        scorer = findViewById(R.id.textScore);   // Or use binding.contentMain.textScore
        testString = getResources().getString(R.string.testString);

        Button mBeginButton = findViewById(R.id.button); // Or use binding.contentMain.button
        mBeginButton.setOnClickListener(v -> compute());

        requestNewInterstitial();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // If you were using binding for views that need to be cleared, do it here.
        // For example, binding = null; but typically not needed for Activity level bindings.
    }

    private void requestNewInterstitial() {
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
            computeSHAHash(testString);
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
    }

    public void computeSHAHash(String password) {
        MessageDigest mdSha1 = null;
        try {
            mdSha1 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e1) {
            logger.error("Benchmark: {}", "Error initializing SHA1");
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
            logger.error("Benchmark: {}", "Error initializing MD5");
        }
    }
}
