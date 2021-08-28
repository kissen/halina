package me.schaertl.halina.support;

import android.text.Html;
import android.text.Spanned;

import me.schaertl.halina.storage.Definition;

public class DefinitionFormatter {
    private DefinitionFormatter() {
    }

    public static Spanned format(String word, Definition boxed) {
        final StringBuilder buf = new StringBuilder();

        buf.append("<ul>\n");

        for (final String definition : boxed.definitions) {
            // For now we drop the [[links]] because implementing them is going to be
            // a topic of its own.
            final String withoutLinks = definition.replace("[", "").replace("]", "");

            buf.append("    <li>");
            buf.append(withoutLinks);
            buf.append("</li>\n");
        }

        buf.append("</ul>\n");

        return Html.fromHtml(buf.toString(), 0);
    }
}
