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
import com.liuzhenlin.h264projection.H264ProjectionReceivingSampleActivity;
import com.liuzhenlin.h264projection.H264ProjectionSendingSampleActivity;
import com.liuzhenlin.videomixing.VideoMixingSampleActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        verifyStoragePermissions(this);

        findViewById(R.id.btn_gifLoadSample).setOnClickListener(this);
        View h264ScreencapEncodingSampleBtn = findViewById(R.id.btn_h264ScreencapEncodingSample);
        View h264DecodingSampleBtn = findViewById(R.id.btn_h264DecodingSample);
        View h264ProjectionSendingSampleBtn = findViewById(R.id.btn_h264ProjectionSendingSample);
        View h264ProjectionReceivingSampleBtn = findViewById(R.id.btn_h264ProjectionReceivingSample);
        View videoMixingSampleBtn = findViewById(R.id.btn_videoMixingSample);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            h264ScreencapEncodingSampleBtn.setVisibility(View.GONE);
            h264DecodingSampleBtn.setVisibility(View.GONE);
            h264ProjectionSendingSampleBtn.setVisibility(View.GONE);
            h264ProjectionReceivingSampleBtn.setVisibility(View.GONE);
            videoMixingSampleBtn.setVisibility(View.GONE);
        }
        h264ScreencapEncodingSampleBtn.setOnClickListener(this);
        h264DecodingSampleBtn.setOnClickListener(this);
        h264ProjectionSendingSampleBtn.setOnClickListener(this);
        h264ProjectionReceivingSampleBtn.setOnClickListener(this);
        videoMixingSampleBtn.setOnClickListener(this);
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
            case R.id.btn_h264ProjectionSendingSample:
                startActivity(new Intent(this, H264ProjectionSendingSampleActivity.class));
                break;
            case R.id.btn_h264ProjectionReceivingSample:
                startActivity(new Intent(this, H264ProjectionReceivingSampleActivity.class));
                break;
            case R.id.btn_videoMixingSample:
                startActivity(new Intent(this, VideoMixingSampleActivity.class));
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