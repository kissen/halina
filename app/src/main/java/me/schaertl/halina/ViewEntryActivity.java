package me.schaertl.halina;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.text.Spanned;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Optional;

import me.schaertl.halina.storage.Definition;
import me.schaertl.halina.storage.Wiktionary;
import me.schaertl.halina.support.DefinitionFormatter;

public class ViewEntryActivity extends AppCompatActivity {
    private int wordId;
    private String word;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set up Activity.
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_entry);

        // Configure the action bar.
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Load arguments passed from MainActivity.
        final Bundle arguments = getIntent().getExtras();
        if (arguments != null) {
            this.wordId = arguments.getInt("word_id");
            this.word = arguments.getString("word");
        }

        // Set up title.
        setTitle(word);

        // Start looking for the contents.
        final Thread worker = new DefinitionFinder(getApplicationContext());
        worker.start();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("ShowToast")
    private void showToast(String message) {
        runOnUiThread(() -> {
            final Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG);
            toast.show();
        });
    }

    private void setContent(Spanned content) {
        final TextView titleView = findViewById(R.id.text_content);
        runOnUiThread(() -> titleView.setText(content));
    }

    private class DefinitionFinder extends Thread {
        private final Context context;

        public DefinitionFinder(Context context) {
            this.context = context;
        }

        @Override
        public void run() {
            final Optional<Definition> boxed = Wiktionary.lookUpDefinitionFor(wordId, context);

            if (!boxed.isPresent()) {
                showToast("could not find definition");
                return;
            }

            final Definition definition = boxed.get();
            final Spanned markup = DefinitionFormatter.format(word, definition);
            setContent(markup);
        }
    }
}