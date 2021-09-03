package me.schaertl.halina.storage;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Path;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Wiktionary {
    private Wiktionary() {}

    /**
     * Given query string, return a list of possible candidates the user
     * may be interested in.
     */
    public static List<Word> lookUpChoicesFor(String query, Context context) {
        if (query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try (SQLiteDatabase db = getDatabaseFor(context)) {
            final List<Word> candidates = queryChoicesFor(query, db);
            candidates.sort(new WordComparator(query));
            return candidates;
        }
    }

    /**
     * Given query string, return the definition for given id.
     */
    public static Optional<Definition> lookUpDefinitionFor(int wordId, Context context) {
        try (SQLiteDatabase db = getDatabaseFor(context)) {
            return queryDefinitionFor(wordId, db);
        }
    }

    /**
     * Given query string, return the definition for given word.
     */
    public static Optional<Definition> lookUpDefinitionFor(String word, Context contenxt) {
        try (SQLiteDatabase db = getDatabaseFor(contenxt)) {
            return queryDefinitionFor(word, db);
        }
    }

    @SuppressLint("Range")
    private static List<Word> queryChoicesFor(String query, SQLiteDatabase db) {
        final String from = "words";
        final String[] select = { "id", "word", "nreferences" };
        final String where = "word LIKE ?";
        final String[] parameters = { query + "%" };
        final String limit = "100";

        try (Cursor resultCursor = db.query(from, select, where, parameters, null, null, null, limit)) {
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
                final int nreferences = resultCursor.getInt(resultCursor.getColumnIndex("nreferences"));

                final Word entry = new Word(id, word, nreferences);
                entries.add(entry);
            } while (resultCursor.moveToNext());

            return entries;
        }
    }

    @SuppressLint("Range")
    private static Optional<Definition> queryDefinitionFor(int wordId, SQLiteDatabase db) {
        final String from = "definitions";
        final String[] select = { "word_id", "definition" };
        final String where = "word_id=" + wordId;
        final String limit = "1000";

        try (Cursor resultCursor = db.query(from, select, where, null, null, null, null, limit)) {
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

    @SuppressLint("Range")
    private static Optional<Definition> queryDefinitionFor(String word, SQLiteDatabase db) {
        final Optional<Integer> wordId = queryWordIdFor(word, db);
        if (!wordId.isPresent()) {
            return Optional.empty();
        }

        return queryDefinitionFor(wordId.get(), db);
    }

    @SuppressLint("Range")
    private static Optional<Integer> queryWordIdFor(String word, SQLiteDatabase db) {
        final String from = "words";
        final String[] select = { "id" };
        final String where = "word = ?";
        final String[] parameters = { word };
        final String limit = "1";

        try (Cursor resultCursor = db.query(from, select, where, parameters, null, null, limit)) {
            if (resultCursor == null) {
                return Optional.empty();
            }

            if (!resultCursor.moveToFirst()) {
                return Optional.empty();
            }

            final int wordId = resultCursor.getInt(resultCursor.getColumnIndex("id"));
            return Optional.of(wordId);
        }
    }

    private static synchronized SQLiteDatabase getDatabaseFor(Context context) {
        try {
            final DatabaseHelper helper = new DatabaseHelper(context);
            helper.ensureImported();
            return helper.getReadableDatabase();
        } catch (IOException e) {
            throw new Error("accessing underlying dictionary storage failed", e);
        }
    }
}