package com.liuzhenlin.common;

public class Consts {
    private Consts() {
    }

    public static final long NULL = 0L;

    public static final String REGEX_IP_ADDRESS =
            "^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9])"
            + "(\\.(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)){3}$";

    public static final int MAX_AUDIO_WAVE = 32767;
    public static final int MIN_AUDIO_WAVE = -32768;
}
