package me.schaertl.halina.storage.structs;

import android.annotation.SuppressLint;

public class Word {
    /**
     * Unique ID of the given word.
     */
    public final int id;

    /**
     * Actual word.
     */
    public final String word;

    /**
     * Wiktionary revision number.
     */
    public final long revision;

    /**
     * Number of references, that is number of words that reference this word
     * in its definitions.
     */
    public final int nreferences;

    public Word(int id, String word, long revision, int nreferences) {
        this.id = id;
        this.word = word;
        this.revision = revision;
        this.nreferences = nreferences;
    }

    /**
     * @return Original upstream URL of given word.
     */
    @SuppressLint("DefaultLocale")
    public String getOriginalUrl() {
        return String.format("https://en.wiktionary.org/w/index.php?oldid=%d", revision);
    }
}
