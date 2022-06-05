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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.schaertl.halina.storage.structs.Definition;
import me.schaertl.halina.storage.Wiktionary;
import me.schaertl.halina.storage.structs.Word;
import me.schaertl.halina.support.Caller;
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

    /**
     * Task that runs in the background and renders the HTML for display.
     */
    private class DefinitionFinder extends Task {
        private final static String REGEX = "\\[\\[(.*?)\\]\\]";
        private final static String SUBSTITUTION = "<a href=\"halina://$1\">$1</a>";
        private final Pattern PATTERN = Pattern.compile(REGEX, Pattern.MULTILINE);

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

            final String definitionHtml = formatDefinitions(definition);
            final String copyingHtml = formatCopying(meta.get());

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

        private String formatDefinitions(Definition definitions) {
            final StringBuilder buf = new StringBuilder();

            for (final String definition : uniqueElementsIn(definitions.definitions)) {
                buf.append("<p>");
                buf.append(htmlifyLinks(definition));
                buf.append("</p>\n");
            }

            return buf.toString();
        }

        private String htmlifyLinks(String definition) {
            final Matcher matcher = PATTERN.matcher(definition);
            return matcher.replaceAll(SUBSTITUTION);
        }

        private List<String> uniqueElementsIn(List<String> list) {
            // NOTE: We want to maintain the order in list!

            final List<String> uniqueElements = new ArrayList<>(list.size());
            final Set<String> seen = new HashSet<>();

            for (final String element : list) {
                if (!seen.contains(element)) {
                    uniqueElements.add(element);
                    seen.add(element);
                }
            }

            return uniqueElements;
        }

        private String formatCopying(Word word) {
            final StringBuilder buf = new StringBuilder();

            buf.append("<p>\n");
            buf.append("Dictionary entry licensed CC-BY-SA and based on");
            buf.append("<a href=\"");
            buf.append(word.getOriginalUrl());
            buf.append("\">this original entry on Wiktionary.org</a>.\n");
            buf.append("Refer to the original entry for detailed copyright and authorship information of the original work.");
            buf.append("</p>\n");

            return buf.toString();
        }
    }
}