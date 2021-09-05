package me.schaertl.halina.storage;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class Toaster {
    private Toaster() {}

    @SuppressLint("ShowToast")
    public static void toastFrom(AppCompatActivity activity, String message) {
        final Context context = activity.getApplicationContext();
        final Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        activity.runOnUiThread(toast::show);
    }
}
