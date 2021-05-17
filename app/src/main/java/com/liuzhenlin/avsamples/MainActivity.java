package com.liuzhenlin.avsamples;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.liuzhenlin.gifloader.GifLoadSampleActivity;
import com.liuzhenlin.h264codec.H264DecodingSampleActivity;
import com.liuzhenlin.h264codec.H264ScreencapEncodingSampleActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        verifyStoragePermissions(this);

        findViewById(R.id.btn_gifLoadSample).setOnClickListener(this);
        View h264ScreencapEncodingSampleBtn = findViewById(R.id.btn_h264ScreencapEncodingSample);
        View h264DecodingSampleBtn = findViewById(R.id.btn_h264DecodingSample);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            h264ScreencapEncodingSampleBtn.setVisibility(View.GONE);
            h264DecodingSampleBtn.setVisibility(View.GONE);
        }
        h264ScreencapEncodingSampleBtn.setOnClickListener(this);
        h264DecodingSampleBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_gifLoadSample:
                startActivity(new Intent(this, GifLoadSampleActivity.class));
                break;
            case R.id.btn_h264ScreencapEncodingSample:
                startActivity(new Intent(this, H264ScreencapEncodingSampleActivity.class));
                break;
            case R.id.btn_h264DecodingSample:
                startActivity(new Intent(this, H264DecodingSampleActivity.class));
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