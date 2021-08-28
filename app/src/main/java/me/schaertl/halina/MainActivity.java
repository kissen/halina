package me.schaertl.halina;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import java.util.List;
import java.util.stream.Collectors;

import me.schaertl.halina.storage.DictionaryEntry;
import me.schaertl.halina.storage.Wiktionary;
import me.schaertl.halina.support.AbstractTextFieldUpdater;

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

        // Set up event handler for search field..
        final EditText searchField = findViewById(R.id.text_main_input);
        searchField.addTextChangedListener(new TextFieldUpdater(getApplicationContext()));

        // Set up event handler for test button.
        final Button button = findViewById(R.id.button_test);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent next = new Intent(MainActivity.this, ViewEntryActivity.class);
                startActivity(next);
            }
        });
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

    private void callViewActivityFor(int wordId, String word) {
        final Bundle arguments = new Bundle();
        arguments.putInt("word_id", wordId);
        arguments.putString("word", word);

        final Intent next = new Intent(this, ViewEntryActivity.class);
        next.putExtras(arguments);

        startActivity(next);
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
