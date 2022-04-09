package me.schaertl.halina.storage;

import org.apache.commons.text.similarity.LevenshteinDistance;

import java.util.Comparator;

import me.schaertl.halina.storage.structs.Word;

public class WordComparator implements Comparator<Word> {
    private final String query;
    private final LevenshteinDistance levenshteinDistance;

    public WordComparator(String query) {
        this.query = query;
        this.levenshteinDistance = new LevenshteinDistance();
    }

    @Override
    public int compare(Word lhs, Word rhs) {
        final int lhsDistance = levenshteinDistance.apply(this.query, lhs.word);
        final int rhsDistance = levenshteinDistance.apply(this.query, rhs.word);

        if (lhsDistance != rhsDistance) {
            return Integer.compare(lhsDistance, rhsDistance);
        }

        return (-1) * Integer.compare(lhs.nreferences, rhs.nreferences);
    }
}
