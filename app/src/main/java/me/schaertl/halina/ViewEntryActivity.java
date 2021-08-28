package me.schaertl.halina;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Optional;

import me.schaertl.halina.storage.Definition;
import me.schaertl.halina.storage.Wiktionary;

public class ViewEntryActivity extends AppCompatActivity {
    private int wordId;
    private String word;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set up Activity.
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_entry);

        // Load arguments passed from MainActivity.
        final Bundle arguments = getIntent().getExtras();
        if (arguments != null) {
            this.wordId = arguments.getInt("word_id");
            this.word = arguments.getString("word");
        }

        // Set up title.
        final TextView titleView = findViewById(R.id.text_title);
        titleView.setText(this.word);

        // Start looking for the contents.
        final Thread worker = new DefinitionFinder(getApplicationContext());
        worker.start();
    }

    @SuppressLint("ShowToast")
    private void showToast(String message) {
        runOnUiThread(() -> {
            final Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG);
            toast.show();
        });
    }

    private void setContent(String content) {
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
            setContent(definition.definitions.get(0));
        }
    }
}