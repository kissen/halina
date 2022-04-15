package me.schaertl.halina;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import me.schaertl.halina.remote.MetaDownloadService;
import me.schaertl.halina.remote.structs.Progress;
import me.schaertl.halina.remote.DictionaryInstallService;
import me.schaertl.halina.remote.structs.RemoteDictionaryMeta;
import me.schaertl.halina.support.FileSizeFormatter;

public class SettingsActivity extends AppCompatActivity {
    private MetaDownloadService metaDownloadService;
    private boolean metaDownloadServiceIsBound;

    private DictionaryInstallService dictionaryInstallService;
    private boolean dictionaryInstallServiceIsBound;

    private Preference checkForDictionariesPreference;
    private Preference downloadNewDictionaryPreference;

    private String checkForDictionariesSummary;

    //
    // Android Lifetime Callbacks.
    //

    @Override
    protected synchronized void onCreate(Bundle savedInstanceState) {
        // Set up activity.

        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        setTitle("Settings");

        // Configure action bar.

        final ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Load settings fragment.

        final FragmentManager fragmentManger = getSupportFragmentManager();
        fragmentManger.beginTransaction().replace(R.id.settings, new ClickableSettingsFragment(this)).commit();
    }

    @Override
    protected synchronized void onStart() {
        super.onStart();

        // Set up meta service.

        final Intent metaService = new Intent(this, MetaDownloadService.class);
        bindService(metaService, metaConnection, Context.BIND_AUTO_CREATE);

        // Set up install service.

        final Intent remoteService = new Intent(this, DictionaryInstallService.class);
        startForegroundService(remoteService);
        bindService(remoteService, installConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected synchronized void onStop() {
        super.onStop();

        syncWithMetaDownloadService();
        unbindService(metaConnection);
        metaDownloadServiceIsBound = false;

        syncWithInstallService();
        unbindService(installConnection);
        dictionaryInstallServiceIsBound = false;
    }

    @Override
    protected synchronized void onResume() {
        super.onResume();

        final IntentFilter metaFilter = new IntentFilter(MetaDownloadService.BROADCAST_FILTER);
        registerReceiver(metaReceiver, metaFilter);
        syncWithMetaDownloadService();

        final IntentFilter installFilter = new IntentFilter(DictionaryInstallService.BROADCAST_FILTER);
        registerReceiver(installReceiver, installFilter);
        syncWithInstallService();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(metaReceiver);
        unregisterReceiver(installReceiver);

        super.onDestroy();
    }

    private final BroadcastReceiver metaReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            syncWithMetaDownloadService();
        }
    };

