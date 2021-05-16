package com.liuzhenlin.gifloader;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import java.io.File;

import static com.liuzhenlin.common.Consts.NULL;

public class GifLoadSampleActivity extends Activity {

    private LoadGifTask mLoadGifTask;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gif_load_sample);
        mLoadGifTask = new LoadGifTask(findViewById(R.id.image_gif));
        mLoadGifTask.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLoadGifTask.stop();
    }

    private static final class LoadGifTask implements Runnable {
        long mNativeGifLoader;

        final ImageView mImgView;
        Bitmap mBmp;

        LoadGifTask(ImageView imgView) {
            mImgView = imgView;
        }

        @Override
        public void run() {
            if (mNativeGifLoader != NULL) {
                int delay = GifLoader.updateFrame(mNativeGifLoader, mBmp);
                mImgView.setImageBitmap(mBmp);
                mImgView.postDelayed(this, delay);
            }
        }

        void start() {
            if (mNativeGifLoader == NULL) {
                File gif = new File(Environment.getExternalStorageDirectory(), "demo.gif");
                mNativeGifLoader = GifLoader.load(gif.getAbsolutePath());
                mBmp = Bitmap.createBitmap(
                        GifLoader.getGifWidth(mNativeGifLoader),
                        GifLoader.getGifHeight(mNativeGifLoader),
                        Bitmap.Config.ARGB_8888);
                mImgView.post(this);
            }
        }

        void stop() {
            mImgView.removeCallbacks(this);
            if (mNativeGifLoader != NULL) {
                GifLoader.release(mNativeGifLoader);
                mNativeGifLoader = NULL;
            }
        }
    }
}
