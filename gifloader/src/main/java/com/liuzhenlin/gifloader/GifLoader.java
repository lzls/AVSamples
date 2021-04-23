package com.liuzhenlin.gifloader;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

public class GifLoader {

    static {
        System.loadLibrary("gifloader-lib");
    }

    private GifLoader() {
    }

    public static native long load(@NonNull String path);
    public static native int getGifWidth(long nativeGifLoader);
    public static native int getGifHeight(long nativeGifLoader);
    public static native int updateFrame(long nativeGifLoader, @NonNull Bitmap bmp);
    public static native void release(long nativeGifLoader);
}
