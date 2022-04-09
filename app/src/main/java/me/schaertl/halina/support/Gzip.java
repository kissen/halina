package me.schaertl.halina.support;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

public class Gzip {
    private Gzip() {}

    public static void extract(String gzipFileName, String rawFileName) throws IOException {
        final int bufsize = 4096;
        final int EOF = -1;

        try (final FileInputStream fis = new FileInputStream(gzipFileName)) {
            try (final GZIPInputStream gis = new GZIPInputStream(fis, bufsize)) {
                try (final FileOutputStream fos = new FileOutputStream(rawFileName)) {
                    final byte[] buffer = new byte[bufsize];
                    int nbytes;

                    while ((nbytes = gis.read(buffer)) != EOF) {
                        fos.write(buffer, 0, nbytes);
                    }
                }
            }
        }
    }
}
