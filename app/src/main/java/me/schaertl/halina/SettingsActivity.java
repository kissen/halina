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
import me.schaertl.halina.remote.RemoteDictionaryServiceBinder;
import me.schaertl.halina.remote.structs.Phase;
import me.schaertl.halina.remote.structs.Progress;
import me.schaertl.halina.remote.structs.RemoteDictionaryHandler;
import me.schaertl.halina.remote.DictionaryInstallService;
import me.schaertl.halina.remote.structs.RemoteDictionaryMeta;
import me.schaertl.halina.support.FileSizeFormatter;

public class SettingsActivity extends AppCompatActivity implements RemoteDictionaryHandler {
    private MetaDownloadService metaDownloadService;
    private boolean metaDownloadServiceIsBound;

    private DictionaryInstallService dictionaryInstallService;
    private boolean remoteDictionaryServiceIsBound;

    private Preference checkForDictionariesPreference;
    private Preference downloadNewDictionaryPreference;

    private String checkForDictionariesSummary;

    //
    // Android Lifetime Callbacks.
    //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
    protected void onStart() {
        super.onStart();

        // Set up meta service.

        final Intent metaService = new Intent(this, MetaDownloadService.class);
        bindService(metaService, metaConnection, Context.BIND_AUTO_CREATE);

        // Set up install service.

        final Intent remoteService = new Intent(this, DictionaryInstallService.class);
        bindService(remoteService, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();

        unbindService(connection);
        remoteDictionaryServiceIsBound = false;

        syncWithMetaDownloadService();
        unbindService(metaConnection);
        metaDownloadServiceIsBound = false;
    }

    @Override
    protected void onResume() {
        super.onResume();

        final IntentFilter metaFilter = new IntentFilter(MetaDownloadService.BROADCAST_FILTER);
        registerReceiver(metaReceiver, metaFilter);

        syncWithMetaDownloadService();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(metaReceiver);

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

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            final RemoteDictionaryServiceBinder binder = (RemoteDictionaryServiceBinder) iBinder;
            dictionaryInstallService = binder.getService();
            remoteDictionaryServiceIsBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            remoteDictionaryServiceIsBound = false;
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
        // final Optional<RemoteDictionaryService> service = RemoteDictionaryService.getInstance();

        // if (service.isPresent()) {
        //     final String hardCodedUrl = "https://halina.schaertl.me/dictionaries/enwiktionary-latest-pages-articles.sqlite3.gz";

        //     Toaster.toastFrom(this, "downloading new dictionary...");
        //     service.get().startNewDownloadFrom(hardCodedUrl);
        // }
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
    // Callbacks triggered by Meta Task.
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

    @Override
    public synchronized void onNewMeta(RemoteDictionaryMeta meta) {
        // TODO: Delete me
    }

    @Override
    public synchronized void onNewMetaFailed(String errorMessage) {
        // TODO: Delete me
    }

    @SuppressLint("DefaultLocale")
    @Override
    public synchronized void onInstallProgress(String url, Phase phase, Progress progress) {
        runOnUiThread(() -> {
            final String message = phase.format(progress);
            this.downloadNewDictionaryPreference.setSummary(message);
        });
    }

    @Override
    public synchronized void onInstallCompleted(String fileLocation) {
        runOnUiThread(() -> {
            this.downloadNewDictionaryPreference.setSummary("Installed!");
        });
    }

    @Override
    public synchronized void onInstallFailed(String errorMessage) {
        runOnUiThread(() -> {
            final String message = "Install Failed: " + errorMessage;
            this.downloadNewDictionaryPreference.setSummary(message);
        });
    }
}
