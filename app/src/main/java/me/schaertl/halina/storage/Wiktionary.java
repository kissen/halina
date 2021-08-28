package me.schaertl.halina.storage;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Wiktionary {
    /**
     * Given query string, return a list of possible candidates the user
     * may be interested in.
     */
    public static List<Word> lookUpChoicesFor(String query, Context context) {
        if (query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try (SQLiteDatabase db = getDatabaseFor(context)) {
            return queryChoicesFor(query, db);
        }
    }

    @SuppressLint("Range")
    private static List<Word> queryChoicesFor(String query, SQLiteDatabase db) {
        // Prepare and execute the query.

        final String select = "words";
        final String[] from = { "id", "word" };
        final String where = "word LIKE ?";
        final String[] parameters = { query + "%" };
        final String limit = "100";

        try (Cursor resultCursor = db.query(select, from, where, parameters, null, null, null, limit)) {
            // If we have no results, return nothing.

            if (resultCursor == null) {
                return Collections.emptyList();
            }

            if (!resultCursor.moveToFirst()) {
                return Collections.emptyList();
            }

            // Iterate over each choice. Collect words as we go along.

            final List<Word> entries = new ArrayList<>();

            do {
                final int id = resultCursor.getInt(resultCursor.getColumnIndex("id"));
                final String word = resultCursor.getString(resultCursor.getColumnIndex("word"));
                final Word entry = new Word(id, word);

                entries.add(entry);
            } while (resultCursor.moveToNext());

            //  Man what a pain that was!

            return entries;
        }
    }

    private static SQLiteDatabase getDatabaseFor(Context context) {
        try {
            final DatabaseHelper helper = new DatabaseHelper(context);
            helper.ensureImported();
            return helper.getReadableDatabase();
        } catch (IOException e) {
            throw new Error("accessing underlying dictionary storage failed", e);
        }
    }
}