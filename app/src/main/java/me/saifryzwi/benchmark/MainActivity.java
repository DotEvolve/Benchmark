package me.saifryzwi.benchmark;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MainActivity extends AppCompatActivity {
    private TextView scorer;
    private TextView result;
    private String testString;

    private String HashValue;

    private String MD5Value;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        result = (TextView) findViewById(R.id.textResult);
        scorer = (TextView) findViewById(R.id.textScore);
        testString = getResources().getString(R.string.testString);
    }

    public void onBeginClick(View view) {

        String ttLongString;
        String ttLongStringMD5;

        String output;
        long ttLong;
        long ttLongMD5;
        long ttLongLOOP;

        ttLongLOOP = System.nanoTime();
        for (int i = 0; i < 99999999; i++) ;
        Long ttLong2LOOP = System.nanoTime() - ttLongLOOP;

        ttLong = System.nanoTime();
        for (int i = 0; i < 30000; i++)
            computeSHAhash(testString);
        Long ttLong2 = System.nanoTime() - ttLong;
        ttLongString = ttLong2.toString();

        ttLongMD5 = System.nanoTime();
        for (int i = 0; i < 30000; i++)
            computeMD5hash(testString);
        Long ttLong2MD5 = System.nanoTime() - ttLongMD5;
        ttLongStringMD5 = ttLong2MD5.toString();

        Integer score = Math.round(ttLong2LOOP / 100000000);
        Integer score2 = Math.round(ttLong2 / 100000000);
        Integer score3 = Math.round(ttLong2MD5 / 100000000);
        Integer scoreAvg = (score + score2 + score3) / 3;
        String scoreString = scoreAvg.toString();

        output = "SHA1 hash: \n" + HashValue +
                "\nTime Taken: " + ttLongString;
        output += "\n\nMD5 hash: \n" + MD5Value +
                "\n\nTime Taken: " + ttLongStringMD5;
        result.setText(output);
        scorer.setText(scoreString);
    }

    public void computeSHAhash(String password) {

        MessageDigest mdSha1 = null;
        try {
            mdSha1 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e1) {
            Log.e("Benchmark", "Error initializing SHA1");
        }
        try {
            mdSha1.update(password.getBytes("ASCII"));
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        byte[] data = mdSha1.digest();
        StringBuffer sb = new StringBuffer();
        String hex = null;

        hex = Base64.encodeToString(data, 0, data.length, 0);

        sb.append(hex);
        HashValue = sb.toString();
    }

    public void computeMD5hash(String password) {

        try {
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(password.getBytes());
            byte messageDigest[] = digest.digest();

            StringBuffer MD5Hash = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                String h = Integer.toHexString(0xFF & messageDigest[i]);
                while (h.length() < 2)
                    h = "0" + h;
                MD5Hash.append(h);
            }

            MD5Value = MD5Hash.toString();

        } catch (NoSuchAlgorithmException e) {
            Log.e("Benchmark", "Error initializing MD5");
        }

    }
}