    private final ServiceConnection metaConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            final MetaDownloadService.MetaDownloadBinder binder = (MetaDownloadService.MetaDownloadBinder) iBinder;
            metaDownloadService = binder.getService();
            metaDownloadServiceIsBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            metaDownloadServiceIsBound = false;
        }
    };

    private final BroadcastReceiver installReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            syncWithInstallService();
        }
    };

    private final ServiceConnection installConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            final DictionaryInstallService.DictionaryInstallBinder binder = (DictionaryInstallService.DictionaryInstallBinder) iBinder;
            dictionaryInstallService = binder.getService();
            dictionaryInstallServiceIsBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            dictionaryInstallServiceIsBound = false;
        }
    };

    //
    // Button Press Events.
    //

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //
    // Click Events.
    //

    private synchronized void onCheckForNewDictionaryClicked() {
        if (metaDownloadServiceIsBound) {
            metaDownloadService.startMetaDownload();
        }
    }

    private synchronized void onDownloadNewDictionaryClicked() {
        if (!metaDownloadServiceIsBound || !dictionaryInstallServiceIsBound) {
            return;
        }

        final MetaDownloadService.Report report = metaDownloadService.getReport();

        if (report.state != MetaDownloadService.State.DOWNLOADED) {
            return;
        }

        final Context context = getApplicationContext();
        dictionaryInstallService.installNewDictionary(context, report.meta.url);
    }

    public static class ClickableSettingsFragment extends PreferenceFragmentCompat  {
        private final SettingsActivity parent;

        public ClickableSettingsFragment(SettingsActivity parent) {
            this.parent = parent;
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            // Load preferences from XML.
            setPreferencesFromResource(R.xml.preference_screen, rootKey);

            // [Check for new dictionary]
            parent.checkForDictionariesPreference = findPreference("preference_download_meta");
            parent.checkForDictionariesSummary = parent.checkForDictionariesPreference.getSummary().toString();
            parent.checkForDictionariesPreference.setOnPreferenceClickListener(preference -> {
                parent.runOnUiThread(parent::onCheckForNewDictionaryClicked);
                return false;
            });

            // [Download new dictionary]
            parent.downloadNewDictionaryPreference = findPreference("preference_download_new_dictionary");
            parent.downloadNewDictionaryPreference.setOnPreferenceClickListener(perference -> {
                parent.runOnUiThread(parent::onDownloadNewDictionaryClicked);
                return false;
            });
        }
    }

    //
    // Callbacks triggered by Meta Service.
    //

    private synchronized void syncWithMetaDownloadService() {
        if (!metaDownloadServiceIsBound) {
            return;
        }

        final MetaDownloadService.Report report = metaDownloadService.getReport();

        switch (report.state) {
            case READY:
                onMetaReady();
                break;

            case DOWNLOADING:
                onMetaDownloading();
                break;

            case DOWNLOADED:
                onMetaDownloaded(report.meta);
                break;

            case ERROR:
                onMetaFailed(report.error);
                break;
        }
    }

    private synchronized void onMetaReady() {
        runOnUiThread(() -> {
            checkForDictionariesPreference.setSummary(checkForDictionariesSummary);

            downloadNewDictionaryPreference.setVisible(false);
            downloadNewDictionaryPreference.setSummary("");
        });
    }

    private synchronized void onMetaDownloading() {
        runOnUiThread(() -> {
            checkForDictionariesPreference.setSummary("Downloading...");

            downloadNewDictionaryPreference.setVisible(false);
            downloadNewDictionaryPreference.setSummary("");
        });
    }

    private synchronized void onMetaDownloaded(RemoteDictionaryMeta meta) {
        final String fileSize = FileSizeFormatter.format(meta.nbytes);
        final String summary = String.format("%s (%s)", meta.version, fileSize);

        runOnUiThread(() -> {
            checkForDictionariesPreference.setSummary(checkForDictionariesSummary);

            downloadNewDictionaryPreference.setSummary(summary);
            downloadNewDictionaryPreference.setVisible(true);
        });
    }

    private synchronized void onMetaFailed(Exception error) {
        final String name = error.getClass().getSimpleName();
        final String summary = String.format("%s: %s", name, error.getMessage());

        runOnUiThread(() -> {
            checkForDictionariesPreference.setSummary(summary);

            downloadNewDictionaryPreference.setVisible(false);
            downloadNewDictionaryPreference.setSummary("");
        });
    }

    //
    // Callbacks triggered by Install Service.
    //

    private synchronized void syncWithInstallService() {
        if (!dictionaryInstallServiceIsBound) {
            return;
        }

        final DictionaryInstallService.Report report = dictionaryInstallService.getReport();

        switch (report.state) {
            case DOWNLOADING:
                onInstallDownloading(report.progress);
                break;

            case EXTRACTING:
                onInstallExtracting(report.progress);
                break;

            case INSTALLING:
                onInstallInstalling();
                break;

            case INSTALLED:
                onInstallInstalled();
                break;

            case ERROR:
                onInstallError(report.error);
                break;
        }
    }

    @SuppressLint("DefaultLocale")
    private synchronized void onInstallDownloading(Progress progress) {
        final int percent = Math.round(progress.percent());
        final String summary = String.format("Downloading... (%d%%)", percent);

        runOnUiThread(() -> {
            downloadNewDictionaryPreference.setSummary(summary);
            downloadNewDictionaryPreference.setVisible(true);
        });
    }

    @SuppressLint("DefaultLocale")
    private synchronized void onInstallExtracting(Progress progress) {
        final int percent = Math.round(progress.percent());
        final String summary = String.format("Extracting... (%d%%)", percent);

        runOnUiThread(() -> {
            downloadNewDictionaryPreference.setSummary(summary);
            downloadNewDictionaryPreference.setVisible(true);
        });
    }

    private synchronized void onInstallInstalling() {
        runOnUiThread(() -> {
            downloadNewDictionaryPreference.setSummary("Installing...");
            downloadNewDictionaryPreference.setVisible(true);
        });
    }

    private synchronized void onInstallInstalled() {
        runOnUiThread(() -> {
            downloadNewDictionaryPreference.setVisible(false);
            downloadNewDictionaryPreference.setSummary("Done!");
        });
    }

    private synchronized void onInstallError(Exception error) {
        final String name = error.getClass().getSimpleName();
        final String summary = String.format("%s: %s", name, error.getMessage());

        runOnUiThread(() -> {
            downloadNewDictionaryPreference.setSummary(summary);
            downloadNewDictionaryPreference.setVisible(true);
        });
    }
}
