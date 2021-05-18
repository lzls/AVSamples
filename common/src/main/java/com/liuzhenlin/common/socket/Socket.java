package com.liuzhenlin.common.socket;

import androidx.annotation.NonNull;

public interface Socket {
    void sendData(@NonNull byte[] bytes);

    interface Callback {
        void onData(@NonNull byte[] data);
    }
}
