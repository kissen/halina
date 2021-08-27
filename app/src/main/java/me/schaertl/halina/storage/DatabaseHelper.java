package me.schaertl.halina.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import me.schaertl.halina.R;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = DatabaseHelper.class.getName();
    private static final String DB_NAME = "wiki.sqlite3";

    private final String dbPath;
    private final Context context;

    private boolean needToImport = false;

    @SuppressLint("ResourceType")
    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, context.getResources().getInteger(R.string.database_version));

        this.context = context;
        this.dbPath = context.getDatabasePath(DB_NAME).getAbsolutePath();
    }

    /**
     * Ensure that the database was successfully imported from assets
     * into data directory for reading.
     */
    public void ensureImported() throws IOException {
        getWritableDatabase();

        if (needToImport) {
            Log.i(TAG, "importing wiki.sqlite3 from assets to data directory");

            close();

            try (InputStream myInput = context.getAssets().open(DB_NAME)) {
                copyFile(myInput, dbPath);
            }

            getWritableDatabase().close();
        } else {
            Log.i(TAG, "reusing existing wiki.sqlite3 in data directory");
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        needToImport = true;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        needToImport = true;
    }

    /**
     * Copy already opened file.
     *
     * @param is Open file ready for reading.
     * @param to Where to copy the input stream to.
     */
    private void copyFile(InputStream is, String to) throws IOException {
        final Path toPath = Paths.get(to);
        Files.copy(is, toPath, StandardCopyOption.REPLACE_EXISTING);
    }
}