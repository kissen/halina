package me.schaertl.halina.storage;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.apache.commons.lang3.NotImplementedException;

import me.schaertl.halina.storage.exceptions.DatabaseException;
import me.schaertl.halina.storage.exceptions.NoDatabaseException;
import me.schaertl.halina.support.Guard;

public class DatabaseOpenHelper extends SQLiteOpenHelper {
    @SuppressLint("ResourceType")
    public DatabaseOpenHelper(Context context) throws DatabaseException {
        super(context, Storage.DB_NAME, null, 10);

        if (!Storage.haveDatabase(context)) {
            throw new NoDatabaseException("missing database file");
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        try (final Guard guard = Storage.guard()) {
            super.onOpen(db);
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    @Override
    public SQLiteDatabase getReadableDatabase() {
        return super.getReadableDatabase();
    }

    @Override
    public SQLiteDatabase getWritableDatabase() {
        throw new NotImplementedException();
    }
}