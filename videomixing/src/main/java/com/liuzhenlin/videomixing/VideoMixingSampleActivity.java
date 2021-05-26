package com.liuzhenlin.videomixing;

import android.media.AudioFormat;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.jaygoo.widget.RangeSeekBar;
import com.liuzhenlin.common.Files;
import com.liuzhenlin.common.utils.Executors;
import com.liuzhenlin.common.utils.MediaUtils;
import com.liuzhenlin.common.utils.PcmToWavUtil;
import com.liuzhenlin.common.utils.Synthetic;
import com.liuzhenlin.common.utils.Utils;

import java.io.File;
import java.io.IOException;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class VideoMixingSampleActivity extends AppCompatActivity implements View.OnClickListener,
        RangeSeekBar.OnRangeChangedListener, SeekBar.OnSeekBarChangeListener {
    @Synthetic VideoView mVideoView;
    @Synthetic RangeSeekBar mRangeSeekBar;
    private SeekBar mVideoVolumeSeekBar;
    private SeekBar mMusicVolumeSeekBar;
    private float mVideoVolume;
    private float mMusicVolume;
    private int mDuration;
    private final Runnable mCheckPlaybackPositionInRangeRunnable = new Runnable() {
        @Override
        public void run() {
            float[] range = mRangeSeekBar.getCurrentRange();
            if (mVideoView.getCurrentPosition() >= range[1] * RATIO_DURATION_TO_RANGE_MAX) {
                mVideoView.seekTo(Utils.roundFloat(range[0] * RATIO_DURATION_TO_RANGE_MAX));
            }
            mVideoView.postDelayed(this, 1000);
        }
    };
    private static final int RATIO_DURATION_TO_RANGE_MAX = 1000;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_mixing_sample);

        mVideoView = findViewById(R.id.videoView);

        mRangeSeekBar = findViewById(R.id.rangeSeekBar);
        mRangeSeekBar.setOnRangeChangedListener(this);

        mVideoVolumeSeekBar = findViewById(R.id.sb_videoVolume);
        mVideoVolumeSeekBar.setMax(100);
        mVideoVolumeSeekBar.setOnSeekBarChangeListener(this);

        mMusicVolumeSeekBar = findViewById(R.id.sb_musicVolume);
        mMusicVolumeSeekBar.setMax(100);
        mMusicVolumeSeekBar.setOnSeekBarChangeListener(this);

        findViewById(R.id.btn_mixing).setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startPlay(new File(Files.getAppExternalFilesDir(), "input.mp4").getAbsolutePath());
    }

    private void startPlay(String path) {
        mVideoView.setVideoPath(path);
        mVideoView.start();
        mVideoView.setOnPreparedListener(mp -> {
            mDuration = mp.getDuration();
            mp.setLooping(true);
            mRangeSeekBar.setRange(0, (float) mDuration / RATIO_DURATION_TO_RANGE_MAX);
            mRangeSeekBar.setValue(0, (float) mDuration / RATIO_DURATION_TO_RANGE_MAX);
            mVideoView.removeCallbacks(mCheckPlaybackPositionInRangeRunnable);
            mVideoView.post(mCheckPlaybackPositionInRangeRunnable);
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_mixing) {
            File parent = Files.getAppExternalFilesDir();
            String audioPath = new File(parent, "input.mp3").getAbsolutePath();
            String videoPath = new File(parent, "input.mp4").getAbsolutePath();
            String audioPcmPath = new File(parent, "music.pcm").getAbsolutePath();
            String videoPcmPath = new File(parent, "video.pcm").getAbsolutePath();
            String mixedPcmPath = new File(parent, "mixed.pcm").getAbsolutePath();
            String wavPath = new File(parent, "wave.wav").getAbsolutePath();
            String outputPath = new File(parent, "output.mp4").getAbsolutePath();
            Executors.MEDIA_CODEC_EXECUTOR.execute(() -> {
                float[] range = mRangeSeekBar.getCurrentRange();
                int start = Utils.roundFloat(range[0] * RATIO_DURATION_TO_RANGE_MAX * 1000);
                int end = Utils.roundFloat(range[1] * RATIO_DURATION_TO_RANGE_MAX * 1000);
                try {
                    MediaUtils.decodeAudioToPcm(videoPath, videoPcmPath, 0, end - start);
                    MediaUtils.decodeAudioToPcm(audioPath, audioPcmPath, start, end);
                    MediaUtils.mixPcms(videoPcmPath, audioPcmPath, mixedPcmPath,
                            mVideoVolume, mMusicVolume);
                    new PcmToWavUtil(44100, AudioFormat.CHANNEL_IN_STEREO,
                            2, AudioFormat.ENCODING_PCM_16BIT)
                            .pcmToWav(mixedPcmPath, wavPath);
                    MediaUtils.mixVideoAndAudio(videoPath, wavPath, outputPath, start, end);
                    Executors.MAIN_EXECUTOR.execute(() -> {
                        startPlay(outputPath);
                        Toast.makeText(VideoMixingSampleActivity.this,
                                R.string.editingIsComplete, Toast.LENGTH_SHORT).show();
                    });
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Override
    public void onRangeChanged(RangeSeekBar view, float min, float max, boolean isFromUser) {
        mVideoView.seekTo(Utils.roundFloat(min * RATIO_DURATION_TO_RANGE_MAX));
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            if (seekBar == mVideoVolumeSeekBar) {
                mVideoVolume = (float) progress / seekBar.getMax();
            } else if (seekBar == mMusicVolumeSeekBar) {
                mMusicVolume = (float) progress / seekBar.getMax();
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }
}
