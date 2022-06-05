package me.schaertl.halina.support;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.schaertl.halina.storage.structs.Definition;
import me.schaertl.halina.storage.structs.Word;

public class DefinitionFormatter {
    private final static String REGEX = "\\[\\[(.*?)\\]\\]";
    private final static String SUBSTITUTION = "<a href=\"halina://$1\">$1</a>";
    private final static Pattern PATTERN = Pattern.compile(REGEX, Pattern.MULTILINE);

    private DefinitionFormatter() {}

    public static String format( Definition box) {
        final StringBuilder buf = new StringBuilder();

        for (final String definition : uniqueElementsIn(box.definitions)) {
            buf.append("<p>");
            buf.append(htmlifyLinks(definition));
            buf.append("</p>\n");
        }

        return buf.toString();
    }

    public static String formatCopyingFor(Word word) {
        final StringBuilder buf = new StringBuilder();

        final String wordUrl = word.getOriginalUrl();

        appendf(buf, "<p>\n");
        appendf(buf, "  Dictionary entry licensed CC-BY-SA and based on");
        appendf(buf, "  <a href=\"%s\">this original entry on Wiktionary.org</a>.\n", wordUrl);
        appendf(buf, "  Refer to the original entry for detailed copyright and authorship information of the original work.");
        appendf(buf, "</p>\n");

        return buf.toString();
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

    private static void appendf(StringBuilder dst, String format, Object... args) {
        final String formatted = String.format(format, args);
        dst.append(formatted);
    }
}
