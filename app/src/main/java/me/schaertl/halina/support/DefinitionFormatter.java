package me.schaertl.halina.support;

import android.text.Html;
import android.text.Spanned;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.schaertl.halina.storage.Definition;

public class DefinitionFormatter {
    private DefinitionFormatter() {
    }

    public static Spanned format(String word, Definition boxed) {
        final StringBuilder buf = new StringBuilder();

        buf.append("<ul>\n");

        for (final String definition : uniqueElementsIn(boxed.definitions)) {
            // For now we drop the [[links]] because implementing them is going to be
            // a topic of its own.
            final String withoutLinks = definition.replace("[", "").replace("]", "");

            buf.append("    <p>");
            buf.append(withoutLinks);
            buf.append("</p>\n");
        }

        buf.append("</ul>\n");

        return Html.fromHtml(buf.toString(), 0);
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
