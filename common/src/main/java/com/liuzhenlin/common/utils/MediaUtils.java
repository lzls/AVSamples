package com.liuzhenlin.common.utils;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import static com.liuzhenlin.common.Configs.DEFAULT_MEDIA_MAX_BUFFER_SIZE;
import static com.liuzhenlin.common.Configs.MEDIA_CODEC_DEQUEUE_BUFFER_TIMEOUT;
import static com.liuzhenlin.common.Consts.MAX_AUDIO_WAVE;
import static com.liuzhenlin.common.Consts.MIN_AUDIO_WAVE;

public class MediaUtils {
    private MediaUtils() {
    }

    public static void decodeAudioToPcm(
            @NonNull String filePath, @NonNull String pcmFilePath, long startTimeUs, long endTimeUs)
            throws IOException {
        if (startTimeUs >= endTimeUs || (startTimeUs | endTimeUs) < 0) {
            throw new IllegalArgumentException("Illegal startTimeUs or endTimeUs");
        }

        MediaExtractor extractor = null;
        MediaCodec codec = null;
        OutputStream pcmFileWriter = null;
        try {
            extractor = new MediaExtractor();
            extractor.setDataSource(filePath);
            int audioTrack = selectTrack(extractor, true);
            extractor.selectTrack(audioTrack);
            MediaFormat audioFormat = extractor.getTrackFormat(audioTrack);

            codec = MediaCodec.createDecoderByType(audioFormat.getString(MediaFormat.KEY_MIME));
            codec.configure(audioFormat, null, null, 0);
            codec.start();

            long sampleTimeUs;
            int bufferIndex;
            ByteBuffer[] inBuffers = codec.getInputBuffers();
            ByteBuffer[] outBuffers = codec.getOutputBuffers();
            int maxInputSize;
            if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                maxInputSize = audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
            } else {
                maxInputSize = DEFAULT_MEDIA_MAX_BUFFER_SIZE;
            }
            ByteBuffer buffer = ByteBuffer.allocateDirect(maxInputSize);
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            pcmFileWriter = new BufferedOutputStream(new FileOutputStream(pcmFilePath));

            extractor.seekTo(startTimeUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
            while (true) {
                sampleTimeUs = extractor.getSampleTime();
                if (sampleTimeUs == -1) {
                    break;
                } else if (sampleTimeUs < startTimeUs) {
                    extractor.advance();
                    continue;
                } else if (sampleTimeUs > endTimeUs) {
                    break;
                }
                info.size = extractor.readSampleData(buffer, 0);
                info.presentationTimeUs = sampleTimeUs - startTimeUs;
                info.flags = extractor.getSampleFlags();

                do {
                    bufferIndex = codec.dequeueInputBuffer(MEDIA_CODEC_DEQUEUE_BUFFER_TIMEOUT);
                } while (bufferIndex < 0);
                inBuffers[bufferIndex].clear();
                inBuffers[bufferIndex].put(buffer);
                codec.queueInputBuffer(bufferIndex, 0, info.size, info.presentationTimeUs, info.flags);

                boolean hasOutputBuffer;
                do {
                    bufferIndex = codec.dequeueOutputBuffer(info, MEDIA_CODEC_DEQUEUE_BUFFER_TIMEOUT);
                    if (hasOutputBuffer = bufferIndex >= 0) {
                        for (int i = 0, len = outBuffers[bufferIndex].remaining(); i < len; i++) {
                            pcmFileWriter.write(outBuffers[bufferIndex].get(i));
                        }
                        codec.releaseOutputBuffer(bufferIndex, false);
                    } else if (bufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        outBuffers = codec.getOutputBuffers();
                    }
                } while (hasOutputBuffer);

                buffer.clear();
                extractor.advance();
            }
        } finally {
            IOUtils.closeSilently(pcmFileWriter);
            if (codec != null) {
                codec.stop();
                codec.release();
            }
            if (extractor != null) {
                extractor.release();
            }
        }
    }

