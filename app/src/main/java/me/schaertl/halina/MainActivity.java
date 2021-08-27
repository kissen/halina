package me.schaertl.halina;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import java.util.Arrays;
import java.util.List;

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
        searchField.addTextChangedListener(new TextFieldUpdater());

        // Set up the List.
        updateListWith(Arrays.asList("Yes", "Yes"), System.currentTimeMillis());
    }

    private synchronized void updateListWith(List<String> choices, long startedAt) {
        // If the result is older than the most recent update, discard it.
        if (startedAt <= this.lastResultSetAt) {
            return;
        }

        // If not, update the list accordingly.
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.list_entry, choices);
        final ListView targetList = findViewById(R.id.list_main);
        runOnUiThread(() -> targetList.setAdapter(adapter));
    }

    /**
     * Helper class that handles updates to the current search. Every time the user
     * enters a new character into the search bar, this TextFieldUpdater is called
     * to action. It looks up possible results in the underlying word database.
     */
    private class TextFieldUpdater implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int counter, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            final String query = s.toString();
            final Thread worker = new ResultFinder(query);
            worker.start();
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }

    /**
     * This is the worker thread that goes out to the word database and queries
     * it for possible candidates.
     */
    private class ResultFinder extends Thread {
        /**
         * The query as entered by the user.
         */
        private final String query;

        /**
         * Millisecond time when this worker was created.
         */
        private final long startedAt;

        public ResultFinder(String query) {
            this.query = query;
            this.startedAt = System.currentTimeMillis();
        }

        @Override
        public void run() {
            System.out.println(query);
            updateListWith(Arrays.asList("Yes", "That", "Rocks"), this.startedAt);
        }
    }
}
