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

import me.schaertl.halina.storage.structs.Definition;
import me.schaertl.halina.storage.Wiktionary;
import me.schaertl.halina.storage.structs.Word;
import me.schaertl.halina.support.Caller;
import me.schaertl.halina.support.DefinitionFormatter;
import me.schaertl.halina.support.Task;

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
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setHtmlContentOnContentView(String html) {
        final TextView contentView = findViewById(R.id.text_content);
        setHtmlContentOn(contentView, html);
    }

    private void setHtmlOnCopyingView(String html) {
        final TextView copyingView = findViewById(R.id.text_copying);
        setHtmlContentOn(copyingView, html);
    }

    private void setHtmlContentOn(TextView textView, String html) {
        // Created with much help from https://stackoverflow.com/a/19989677

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
                    final String url = span.getURL();

                    if (url == null) {
                        return;
                    }

                    if (url.startsWith("halina://")) {
                        final String query = url.replace("halina://", "");
                        Caller.callViewActivityFrom(ViewEntryActivity.this, query);
                    }

                    if (url.startsWith("https://")) {
                        Caller.callBrowserFrom(ViewEntryActivity.this, url);
                    }
                }
            };

            builder.setSpan(clickableSpan, start, end, flags);
            builder.removeSpan(span);
        }

        runOnUiThread(() -> {
            textView.setText(builder);
            textView.setMovementMethod(LinkMovementMethod.getInstance());
        });
    }

    private class DefinitionFinder extends Task {
        private final Context context;

        public DefinitionFinder(Context context) {
            this.context = context;
        }

        @Override
        public void execute() throws Exception {
            final Optional<Word> meta;
            final Optional<Definition> definitions;

            if (!(meta = Wiktionary.queryWordFor(word, context)).isPresent()) {
                return;
            }

            if (!(definitions = lookUpDefinitions()).isPresent()) {
                return;
            }

            final Definition definition = definitions.get();

            final String definitionHtml = DefinitionFormatter.format(definition);
            final String copyingHtml = DefinitionFormatter.formatCopyingFor(meta.get());

            setHtmlContentOnContentView(definitionHtml);
            setHtmlOnCopyingView(copyingHtml);
        }

        private Optional<Definition> lookUpDefinitions() throws Exception {
            if (wordId <= -1) {
                return Wiktionary.lookUpDefinitionFor(word, context);
            } else {
                return Wiktionary.lookUpDefinitionFor(wordId, context);
            }
        }
    }
}