    public static void mixPcms(
            @NonNull String pcm1Path, @NonNull String pcm2Path, @NonNull String outPath,
            @FloatRange(from = 0, to = 1) float volume1, @FloatRange(from = 0, to = 1) float volume2)
            throws IOException {
        checkVolume(volume1);
        checkVolume(volume2);
        InputStream pcm1In = null;
        InputStream pcm2In = null;
        OutputStream out = null;
        try {
            pcm1In = new FileInputStream(pcm1Path);
            pcm2In = new FileInputStream(pcm2Path);
            out = new BufferedOutputStream(new FileOutputStream(outPath));

            int bufferSize = 8 * 1024;
            byte[] data1 = new byte[bufferSize];
            byte[] data2 = new byte[bufferSize];

            boolean pcm1End = false, pcm2End = false;
            int len1, len2;
            short sample1, sample2;
            int sample;
            IntArray samples = new IntArray();

            while (!pcm1End || !pcm2End) {
                //noinspection StatementWithEmptyBody
                if (!(pcm1End = (len1 = pcm1In.read(data1)) == -1)) {
                }
                if (len1 < 0) {
                    len1 = 0;
                }
                for (int i = len1; i < data1.length - 1 - len1; i++) {
                    data1[i] = (byte) 0;
                }
                if (!(pcm2End = (len2 = pcm2In.read(data2)) == -1)) {
                    for (int i = 0; i < len2 - 1; i += 2) {
                        sample1 = (short) ((data1[i] & 0xff) | ((data1[i + 1] & 0xff) << 8));
                        sample2 = (short) ((data2[i] & 0xff) | ((data2[i + 1] & 0xff) << 8));
                        sample = Utils.roundFloat(sample1 * volume1 + sample2 * volume2);
//                        writeAudioSample(out, sample);
                        samples.add(sample);
                    }
                }
                if (len2 < 0) {
                    len2 = 0;
                }
                if (len2 < len1) {
                    for (int i = len1 - 1 - len2; i < len1 - 1; i += 2) {
                        sample1 = (short) ((data1[i] & 0xff) | ((data1[i + 1] & 0xff) << 8));
                        sample = Utils.roundFloat(sample1 * volume1);
//                        writeAudioSample(out, sample);
                        samples.add(sample);
                    }
                }
            }

            int maxSample = 0;
            int minSample = 0;
            int sampleCount = samples.size();
            for (int i = 0; i < sampleCount; i++) {
                sample = samples.get(i);
                maxSample = Math.max(sample, maxSample);
                minSample = Math.min(sample, minSample);
            }
            float sampleRatio = Math.min(
                    (float) MAX_AUDIO_WAVE / maxSample, (float) MIN_AUDIO_WAVE / minSample);
            for (int i = 0; i < sampleCount; i++) {
                sample = Utils.roundFloat(samples.get(i) * sampleRatio);
                out.write(sample & 0xff);
                out.write(sample >> 8);
            }
        } finally {
            IOUtils.closeSilently(out);
            IOUtils.closeSilently(pcm1In);
            IOUtils.closeSilently(pcm2In);
        }
    }

