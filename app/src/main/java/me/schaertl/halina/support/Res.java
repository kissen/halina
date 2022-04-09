package me.schaertl.halina.support;

import androidx.appcompat.app.AppCompatActivity;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Res {
    private Res() {}

    public static String loadAsString(AppCompatActivity activity, int id) {
        try (final InputStream is = activity.getResources().openRawResource(id)) {
            final Charset utf8 = StandardCharsets.UTF_8;
            return IOUtils.toString(is, utf8);
        } catch (IOException e) {
            return String.format("%s: %s", e.getClass().toString(), e.getMessage());
        }
    }
}
