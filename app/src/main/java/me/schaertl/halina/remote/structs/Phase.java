package me.schaertl.halina.remote.structs;

import android.annotation.SuppressLint;

public enum Phase {
    QUEUED, DOWNLOADING, EXTRACTING, VALIDATING;

    @SuppressLint("DefaultLocale")
    public String format(Progress progress) {
        if (progress == null) {
            return this.prefix();
        }

        final String name = this.prefix();
        final int percent = Math.round(progress.percent());

        return String.format("%s (%d%%)", name, percent);
    }

    private String prefix() {
        switch (this) {
            case QUEUED:
                return "Preparing";
            case DOWNLOADING:
                return "Downloading";
            case EXTRACTING:
                return "Extracting";
            case VALIDATING:
                return "Validating";
            default:
                throw new IllegalArgumentException("unknown enum value");
        }
    }
}
