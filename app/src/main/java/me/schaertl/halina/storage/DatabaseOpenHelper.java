package me.schaertl.halina.storage;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.apache.commons.lang3.NotImplementedException;

import java.util.concurrent.locks.Lock;

import me.schaertl.halina.support.Guard;

public class DatabaseOpenHelper extends SQLiteOpenHelper {
    @SuppressLint("ResourceType")
    public DatabaseOpenHelper(Context context) {
        super(context, Storage.DB_NAME, null, 10);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        final Lock lock = Storage.getLock();

        try (final Guard guard = new Guard(lock)) {
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