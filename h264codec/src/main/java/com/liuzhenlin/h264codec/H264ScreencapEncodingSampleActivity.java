package com.liuzhenlin.h264codec;

import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Surface;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.liuzhenlin.common.Files;
import com.liuzhenlin.common.utils.Executors;
import com.liuzhenlin.common.utils.IOUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import static com.liuzhenlin.common.Configs.MEDIA_CODEC_DEQUEUE_BUFFER_TIMEOUT;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class H264ScreencapEncodingSampleActivity extends AppCompatActivity {

    private static final int REQUEST_SCREEN_CAPTURE = 1;

    static final String SCREENCAP_FILE_NAME = "screencap.h264";
    static final String SCREENCAP_FILE_PATH =
            Files.getAppExternalFilesDir() + "/" + SCREENCAP_FILE_NAME;
    //TODO: remove below fields when size can be dynamically got
    static final int SCREENCAP_WIDTH = 1080;
    static final int SCREENCAP_HEIGHT = 1920;

    private MediaProjectionManager mMediaProjectionManager;

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
            try {
                capture(mediaProjection);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void capture(MediaProjection projection) throws IOException {
        MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,
                SCREENCAP_WIDTH, SCREENCAP_HEIGHT);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 4000_000);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);

        MediaCodec codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        Surface surface = codec.createInputSurface();
        codec.start();

        Executors.THREAD_POOL_EXECUTOR.execute(() -> {
            projection.createVirtualDisplay("screencap",
                    SCREENCAP_WIDTH, SCREENCAP_HEIGHT, 1,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, surface,
                    null, null);
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int bufferIndex = -1;
            byte[] linefeed = "\n".getBytes();
            File file = new File(SCREENCAP_FILE_PATH);
            OutputStream fileWriter = null;
            try {
                fileWriter = new BufferedOutputStream(new FileOutputStream(file, true));
                while (true) {
                    if (isFinishing()) {
                        break;
                    }
                    do {
                        //noinspection WrongConstant
                        bufferIndex = codec.dequeueOutputBuffer(bufferInfo,
                                MEDIA_CODEC_DEQUEUE_BUFFER_TIMEOUT);
                    } while (bufferIndex < 0);
                    ByteBuffer buffer = codec.getOutputBuffer(bufferIndex);
                    byte[] outData = new byte[bufferInfo.size + linefeed.length];
                    buffer.get(outData, 0, bufferInfo.size);
                    System.arraycopy(linefeed, 0,
                            outData, outData.length - 1 - linefeed.length, linefeed.length);
                    fileWriter.write(outData);
                    codec.releaseOutputBuffer(bufferIndex, false);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                IOUtils.closeSilently(fileWriter);
                projection.stop();
                codec.stop();
                codec.release();
            }
        });
    }
}
