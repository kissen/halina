package me.schaertl.halina.storage;

import android.annotation.SuppressLint;
import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import me.schaertl.halina.support.Guard;

public class Storage {
    public static final String DB_NAME = "wiki.sqlite3";
    private static final Lock lock = new ReentrantLock();

    private Storage() {}

    /**
     * Install a new dictionary file.
     *
     * @param context Application context.
     * @param temporaryFilePath File location of the sqlite3 dictionary. Will be deleted.
     */
    public static void installNewDictionary(Context context, String temporaryFilePath) throws IOException {
        try (final Guard guard = guard()) {
            final Path srcPath = Paths.get(temporaryFilePath);
            final Path dstPath = Paths.get(getDatabasePath(context));
            Files.move(srcPath, dstPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Return whether we a database is installed, i.e. whether a dictionary has been
     * downloaded and installed with installNewDictionary() before.
     *
     * @param context Application context that owns the database file.
     */
    public static boolean haveDatabase(Context context) {
        try (final Guard guard = guard()) {
            final String path = getDatabasePath(context);
            return new File(path).exists();
        }
    }

    /**
     * Return the storage lock. Code opening the database should do so only while holding the
     * returned lock. This is to avoid races between installNewDictionary() and read accesses.
     */
    public static Guard guard() {
        return new Guard(lock);
    }

    /**
     * Return path to dictionary database on the file system.
     *
     * @param context Application context that owns the database file.
     * @return Absolute path to dictionary file.
     */
    public static String getDatabasePath(Context context) {
        return context.getDatabasePath(DB_NAME).getAbsolutePath();
    }
}
