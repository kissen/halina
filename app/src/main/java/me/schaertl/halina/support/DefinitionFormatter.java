package me.schaertl.halina.support;

import android.text.Html;
import android.text.Spanned;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.schaertl.halina.storage.Definition;

public class DefinitionFormatter {
    private final static String REGEX = "\\[\\[(.*?)\\]\\]";
    private final static String SUBSTITUTION = "<a href=\"halina://$1\">$1</a>";
    private final static Pattern PATTERN = Pattern.compile(REGEX, Pattern.MULTILINE);

    private DefinitionFormatter() {
    }

    public static Spanned format(String word, Definition boxed) {
        final StringBuilder buf = new StringBuilder();

        buf.append("<ul>\n");

        for (final String definition : uniqueElementsIn(boxed.definitions)) {
            buf.append("    <p>");
            buf.append(htmlifyLinks(definition));
            buf.append("</p>\n");
        }

        buf.append("</ul>\n");

        return Html.fromHtml(buf.toString(), 0);
    }

    private static String htmlifyLinks(String definition) {
        final Matcher matcher = PATTERN.matcher(definition);
        return matcher.replaceAll(SUBSTITUTION);
    }

    private static List<String> uniqueElementsIn(List<String> list) {
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
}
