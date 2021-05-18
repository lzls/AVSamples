package com.liuzhenlin.h264projection;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.liuzhenlin.common.utils.Executors;
import com.liuzhenlin.common.socket.SocketClient;

import java.io.IOException;
import java.nio.ByteBuffer;

import static com.liuzhenlin.common.Configs.MEDIA_CODEC_DEQUEUE_BUFFER_TIMEOUT;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class H264ProjectionReceiver extends MediaCodec.Callback implements SocketClient.Callback {

    private MediaCodec mMediaCodec;
    private final MediaCodec.BufferInfo mBufferInfo =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M ? new MediaCodec.BufferInfo() : null;
    private final Handler mHandler = Executors.MEDIA_CODEC_EXECUTOR.getHandler();

    public H264ProjectionReceiver() {
    }

    public void start(@NonNull Surface surface) {
        mHandler.post(() -> {
            if (mMediaCodec != null) {
                return;
            }
            try {
                mMediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            @SuppressLint("NewApi") MediaFormat mediaFormat =
                    MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,
                            H264ProjectionSender.SCREENCAP_WIDTH,
                            H264ProjectionSender.SCREENCAP_HEIGHT); //TODO: obtain size from the video content
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mMediaCodec.setCallback(H264ProjectionReceiver.this, mHandler);
            }
            mMediaCodec.configure(mediaFormat, surface, null, 0);
            mMediaCodec.start();
        });
    }

    public void stop() {
        mHandler.post(() -> {
            if (mMediaCodec != null) {
                mMediaCodec.stop();
                mMediaCodec.release();
                mMediaCodec = null;
            }
        });
    }

    @Override
    public void onData(@NonNull byte[] data) {
        mHandler.post(() -> {
            if (mMediaCodec == null) return;

            int bufferIndex = -1;
            do {
                bufferIndex = mMediaCodec.dequeueInputBuffer(MEDIA_CODEC_DEQUEUE_BUFFER_TIMEOUT);
            } while (bufferIndex < 0);
            ByteBuffer buffer = mMediaCodec.getInputBuffer(bufferIndex);
            buffer.clear();
            buffer.put(data);
            mMediaCodec.queueInputBuffer(bufferIndex, 0, data.length, 0, 0);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                boolean hasBuffer = false;
                do {
                    bufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo,
                            MEDIA_CODEC_DEQUEUE_BUFFER_TIMEOUT);
                    if (hasBuffer = bufferIndex >= 0) {
                        mMediaCodec.releaseOutputBuffer(bufferIndex, true);
                    }
                } while (hasBuffer);
            }
        });
    }

    @Override
    public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {

    }

    @Override
    public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
        codec.releaseOutputBuffer(index, true);
    }

    @Override
    public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {

    }

    @Override
    public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {

    }
}