    private static void checkVolume(float volume) {
        if (volume < 0 || volume > 1) {
            throw new IllegalArgumentException("`volume` must be between 0 and 1");
        }
    }

//    private static void writeAudioSample(OutputStream writer, int sample) throws IOException {
//        if (sample > MAX_AUDIO_SAMPLE) {
//            sample = MAX_AUDIO_SAMPLE;
//        } else if (sample < -MIN_AUDIO_SAMPLE) {
//            sample = -MIN_AUDIO_SAMPLE;
//        }
//        writer.write(sample & 0xff);
//        writer.write(sample >> 8);
//    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void mixVideoAndAudio(
            @NonNull String videoInput, @NonNull String wav, @NonNull String videoOutput,
            long startTimeUs, long endTimeUs) throws IOException {
        if (startTimeUs >= endTimeUs || (startTimeUs | endTimeUs) < 0) {
            throw new IllegalArgumentException("Illegal startTimeUs or endTimeUs");
        }

        MediaMuxer muxer = null;
        MediaExtractor videoExtractor = null;
        MediaExtractor pcmExtractor = null;
        MediaCodec pcmEncoder = null;
        try {
            videoExtractor = new MediaExtractor();
            videoExtractor.setDataSource(videoInput);
            int videoTrack = selectTrack(videoExtractor, false);
            int audioTrack = selectTrack(videoExtractor, true);
            MediaFormat videoFormat = videoExtractor.getTrackFormat(videoTrack);
            MediaFormat audioFormat = videoExtractor.getTrackFormat(audioTrack);
            audioFormat.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC);

            pcmExtractor = new MediaExtractor();
            pcmExtractor.setDataSource(wav);
            int pcmTrack = selectTrack(pcmExtractor, true);

            pcmEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            MediaFormat encoderFormat = MediaFormat.createAudioFormat(
                    MediaFormat.MIMETYPE_AUDIO_AAC, 44100, 2);
            encoderFormat.setInteger(
                    MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            encoderFormat.setInteger(
                    MediaFormat.KEY_BIT_RATE, audioFormat.getInteger(MediaFormat.KEY_BIT_RATE));
            int maxInputSize;
            if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                maxInputSize = audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                encoderFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, maxInputSize);
            } else {
                maxInputSize = DEFAULT_MEDIA_MAX_BUFFER_SIZE;
            }
            pcmEncoder.configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            pcmEncoder.start();

            muxer = new MediaMuxer(videoOutput, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            int outVideoTrackIndex = muxer.addTrack(videoFormat);
            int outAudioTrackIndex = muxer.addTrack(audioFormat);
            muxer.start();

            long videoEndSampleTimeUs = -1;
            long audioStartSampleTimeUs = -1;
            long sampleTimeUs;
            int bufferIndex;
            ByteBuffer[] inBuffers = pcmEncoder.getInputBuffers();
            ByteBuffer[] outBuffers = pcmEncoder.getOutputBuffers();
            if (videoFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                maxInputSize = Math.max(
                        maxInputSize, videoFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE));
            } else {
                maxInputSize = Math.max(maxInputSize, DEFAULT_MEDIA_MAX_BUFFER_SIZE);
            }
            ByteBuffer buffer = ByteBuffer.allocateDirect(maxInputSize);
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

            videoExtractor.selectTrack(videoTrack);
            videoExtractor.seekTo(startTimeUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
            while (true) {
                sampleTimeUs = videoExtractor.getSampleTime();
                if (sampleTimeUs == -1) {
                    break;
                } else if (sampleTimeUs < startTimeUs) {
                    videoExtractor.advance();
                    continue;
                } else if (sampleTimeUs > endTimeUs) {
                    break;
                }
                info.size = videoExtractor.readSampleData(buffer, 0);
                videoEndSampleTimeUs = sampleTimeUs;
                info.presentationTimeUs = sampleTimeUs - startTimeUs;
                info.flags = videoExtractor.getSampleFlags();
                muxer.writeSampleData(outVideoTrackIndex, buffer, info);
                buffer.clear();
                videoExtractor.advance();
            }

            pcmExtractor.selectTrack(pcmTrack);
            while (true) {
                sampleTimeUs = pcmExtractor.getSampleTime();
                if (audioStartSampleTimeUs == -1) {
                    audioStartSampleTimeUs = sampleTimeUs;
                }
                if (sampleTimeUs == -1) {
                    break;
                } else if (sampleTimeUs - audioStartSampleTimeUs > videoEndSampleTimeUs - startTimeUs) {
                    break;
                }
                info.size = pcmExtractor.readSampleData(buffer, 0);
                info.presentationTimeUs = sampleTimeUs - audioStartSampleTimeUs;
                info.flags = pcmExtractor.getSampleFlags();

                do {
                    bufferIndex = pcmEncoder.dequeueInputBuffer(MEDIA_CODEC_DEQUEUE_BUFFER_TIMEOUT);
                } while (bufferIndex < 0);
                inBuffers[bufferIndex].clear();
                inBuffers[bufferIndex].put(buffer);
                pcmEncoder.queueInputBuffer(bufferIndex, 0, info.size, info.presentationTimeUs, info.flags);

                boolean hasOutputBuffer;
                do {
                    bufferIndex = pcmEncoder.dequeueOutputBuffer(info,
                            MEDIA_CODEC_DEQUEUE_BUFFER_TIMEOUT);
                    if (hasOutputBuffer = bufferIndex >= 0) {
                        muxer.writeSampleData(outAudioTrackIndex, outBuffers[bufferIndex], info);
                        pcmEncoder.releaseOutputBuffer(bufferIndex, false);
                    } else if (bufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        outBuffers = pcmEncoder.getOutputBuffers();
                    }
                } while (hasOutputBuffer);

                buffer.clear();
                pcmExtractor.advance();
            }
        } finally {
            if (muxer != null) {
                muxer.stop();
                muxer.release();
            }
            if (pcmEncoder != null) {
                pcmEncoder.stop();
                pcmEncoder.release();
            }
            if (pcmExtractor != null) {
                pcmExtractor.release();
            }
            if (videoExtractor != null) {
                videoExtractor.release();
            }
        }
    }

    private static int selectTrack(MediaExtractor extractor, boolean audio) {
        for (int i = 0, trackCount = extractor.getTrackCount(); i < trackCount; i++) {
            String mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME);
            if (!TextUtils.isEmpty(mime)) {
                if (audio) {
                    if (mime.startsWith("audio/")) {
                        return i;
                    }
                } else if (mime.startsWith("video/")) {
                    return i;
                }
            }
        }
        return -1;
    }
}
