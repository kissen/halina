package me.schaertl.halina;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import java.util.List;
import java.util.stream.Collectors;

import me.schaertl.halina.storage.DictionaryEntry;
import me.schaertl.halina.storage.Wiktionary;

public class MainActivity extends AppCompatActivity {
    /***
     * Time in ms when the list was most recently updated.
     */
    private volatile long lastResultSetAt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set up Activity.
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up event handlers.
        final EditText searchField = findViewById(R.id.text_main_input);
        searchField.addTextChangedListener(new TextFieldUpdater(getApplicationContext()));
    }

    /**
     * Update the main list to contain new elements.
     *
     * @param choices Entries to show. First elements are listed first.
     * @param startedAt Time in milliseconds since epoch when the calling job was started.
     */
    private synchronized void updateListWith(List<String> choices, long startedAt) {
        // If the result is older than the most recent update, discard it.
        if (startedAt <= this.lastResultSetAt) {
            return;
        }

        // If not, update the list accordingly.
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.list_entry, choices);
        final ListView targetList = findViewById(R.id.list_main);
        runOnUiThread(() -> targetList.setAdapter(adapter));

        // Update complete. Remember the latest state.
        this.lastResultSetAt = startedAt;
    }

    /**
     * Helper class that handles updates to the current search. Every time the user
     * enters a new character into the search bar, this TextFieldUpdater is called
     * to action. It looks up possible results in the underlying word database.
     */
    private class TextFieldUpdater extends AbstractTextFieldUpdater {
        private final Context parentContext;

        public TextFieldUpdater(Context context) {
            this.parentContext = context;
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            final String query = s.toString();
            final Thread worker = new ResultFinder(query, this.parentContext);
            worker.start();
        }
    }

    /**
     * This is the worker thread that goes out to the word database and queries
     * it for possible candidates.
     */
    private class ResultFinder extends Thread {
        private final Context parentContext;

        /**
         * The query as entered by the user.
         */
        private final String query;

        /**
         * Millisecond time when this worker was created.
         */
        private final long startedAt;

        public ResultFinder(String query, Context context) {
            this.parentContext = context;
            this.query = query;
            this.startedAt = System.currentTimeMillis();
        }

        @Override
        public void run() {
            final List<DictionaryEntry> entries = Wiktionary.lookUpChoicesFor(this.query, this.parentContext);
            final List<String> choices = entries.stream().map(de -> de.word).collect(Collectors.toList());

            updateListWith(choices, this.startedAt);
        }
    }
}
