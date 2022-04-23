package me.schaertl.halina.support;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

@SuppressLint("SimpleDateFormat")
public class RFC3399 extends DateFormat {
    private final static String FORMAT_STRING = "yyyy-MM-dd'T'HH:mm:ssXXX";
    private final SimpleDateFormat formatter = new SimpleDateFormat(FORMAT_STRING);

    @NonNull
    @Override
    public StringBuffer format(@NonNull Date date, @NonNull StringBuffer stringBuffer, @NonNull FieldPosition fieldPosition) {
        return formatter.format(date, stringBuffer, fieldPosition);
    }

    @Nullable
    @Override
    public Date parse(@NonNull String s, @NonNull ParsePosition parsePosition) {
        return formatter.parse(s, parsePosition);
    }
}
