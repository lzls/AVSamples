package com.liuzhenlin.common;

import android.os.Environment;

import androidx.annotation.NonNull;

import java.io.File;

public class Files {
    public static final String EXTERNAL_FILES_FOLDER = "AVSamples_lzl";

    @NonNull
    public static File getAppExternalFilesDir() {
        File dir = new File(Environment.getExternalStorageDirectory(), EXTERNAL_FILES_FOLDER);
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        return dir;
    }
}
