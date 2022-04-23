package me.schaertl.halina.support;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import me.schaertl.halina.remote.structs.Progress;
import me.schaertl.halina.remote.structs.ProgressHandler;

public class Gzip {
    private final static int BUFSIZE = 4096;
    private final static int EOF = -1;

    private Gzip() {}

    public static void extract(String gzipFileName, String rawFileName, ProgressHandler progressHandler) throws IOException {
        final long gzipBytes = Fs.fileSize(gzipFileName);
        extract(gzipFileName, rawFileName, gzipBytes, progressHandler);
    }

    private static void extract(String gzip, String dst, long gzipSize, ProgressHandler progressHandler) throws IOException {
        try (final FileInputStream fis = new FileInputStream(gzip)) {
            try (final GZIPInputStream gis = new GZIPInputStream(fis, BUFSIZE)) {
                try (final FileOutputStream fos = new FileOutputStream(dst)) {
                    final byte[] buffer = new byte[BUFSIZE];
                    long bytesTotal = 0;
                    int niter = 0;
                    int bytes;

                    while ((bytes = gis.read(buffer)) != EOF) {
                        // (1) Write to output, that is write to extracted file.

                        fos.write(buffer, 0, bytes);

                        // (2) Update progress report.

                        bytesTotal += bytes;
                        niter += 1;

                        if (niter % 512 == 0) {
                            final Progress progress = new Progress(bytesTotal, gzipSize);
                            progressHandler.onProgress(progress);
                        }
                    }
                }
            }
        }
    } // ouch!
}
