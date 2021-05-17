package com.liuzhenlin.h264codec;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.liuzhenlin.common.utils.Executors;
import com.liuzhenlin.common.utils.IOUtils;
import com.liuzhenlin.common.utils.LooperExecutor;
import com.liuzhenlin.common.utils.Synthetic;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import static com.liuzhenlin.common.Configs.MEDIA_CODEC_DEQUEUE_BUFFER_TIMEOUT;

public class H264Player {

    private final Context mContext;
    private final String mFilePath;
    private final Surface mSurface;
    @Synthetic MediaCodec mMediaCodec;
    @Synthetic boolean mStarted;
    @Synthetic final LooperExecutor mExecutor = Executors.MEDIA_CODEC_EXECUTOR;

    public H264Player(@NonNull Context context, @NonNull String filePath, @NonNull Surface surface) {
        mContext = context;
        mFilePath = filePath;
        mSurface = surface;
    }

    @SuppressLint("InlinedApi")
    public void play() {
        mExecutor.post(() -> {
            if (mMediaCodec != null && mStarted) {
                return;
            }
            try {
                if (mMediaCodec == null) {
                    mMediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
                    @SuppressLint("NewApi") MediaFormat mediaFormat =
                            MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,
                                    H264ScreencapEncodingSampleActivity.SCREENCAP_WIDTH,
                                    H264ScreencapEncodingSampleActivity.SCREENCAP_HEIGHT); //TODO: obtain size from the video content
                    mMediaCodec.configure(mediaFormat, mSurface, null, 0);
                }
                mMediaCodec.start();
                mStarted = true;

                byte[] bytes = IOUtils.toByteArray(new File(mFilePath));
                ByteBuffer[] buffers = mMediaCodec.getInputBuffers();
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                new Runnable() {
                    int bufferIndex = -1;
                    int frameStartPos = 0;
                    int nextFrameStartPos = 0;

                    @Override
                    public void run() {
                        if (mStarted) {
                            nextFrameStartPos = findNextFrameStartPos(bytes, frameStartPos);
                            if (nextFrameStartPos < 0) {
                                return;
                            }

                            do {
                                bufferIndex = mMediaCodec.dequeueInputBuffer(
                                        MEDIA_CODEC_DEQUEUE_BUFFER_TIMEOUT);
                            } while (bufferIndex < 0);
                            ByteBuffer buffer = buffers[bufferIndex];
                            buffer.clear();
                            buffer.put(bytes, frameStartPos, nextFrameStartPos - frameStartPos);
                            mMediaCodec.queueInputBuffer(bufferIndex,
                                    0, buffer.remaining(),
                                    0, 0);

                            do {
                                //noinspection WrongConstant
                                bufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo,
                                        MEDIA_CODEC_DEQUEUE_BUFFER_TIMEOUT);
                            } while (bufferIndex < 0);
                            mMediaCodec.releaseOutputBuffer(bufferIndex, true);

                            frameStartPos = nextFrameStartPos;
                            mExecutor.post(this);
                        }
                    }
                }.run();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void pause() {
        mExecutor.post(this::pauseInternal);
    }

    private void pauseInternal() {
        if (mMediaCodec != null && mStarted) {
            mStarted = false;
            mMediaCodec.stop();
        }
    }

    public void release() {
        mExecutor.post(() -> {
            if (mMediaCodec != null) {
                pauseInternal();
                mMediaCodec.release();
                mMediaCodec = null;
            }
            mExecutor.remove(null);
        });
    }

    @Synthetic int findNextFrameStartPos(byte[] bytes, int start) {
        for (int i = start + 2; i < bytes.length - 4; i++) {
            if (bytes[i] == 0x00 && bytes[i + 1] == 0x00
                    && (bytes[i + 2] == 0x01 || bytes[i + 2] == 0x00 && bytes[i + 3] == 0x01)) {
                return i;
            }
        }
        return -1;
    }
}
