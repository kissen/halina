package me.schaertl.halina.support;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import me.schaertl.halina.SettingsActivity;
import me.schaertl.halina.TextViewActivity;
import me.schaertl.halina.ViewEntryActivity;

/**
 * Provide helpers for calling other activities.
 */
public class Caller {
    private Caller() {}

    /**
     * Open the view activity that displays a single dictionary entry.
     *
     * @param activity Caller.
     * @param word The word for which definitions should be displayed.
     */
    public static void callViewActivityFrom(AppCompatActivity activity, String word) {
        callViewActivityFrom(activity, word, -1);
    }

    /**
     * Open the view activity that displays a single dictionary entry.
     *
     * @param activity Caller.
     * @param word The word for which definitions should be displayed.
     * @param wordId Primary key of given word.
     */
    public static void callViewActivityFrom(AppCompatActivity activity, String word, int wordId) {
        final Bundle arguments = new Bundle();
        arguments.putInt("word_id", wordId);
        arguments.putString("word", word);

        final Intent next = new Intent(activity, ViewEntryActivity.class);
        next.putExtras(arguments);

        activity.startActivity(next);
    }

    /**
     * Open the settings activity.
     *
     * @param activity Caller.
     */
    public static void callSettingsActivityFrom(AppCompatActivity activity) {
        final Intent next = new Intent(activity, SettingsActivity.class);
        activity.startActivity(next);
    }

    /**
     * Open the text view activity that displays some monospaced text.
     *
     * @param activity Caller.
     * @param title Title to be displayed on the text view's title bar.
     * @param text Text to displayed in the main text view.
     */
    public static void callTextViewActivityFrom(AppCompatActivity activity, String title, String text) {
        final Bundle arguments = new Bundle();
        arguments.putString("title", title);
        arguments.putString("text", text);

        final Intent next = new Intent(activity, TextViewActivity.class);
        next.putExtras(arguments);

        activity.startActivity(next);
    }

    /**
     * Open given url in the default browser.
     *
     * @param activity Caller.
     * @param url The URL to call. Has to start with a proper prefix (e.g. "https://").
     */
    public static void callBrowserFrom(AppCompatActivity activity, String url) {
        final Intent next = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        activity.startActivity(next);
    }
}
