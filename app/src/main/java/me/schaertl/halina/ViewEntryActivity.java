package me.schaertl.halina;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

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
        final Thread worker = new DefinitionFinder();
        worker.start();
    }

    private class DefinitionFinder extends Thread {
        @Override
        public void run() {
        }
    }
}