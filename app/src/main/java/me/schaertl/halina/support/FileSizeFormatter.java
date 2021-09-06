package me.schaertl.halina.support;

import java.util.Arrays;
import java.util.List;

public class FileSizeFormatter {
    private FileSizeFormatter() {}

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
