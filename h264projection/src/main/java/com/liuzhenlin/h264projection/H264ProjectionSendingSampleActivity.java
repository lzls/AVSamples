package com.liuzhenlin.h264projection;

import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.liuzhenlin.common.socket.SocketServer;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class H264ProjectionSendingSampleActivity extends AppCompatActivity {

    private static final int REQUEST_SCREEN_CAPTURE = 1;
    private MediaProjectionManager mMediaProjectionManager;
    private SocketServer mSocketServer;
    static final int SOCKET_PORT = 8080;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMediaProjectionManager = (MediaProjectionManager)
                getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent it = mMediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(it, REQUEST_SCREEN_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SCREEN_CAPTURE && resultCode == RESULT_OK) {
            MediaProjection mediaProjection =
                    mMediaProjectionManager.getMediaProjection(resultCode, data);
            mSocketServer = new SocketServer(SOCKET_PORT);
            mSocketServer.start();
            new H264ProjectionSender(mSocketServer).startCaptureAndSend(mediaProjection, this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSocketServer != null) {
            mSocketServer.stop();
        }
    }
}
