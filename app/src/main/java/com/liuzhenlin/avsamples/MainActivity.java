package com.liuzhenlin.avsamples;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;

import com.liuzhenlin.gifloader.GifLoadSampleActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        verifyStoragePermissions(this);
        findViewById(R.id.btn_gifLoadSample).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_gifLoadSample:
                startActivity(new Intent(this, GifLoadSampleActivity.class));
                break;
        }
    }

    public void verifyStoragePermissions(Activity activity) {
        int REQUEST_STORAGE = 1;
        String[] PERMISSIONS_STORAGE = {
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE"};
        try {
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}