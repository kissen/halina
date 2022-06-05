package me.schaertl.halina.storage;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import me.schaertl.halina.storage.exceptions.DatabaseException;
import me.schaertl.halina.storage.structs.Definition;
import me.schaertl.halina.storage.structs.Word;
import me.schaertl.halina.support.RFC3399;

public class Wiktionary {
    private Wiktionary() {}

    /**
     * Given query string, return a list of possible candidates the user
     * may be interested in.
     */
    public static List<Word> lookUpChoicesFor(String query, Context context) throws DatabaseException {
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
     * Given word, return Word object.
     */
    public static Word queryWordFor(String word, Context context) throws DatabaseException {
        // TODO: Write an efficient query to get just the word.

        final List<Word> ws = lookUpChoicesFor(word, context);
        return ws.stream().filter(w -> w.word.equals(word)).findFirst().orElseThrow(() -> new DatabaseException("no such word"));
    }

    /**
     * Given query string, return the definition for given id.
     */
    public static Definition lookUpDefinitionFor(int wordId, Context context) throws DatabaseException {
        try (SQLiteDatabase db = getDatabaseFor(context)) {
            return queryDefinitionFor(wordId, db);
        }
    }

    /**
     * Given query string, return the definition for given word.
     */
    public static Definition lookUpDefinitionFor(String word, Context context) throws DatabaseException {
        try (SQLiteDatabase db = getDatabaseFor(context)) {
            return queryDefinitionFor(word, db);
        }
    }

    /**
     * Return when the database was created (on the backend.)
     * @return The date or an empty optional if no dictionary is installed.
     */
    public static Optional<Date> getCreatedOn(Context context) {
        try {
            final Optional<String> createdOn = getMeta("CreatedOn", context);
            return createdOn.isPresent() ? parseDateString(createdOn.get()) : Optional.empty();
        } catch (DatabaseException e) {
            return Optional.empty();
        }
    }

    /**
     * Return copying information embedded in database.
     * @return The copying text as-is or an empty optional if no dictionary is installed.
     */
    public static Optional<String> getCopying(Context context) {
        try {
            return getMeta("Copying", context);
        } catch (DatabaseException e) {
            return Optional.empty();
        }
    }

    /**
     * Return meta information from dictionary database.
     */
    private static Optional<String> getMeta(String key, Context context) throws DatabaseException {
        try (SQLiteDatabase db = getDatabaseFor(context)) {
            return queryMetaWith(key, db);
        }
    }

    /**
     * Parse date/time string as used in the meta table.
     */
    @SuppressLint("SimpleDateFormat")
    public static Optional<Date> parseDateString(String s) {
        try {
            final Date date = new RFC3399().parse(s);
            return Optional.of(date);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @SuppressLint("Range")
    private static List<Word> queryChoicesFor(String query, SQLiteDatabase db) {
        final String from = "words";
        final String[] select = { "id", "word" ,"revision", "nreferences" };
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
                final long revision = resultCursor.getLong(resultCursor.getColumnIndex("revision"));
                final int nreferences = resultCursor.getInt(resultCursor.getColumnIndex("nreferences"));

                final Word entry = new Word(id, word, revision, nreferences);
                entries.add(entry);
            } while (resultCursor.moveToNext());

            return entries;
        }
    }

    @SuppressLint("Range")
    private static Definition queryDefinitionFor(int wordId, SQLiteDatabase db) throws DatabaseException {
        final String from = "definitions";
        final String[] select = { "word_id", "definition" };
        final String where = "word_id=" + wordId;
        final String limit = "1000";

        try (Cursor resultCursor = db.query(from, select, where, null, null, null, null, limit)) {
            if (resultCursor == null) {
                throw new DatabaseException("no results");
            }

            if (!resultCursor.moveToFirst()) {
                throw new DatabaseException("empty result");
            }

            final List<String> definitions = new ArrayList<>();

            do {
                final String definition = resultCursor.getString(resultCursor.getColumnIndex("definition"));
                definitions.add(definition);
            } while (resultCursor.moveToNext());

            return new Definition(wordId, definitions);
        }
    }

    @SuppressLint("Range")
    private static Definition queryDefinitionFor(String word, SQLiteDatabase db) throws DatabaseException {
        final Optional<Integer> wordId = queryWordIdFor(word, db);
        if (!wordId.isPresent()) {
            throw new DatabaseException("no word_id for given word");
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

    @SuppressLint("Range")
    private static Optional<String> queryMetaWith(String key, SQLiteDatabase db) {
        final String from = "meta";
        final String[] select = { "value" };
        final String where = "key = ?";
        final String[] parameters = { key };
        final String limit = "1";

        try (Cursor resultCursor = db.query(from, select, where, parameters, null, null, limit)) {
            if (resultCursor == null) {
                return Optional.empty();
            }

            if (!resultCursor.moveToFirst()) {
                return Optional.empty();
            }

            final String value = resultCursor.getString(resultCursor.getColumnIndex("value"));
            return Optional.of(value);
        }
    }

    private static synchronized SQLiteDatabase getDatabaseFor(Context context) throws DatabaseException {
        final DatabaseOpenHelper helper = new DatabaseOpenHelper(context);
        return helper.getReadableDatabase();
    }
}