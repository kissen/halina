package me.schaertl.halina.support;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import me.schaertl.halina.SettingsActivity;
import me.schaertl.halina.ViewEntryActivity;

public class Caller {
    private Caller() {}

    public static void callViewActivityFrom(AppCompatActivity activity, String word) {
        callViewActivityFrom(activity, word, -1);
    }

    public static void callViewActivityFrom(AppCompatActivity activity, String word, int wordId) {
        final Bundle arguments = new Bundle();
        arguments.putInt("word_id", wordId);
        arguments.putString("word", word);

        final Intent next = new Intent(activity, ViewEntryActivity.class);
        next.putExtras(arguments);

        activity.startActivity(next);
    }

    public static void callSettingsActivityFrom(AppCompatActivity activity) {
        final Intent next = new Intent(activity, SettingsActivity.class);
        activity.startActivity(next);
    }
}
