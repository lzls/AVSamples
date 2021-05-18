package com.liuzhenlin.h264projection;

import android.app.Activity;
import android.hardware.display.DisplayManager;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.liuzhenlin.common.utils.Executors;
import com.liuzhenlin.common.socket.SocketServer;

import java.io.IOException;
import java.nio.ByteBuffer;

import static com.liuzhenlin.common.Configs.MEDIA_CODEC_DEQUEUE_BUFFER_TIMEOUT;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class H264ProjectionSender {

    //TODO: remove below fields when size can be dynamically got
    static final int SCREENCAP_WIDTH = 1080;
    static final int SCREENCAP_HEIGHT = 1920;

    private byte[] mSpsPps;
    private static final int NAL_SPS = 7;
    private static final int NAL_I = 5;

    private final SocketServer mSocketServer;

    public H264ProjectionSender(@NonNull SocketServer socketServer) {
        mSocketServer = socketServer;
    }

    public void startCaptureAndSend(@NonNull MediaProjection projection, @NonNull Activity activity) {
        MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,
                SCREENCAP_WIDTH, SCREENCAP_HEIGHT);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 4000_000);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);

        MediaCodec codec;
        try {
            codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        Surface surface = codec.createInputSurface();
        codec.start();

        Executors.THREAD_POOL_EXECUTOR.execute(() -> {
            projection.createVirtualDisplay("screencap_projection",
                    SCREENCAP_WIDTH, SCREENCAP_HEIGHT, 1,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, surface,
                    null, null);
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int bufferIndex = -1;
            while (true) {
                if (activity.isFinishing()) {
                    projection.stop();
                    codec.stop();
                    codec.release();
                    break;
                }
                do {
                    //noinspection WrongConstant
                    bufferIndex = codec.dequeueOutputBuffer(bufferInfo,
                            MEDIA_CODEC_DEQUEUE_BUFFER_TIMEOUT);
                } while (bufferIndex < 0);
                ByteBuffer buffer = codec.getOutputBuffer(bufferIndex);
                dealWithFrame(buffer, bufferInfo);
                codec.releaseOutputBuffer(bufferIndex, false);
            }
        });
    }

    private void dealWithFrame(ByteBuffer buffer, MediaCodec.BufferInfo info) {
        int offset = 4;
        if (buffer.get(2) == 0x01) {
            offset = 3;
        }
        int type = buffer.get(offset) & 0x1f;
        switch (type) {
            case NAL_SPS:
                mSpsPps = new byte[info.size];
                buffer.get(mSpsPps);
                break;
            case NAL_I: {
                byte[] bytes = new byte[mSpsPps.length + info.size];
                System.arraycopy(mSpsPps, 0, bytes, 0, mSpsPps.length);
                buffer.get(bytes, mSpsPps.length, info.size);
                mSocketServer.sendData(bytes);
                break;
            }
            default: {
                byte[] bytes = new byte[info.size];
                buffer.get(bytes);
                mSocketServer.sendData(bytes);
                break;
            }
        }
    }
}
