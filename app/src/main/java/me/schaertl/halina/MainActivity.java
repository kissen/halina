package me.schaertl.halina;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import java.util.List;

import me.schaertl.halina.storage.Storage;
import me.schaertl.halina.storage.exceptions.DatabaseException;
import me.schaertl.halina.storage.structs.Word;
import me.schaertl.halina.storage.Wiktionary;
import me.schaertl.halina.support.Caller;
import me.schaertl.halina.support.Res;
import me.schaertl.halina.support.Toaster;
import me.schaertl.halina.support.WordListAdapter;

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

        // Set up event handler for list items.

        final ListView candidatesList = findViewById(R.id.list_main);

        candidatesList.setOnItemClickListener((av, v, pos, l) -> {
            final Object adapter = av.getAdapter();
            if (adapter instanceof WordListAdapter) {
                final WordListAdapter wordListAdapter = (WordListAdapter) adapter;
                final Word word = wordListAdapter.getUnderlyingWord(pos);
                Caller.callViewActivityFrom(MainActivity.this, word.word, word.id);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Configure warning about installing Halina dictionary. Really what should be
        // happening is that a dictionary is automatically installed. For now I haven't
        // implemented that however.

        final TextView warningText = findViewById(R.id.text_warning);

        if (Storage.haveDatabase(getApplicationContext())) {
            warningText.setVisibility(View.GONE);
            return;
        }

        final String warningHtml = Res.loadAsString(this, R.raw.welcome_message);
        warningText.setText(Html.fromHtml(warningHtml, 0));
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
        final MenuItem settingsItem = menu.findItem(R.id.app_bar_settings);

        // Some UI shenanigans I can't seem to achieve in XML.
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setIconified(false);
        searchView.setQueryHint("Search");

        // Set up event handler for list elements.
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

        // Set up event handler for basic menu items.
        settingsItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                Caller.callSettingsActivityFrom(MainActivity.this);
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

        /**
         * Construct new finder.
         *
         * @param query The string entered by the user into the search bar.
         * @param context Application context.
         */
        public ResultFinder(String query, Context context) {
            this.parentContext = context;
            this.query = query;
            this.startedAt = System.currentTimeMillis();
        }

        @Override
        public void run() {
            try {
                final List<Word> entries = Wiktionary.lookUpChoicesFor(this.query, this.parentContext);
                updateListWith(entries, this.startedAt);
            } catch (DatabaseException e) {
                final String msg = "Missing dictionary. Install one in Settings.";
                Toaster.toastFrom(MainActivity.this, msg);
            }
        }
    }
}
