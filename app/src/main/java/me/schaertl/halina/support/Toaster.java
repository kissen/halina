package me.schaertl.halina.support;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Provide an easy API for creating Android toast messages.
 */
public class Toaster {
    private final static String TAG = Toaster.class.getName();

    private Toaster() {}

    /**
     * Show a single toast message.
     *
     * @param activity Caller.
     * @param message Message of the toast message.
     */
    @SuppressLint("ShowToast")
    public static void toastFrom(AppCompatActivity activity, String message) {
        try {
            final Context context = activity.getApplicationContext();
            final Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
            activity.runOnUiThread(toast::show);
        } catch (RuntimeException e) {
            final String logMessage = String.format("could not show toast message=\"%s\": %s", message, e.getMessage());
            Log.e(TAG, logMessage);
        }
    }
}