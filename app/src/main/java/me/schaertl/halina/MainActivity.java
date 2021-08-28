package me.schaertl.halina;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.SearchView;

import java.util.List;

import me.schaertl.halina.storage.Word;
import me.schaertl.halina.storage.Wiktionary;
import me.schaertl.halina.support.WordListAdapter;

public class MainActivity extends AppCompatActivity {
    private static String TAG = MainActivity.class.getName();

    /***
     * Time in ms when the list was most recently updated.
     */
    private volatile long lastResultSetAt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set up Activity.
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up event handler for list items.
        final ListView candidatesList = findViewById(R.id.list_main);
        candidatesList.setOnItemClickListener((av, v, pos, l) -> {
            final Object adapter = av.getAdapter();
            if (adapter instanceof WordListAdapter) {
                final WordListAdapter wordListAdapter = (WordListAdapter) adapter;
                final Word word = wordListAdapter.getUnderlyingWord(pos);
                callViewActivityFor(word.id, word.word);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Save the context for later.
        final Context context = getApplicationContext();

        // Inflate the menu. You know at some point humanity made a mistake when
        // it now has to worry about inflating menus.
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        // Get the menu.
        final MenuItem searchViewItem = menu.findItem(R.id.app_bar_search);
        final SearchView searchView = (SearchView) searchViewItem.getActionView();

        // Some UI shenanigans I can't seem to achieve in XML.
        searchView.setIconified(false);
        searchView.setQueryHint("Search");

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchView.clearFocus();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                final Thread worker = new ResultFinder(query, context);
                worker.start();

                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Update the main list to contain new elements.
     *
     * @param choices Entries to show. First elements are listed first.
     * @param startedAt Time in milliseconds since epoch when the calling job was started.
     */
    private synchronized void updateListWith(List<Word> choices, long startedAt) {
        // If the result is older than the most recent update, discard it.
        if (startedAt <= this.lastResultSetAt) {
            return;
        }

        // If not, update the list accordingly.
        final WordListAdapter adapter = new WordListAdapter(this, R.layout.list_entry, choices);
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
            final List<Word> entries = Wiktionary.lookUpChoicesFor(this.query, this.parentContext);
            updateListWith(entries, this.startedAt);
        }
    }
}
