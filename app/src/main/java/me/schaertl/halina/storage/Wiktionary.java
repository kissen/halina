package me.schaertl.halina.storage;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Wiktionary {
    private Wiktionary() {
    }

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

    /**
     * Given query string, return the defintion for given id.
     */
    public static Optional<Definition> lookUpDefinitionFor(int wordId, Context context) {
        try (SQLiteDatabase db = getDatabaseFor(context)) {
            return queryDefinitionFor(wordId, db);
        }
    }

    @SuppressLint("Range")
    private static List<Word> queryChoicesFor(String query, SQLiteDatabase db) {
        final String select = "words";
        final String[] from = { "id", "word" };
        final String where = "word LIKE ?";
        final String[] parameters = { query + "%" };
        final String limit = "100";

        try (Cursor resultCursor = db.query(select, from, where, parameters, null, null, null, limit)) {
            if (resultCursor == null) {
                return Collections.emptyList();
            }

            if (!resultCursor.moveToFirst()) {
                return Collections.emptyList();
            }

            final List<Word> entries = new ArrayList<>();

            do {
                final int id = resultCursor.getInt(resultCursor.getColumnIndex("id"));
                final String word = resultCursor.getString(resultCursor.getColumnIndex("word"));
                final Word entry = new Word(id, word);

                entries.add(entry);
            } while (resultCursor.moveToNext());

            return entries;
        }
    }

    @SuppressLint("Range")
    private static Optional<Definition> queryDefinitionFor(int wordId, SQLiteDatabase db) {
        final String select = "definitions";
        final String[] from = { "word_id", "definition" };
        final String where = "word_id=" + wordId;
        final String limit = "1000";

        try (Cursor resultCursor = db.query(select, from, where, null, null, null, null, limit)) {
            if (resultCursor == null) {
                return Optional.empty();
            }

            if (!resultCursor.moveToFirst()) {
                return Optional.empty();
            }

            final List<String> definitions = new ArrayList<>();

            do {
                final String definition = resultCursor.getString(resultCursor.getColumnIndex("definition"));
                definitions.add(definition);
            } while (resultCursor.moveToNext());

            final Definition boxed = new Definition(wordId, definitions);
            return Optional.of(boxed);
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