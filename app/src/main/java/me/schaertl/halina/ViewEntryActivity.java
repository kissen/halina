package me.schaertl.halina;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.Optional;

import me.schaertl.halina.storage.exceptions.DatabaseException;
import me.schaertl.halina.storage.structs.Definition;
import me.schaertl.halina.support.Toaster;
import me.schaertl.halina.storage.Wiktionary;
import me.schaertl.halina.support.Caller;
import me.schaertl.halina.support.DefinitionFormatter;

public class ViewEntryActivity extends AppCompatActivity {
    /**
     * Id of the currently displayed word or -1 if not known.
     */
    private int wordId;

    /**
     * The currently displayed word.
     */
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

    private void showToast(String message) {
        Toaster.toastFrom(ViewEntryActivity.this, message);
    }

    private void setHtmlContent(String html) {
        // Created with much help from https://stackoverflow.com/a/19989677

        final TextView contentView = findViewById(R.id.text_content);

        final CharSequence markup = Html.fromHtml(html, 0);
        final SpannableStringBuilder builder = new SpannableStringBuilder(markup);

        final URLSpan[] urls = builder.getSpans(0, markup.length(), URLSpan.class);
        for (final URLSpan span : urls) {
            final int start = builder.getSpanStart(span);
            final int end = builder.getSpanEnd(span);
            final int flags = builder.getSpanFlags(span);

            final ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View view) {
                    final String addr = span.getURL();
                    if (addr != null) {
                        final String query = addr.replace("halina://", "");
                        Caller.callViewActivityFrom(ViewEntryActivity.this, query);
                    }
                }
            };

            builder.setSpan(clickableSpan, start, end, flags);
            builder.removeSpan(span);
        }

        runOnUiThread(() -> {
            contentView.setText(builder);
            contentView.setMovementMethod(LinkMovementMethod.getInstance());
        });
    }

    private class DefinitionFinder extends Thread {
        private final Context context;

        public DefinitionFinder(Context context) {
            this.context = context;
        }

        @Override
        public void run() {
            try {
                runThrowing();
            } catch (DatabaseException e) {
                // TODO
            }
        }

        private void runThrowing() throws DatabaseException {
            final Optional<Definition> boxed;

            // If the wordId is unknown it is set to -1. In that case we have
            // to look up with just the word. It is going to be a bit slower,
            // but it is all we have to work with.

            if (ViewEntryActivity.this.wordId == -1) {
                boxed = Wiktionary.lookUpDefinitionFor(word, context);
            } else {
                boxed = Wiktionary.lookUpDefinitionFor(wordId, context);
            }

            if (!boxed.isPresent()) {
                showToast("could not find definition");
                return;
            }

            final Definition definition = boxed.get();
            final String markup = DefinitionFormatter.format(word, definition);
            setHtmlContent(markup);
        }
    }
}