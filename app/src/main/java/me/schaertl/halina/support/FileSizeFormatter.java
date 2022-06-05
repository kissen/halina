package me.schaertl.halina.support;

import android.annotation.SuppressLint;

import java.util.Arrays;
import java.util.List;

public class FileSizeFormatter {
    private FileSizeFormatter() {}

    /**
     * Format byte count into a human-readable format.
     *
     * For example, byte count 2048 is converted to "2K".
     *
     * @param nbytes Byte count.
     * @return Human-readable string representation of nbytes.
     */
    @SuppressLint("DefaultLocale")
    public static String format(int nbytes) {
        final List<String> units = Arrays.asList(
                "B", "KiB", "MiB", "GiB", "TiB"
        );

        int unitIdx = 0;
        int remaining = nbytes;

        while (remaining > 1024 && unitIdx < units.size() - 1) {
            remaining = remaining / 1024;
            unitIdx += 1;
        }

        final String unit = units.get(unitIdx);
        return String.format("%d %s", remaining, unit);
    }
}
