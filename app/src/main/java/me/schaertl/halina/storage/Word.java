package me.schaertl.halina.storage;

public class Word {
    public final int id;
    public final String word;
    public final int nreferences;

    public Word(int id, String word, int nreferences) {
        this.id = id;
        this.word = word;
        this.nreferences = nreferences;
    }
}
