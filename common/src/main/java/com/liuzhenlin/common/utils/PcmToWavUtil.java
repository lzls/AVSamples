package com.liuzhenlin.common.utils;

import android.media.AudioFormat;
import android.media.AudioRecord;

import androidx.annotation.NonNull;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by huangwei on 2018/2/9.
 */
public class PcmToWavUtil {

    private final int mSampleRate; // 8000|16000
    private final int mChannelConfig; // 立体声
    private final int mChannelCount;
    private final int mEncoding;
    private final int mBufferSize; // 缓存的音频大小

    public PcmToWavUtil() {
        mSampleRate = 8000;
        mChannelConfig = AudioFormat.CHANNEL_IN_STEREO;
        mChannelCount = 2;
        mEncoding = AudioFormat.ENCODING_PCM_16BIT;
        mBufferSize = AudioRecord.getMinBufferSize(mSampleRate, mChannelConfig, mEncoding);
    }

    /**
     * @param sampleRate    sample rate、采样率
     * @param channelConfig channel、声道
     * @param encoding      Audio data format、音频格式
     */
    public PcmToWavUtil(int sampleRate, int channelConfig, int channelCount, int encoding) {
        mSampleRate = sampleRate;
        mChannelConfig = channelConfig;
        mChannelCount = channelCount;
        mEncoding = encoding;
        mBufferSize = AudioRecord.getMinBufferSize(mSampleRate, mChannelConfig, mEncoding);
    }

    /**
     * pcm文件转wav文件
     *
     * @param inFilename  源文件路径
     * @param outFilename 目标文件路径
     */
    public void pcmToWav(@NonNull String inFilename, @NonNull String outFilename) {
        FileInputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);

            long totalAudioLen = in.getChannel().size();
            long totalDataLen = totalAudioLen + 36;
            long byteRate = 16 * mSampleRate * mChannelCount / 8;
            writeWaveFileHeader(out, totalAudioLen, totalDataLen,
                    mSampleRate, mChannelCount, byteRate);

            int len;
            byte[] data = new byte[mBufferSize];
            while ((len = in.read(data)) != -1) {
                out.write(data, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeSilently(out);
            IOUtils.closeSilently(in);
        }
    }

    /**
     * 加入wav文件头
     */
    private void writeWaveFileHeader(
            OutputStream out, long totalAudioLen, long totalDataLen,
            long sampleRate, int channels, long byteRate)
            throws IOException {
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W'; // WAVE
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (sampleRate & 0xff);
        header[25] = (byte) ((sampleRate >> 8) & 0xff);
        header[26] = (byte) ((sampleRate >> 16) & 0xff);
        header[27] = (byte) ((sampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8); // block align
        header[33] = 0;
        header[34] = 16; // bits per sample
        header[35] = 0;
        header[36] = 'd'; // data
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        out.write(header, 0, 44);
    }
}
