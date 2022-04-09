package me.schaertl.halina.remote.structs;

import android.annotation.SuppressLint;

public enum Phase {
    QUEUED, DOWNLOADING, EXTRACTING, VALIDATING;

    @SuppressLint("DefaultLocale")
    public String format(Progress progress) {
        // If we don't have any progress information, we simply only print the string.
        // This can happen in situations where operations are fairly quick and as such
        // I was too lazy to implement an update/progress mechanism.

        if (progress == null) {
            return String.format("%s...", this.prefix());
        }

        // I guess we do have a progress. Here we format it as a percentage.

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
