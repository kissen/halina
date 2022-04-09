package me.schaertl.halina.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Fs {
    private Fs() {}

    public static void delete(Path fileName) throws IOException  {
        Files.delete(fileName);
    }

    public static void delete(String fileName) throws IOException  {
        final Path path = Paths.get(fileName);
        Files.delete(path);
    }

    public static String createTempDirectory(String prefix) throws IOException {
        return Files.createTempDirectory(prefix).toString();
    }

    public static String join(String base, String... components) {
        return Paths.get(base, components).toString();
    }
}
