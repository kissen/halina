package me.schaertl.halina.storage.structs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Definition {
    public final int wordId;
    public final List<String> definitions;

    public Definition(int wordId, List<String> definitions) {
        this.wordId = wordId;

        final List<String> copy = new ArrayList<>(definitions);
        this.definitions = Collections.unmodifiableList(copy);
    }
}
