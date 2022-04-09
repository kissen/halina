package me.schaertl.halina.storage;

import android.content.Context;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import me.schaertl.halina.support.Guard;

public class Storage {
    public static final String DB_NAME = "wiki.sqlite3";
    private static final Lock lock = new ReentrantLock();

    private Storage() {}

    public static void installNewDictionary(Context context, String temporaryFilePath) throws IOException {
        try (final Guard guard = new Guard(getLock())) {
            final Path srcPath = Paths.get(temporaryFilePath);
            final Path dstPath = Paths.get(getDatabasePath(context));
            Files.move(srcPath, dstPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static Lock getLock() {
        return lock;
    }

    public static String getDatabasePath(Context context) {
        return context.getDatabasePath(DB_NAME).getAbsolutePath();
    }
}
