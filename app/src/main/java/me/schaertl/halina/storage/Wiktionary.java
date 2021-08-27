package me.schaertl.halina.storage;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Wiktionary {
    /**
     * Given query string, return a list of possible candidates the user
     * may be interested in.
     */
    public static List<DictionaryEntry> lookUpChoicesFor(String query, Context context) {
        // If the query is empty, return an empty list.

        if (query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // Set up the database.

        final SQLiteDatabase db = getDatabaseFor(context);

        try {
            final List<DictionaryEntry> entries = queryForChoicesFor(query, db);
            return entries;
        } finally {
            db.close();
        }
    }

    private static List<DictionaryEntry> queryForChoicesFor(String query, SQLiteDatabase db) {
        // Prepare the query.

        final String select = "words";
        final String[] from = { "id", "word" };
        final String where = "word LIKE ?";
        final String[] parameters = { query + "%" };
        final String limit = "100";
        final Cursor resultCursor = db.query(select, from, where, parameters, null, null, null, limit);

        // If we have no results, return nothing.

        if (resultCursor == null) {
            return Collections.emptyList();
        }

        if (!resultCursor.moveToFirst()) {
            return Collections.emptyList();
        }

        // Get internal ids. Check for validity.

        final int idColumnId = resultCursor.getColumnIndex("id");
        final int wordColumnId = resultCursor.getColumnIndex("word");

        if (idColumnId < 0) {
            Log.w(Wiktionary.class.getName(), "unexpected idColumnId=" + idColumnId);
            return Collections.emptyList();
        }

        if (wordColumnId < 0) {
            Log.w(Wiktionary.class.getName(), "unexpected wordColumnId=" + idColumnId);
        }

        // Iterate over each choice. Collect words as we go along.

        final List<DictionaryEntry> entries = new ArrayList<>();

        do {
            final int id = resultCursor.getInt(idColumnId);
            final String word = resultCursor.getString(wordColumnId);
            final DictionaryEntry entry = new DictionaryEntry(id, word);

            entries.add(entry);
        } while (resultCursor.moveToNext());

        //  Man what a pain that was!

        return entries;
    }

    private static SQLiteDatabase getDatabaseFor(Context context) {
        final DatabaseHelper helper = new DatabaseHelper(context);
        helper.initializeDataBase();
        return helper.getReadableDatabase();
    }
}
