package com.liuzhenlin.h264codec;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class H264DecodingSampleActivity extends AppCompatActivity {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_h264_decoding_sample);
        SurfaceView surfaceView = findViewById(R.id.surfaceView);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            H264Player player;

            @SuppressLint("NewApi")
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                player = new H264Player(
                        H264DecodingSampleActivity.this,
                        H264ScreencapEncodingSampleActivity.SCREENCAP_FILE_PATH,
                        holder.getSurface());
                player.play();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                if (player != null) {
                    player.release();
                    player = null;
                }
            }
        });
    }
}
