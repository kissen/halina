package me.schaertl.halina.support;

import android.content.Context;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import me.schaertl.halina.storage.Word;

public class WordListAdapter extends ArrayAdapter<String> {
    private final List<Word> words;

    public WordListAdapter(Context context, int resource, List<Word> words) {
        super(context, resource, toWordList(words));
        this.words = new ArrayList<>(words);
    }

    public Word getUnderlyingWord(int position) {
        return words.get(position);
    }

    private static List<String> toWordList(List<Word> words) {
        return words.stream().map(w -> w.word).collect(Collectors.toList());
    }
}
