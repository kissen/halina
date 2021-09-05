package me.schaertl.halina.storage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.appcompat.app.AppCompatActivity;

public class RemoteDictionaryChecker extends Thread {
    public static final String INTENT_ACTION = "RemoteDictionaryChecker.Broadcast";

    private static final String BUNDLE_VERSION = "meta_version";
    private static final String BUNDLE_NBYTES = "meta_nbytes";
    private static final String BUNDLE_URL = "meta_url";

    private final Context parentContext;

    public static RemoteDictionaryMeta getResultsFor(Intent intent) {
        final String version = intent.getStringExtra(BUNDLE_VERSION);
        final int nbytes = intent.getIntExtra(BUNDLE_NBYTES, 0);
        final String url = intent.getStringExtra(BUNDLE_URL);

        return new RemoteDictionaryMeta(version, nbytes, url);
    }

    public RemoteDictionaryChecker(Context context) {
        super();  // initialize thread
        this.parentContext = context;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        final RemoteDictionaryMeta meta = new RemoteDictionaryMeta("ABC", 42, "DEF");
        putResults(meta);
    }

    private void putResults(RemoteDictionaryMeta meta) {
        final Intent broadcast = new Intent(INTENT_ACTION);

        broadcast.putExtra(BUNDLE_VERSION, meta.version);
        broadcast.putExtra(BUNDLE_NBYTES, meta.nbytes);
        broadcast.putExtra(BUNDLE_URL, meta.url);

        this.parentContext.sendBroadcast(broadcast);
    }
}
