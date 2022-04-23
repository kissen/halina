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
import android.provider.ContactsContract;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Optional;

import me.schaertl.halina.remote.MetaDownloadService;
import me.schaertl.halina.remote.DictionaryInstallService;
import me.schaertl.halina.remote.structs.RemoteDictionaryMeta;
import me.schaertl.halina.storage.Storage;
import me.schaertl.halina.storage.Wiktionary;
import me.schaertl.halina.storage.exceptions.DatabaseException;
import me.schaertl.halina.support.FileSizeFormatter;

public class SettingsActivity extends AppCompatActivity {
    private static final int BIND_DO_NOT_CREATE_AUTOMATICALLY = 0;

    private MetaDownloadService metaDownloadService;
    private boolean metaDownloadServiceIsBound;

    private DictionaryInstallService dictionaryInstallService;
    private boolean dictionaryInstallServiceIsBound;
    private String dictionaryInstallServiceUrl;

    private Preference currentDictionaryPreference;
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

    @SuppressLint("WrongConstant")
    @Override
    protected synchronized void onStart() {
        super.onStart();

        // Set up meta service.

        final Intent metaService = new Intent(this, MetaDownloadService.class);
        bindService(metaService, metaConnection, Context.BIND_AUTO_CREATE);

        // Set up install service.

        final Intent remoteService = new Intent(this, DictionaryInstallService.class);
        bindService(remoteService, installConnection, BIND_DO_NOT_CREATE_AUTOMATICALLY);
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

        syncWithStorage();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(metaReceiver);
        unregisterReceiver(installReceiver);

        super.onDestroy();
    }

    //
    // Service Management.
    //

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
        private final SettingsActivity parent = SettingsActivity.this;

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            synchronized (parent) {
                final DictionaryInstallService.DictionaryInstallBinder binder = (DictionaryInstallService.DictionaryInstallBinder) iBinder;
                dictionaryInstallService = binder.getService();
                dictionaryInstallServiceIsBound = true;

                if (dictionaryInstallServiceUrl != null) {
                    final Context context = parent.getApplicationContext();
                    final String url = dictionaryInstallServiceUrl;

                    dictionaryInstallService.installNewDictionary(context, url);
                    dictionaryInstallServiceUrl = null;
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            synchronized (parent) {
                dictionaryInstallServiceIsBound = false;
            }
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

    @SuppressLint("WrongConstant")
    private synchronized void onDownloadNewDictionaryClicked() {
        // We can only start a new dictionary install if we have the necessary
        // meta data from the meta download service.

        if (!metaDownloadServiceIsBound) {
            return;
        }

        final MetaDownloadService.Report report = metaDownloadService.getReport();

        if (report.state != MetaDownloadService.State.DOWNLOADED) {
            return;
        }

        // We do have the necessary meta information. Try to start the install
        // service which will do all the heavy lifting.
        //
        // TODO: Check for race conditions. Can we miss an event?

        if (dictionaryInstallServiceIsBound) {
            final Context context = getApplicationContext();
            final String url = report.meta.url;

            dictionaryInstallService.installNewDictionary(context, url);
            return;
        }

        // If the URL is non-null that means that we previously started the
        // service (for use w/ this url) but the service has not started *yet*.
        // Here we quit early because we do not want to start multiple downloads.

        if (dictionaryInstallServiceUrl != null) {
            return;
        }

        // We do not have a running service nor do we have a pending start.
        // This means we have to start the service ourselves.

        dictionaryInstallServiceUrl = report.meta.url;

        final Intent remoteService = new Intent(this, DictionaryInstallService.class);
        startForegroundService(remoteService);
        bindService(remoteService, installConnection, BIND_DO_NOT_CREATE_AUTOMATICALLY);
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

            // [Display current dictionary]
            parent.currentDictionaryPreference = findPreference("preference_current_dictionary");

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
    // Display Current Dictionary Info.
    //

    private synchronized void syncWithStorage() {
        final Context context = getApplicationContext();

        if (!Storage.haveDatabase(context)) {
            currentDictionaryPreference.setVisible(false);
            currentDictionaryPreference.setSummary("");
            return;
        }

        // TODO: Think of a less bad interface for the database functions.
        // Do we really need *both* Optional and Exceptions? Surely one
        // alone would be enough...

        final Optional<String> versionString;

        try {
            versionString = Wiktionary.getMeta("CreatedOn", context);
        } catch (DatabaseException e) {
            final String name = e.getClass().getSimpleName();
            final String message = e.getMessage();
            final String summary = String.format("%s: %s", name, message);

            currentDictionaryPreference.setSummary(summary);
            currentDictionaryPreference.setVisible(true);

            return;
        }

        if (!versionString.isPresent()) {
            currentDictionaryPreference.setSummary("Unknown version");  // Now that's what I call bad UX!
            currentDictionaryPreference.setVisible(true);
            return;
        }

        currentDictionaryPreference.setSummary(versionString.get());
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
                onInstallDownloading(report);
                break;

            case EXTRACTING:
                onInstallExtracting(report);
                break;

            case INSTALLING:
                onInstallInstalling(report);
                break;

            case INSTALLED:
                onInstallInstalled(report);
                break;

            case ERROR:
                onInstallError(report);
                break;
        }

        syncWithStorage();
    }

    private synchronized void onInstallDownloading(DictionaryInstallService.Report report) {
        final String summary = DictionaryInstallService.format(report);

        runOnUiThread(() -> {
            checkForDictionariesPreference.setVisible(false);

            downloadNewDictionaryPreference.setSummary(summary);
            downloadNewDictionaryPreference.setVisible(true);
        });
    }

    @SuppressLint("DefaultLocale")
    private synchronized void onInstallExtracting(DictionaryInstallService.Report report) {
        final String summary = DictionaryInstallService.format(report);

        runOnUiThread(() -> {
            checkForDictionariesPreference.setVisible(false);

            downloadNewDictionaryPreference.setSummary(summary);
            downloadNewDictionaryPreference.setVisible(true);
        });
    }

    private synchronized void onInstallInstalling(DictionaryInstallService.Report report) {
        final String summary = DictionaryInstallService.format(report);

        runOnUiThread(() -> {
            checkForDictionariesPreference.setVisible(false);

            downloadNewDictionaryPreference.setSummary(summary);
            downloadNewDictionaryPreference.setVisible(true);
        });
    }

    private synchronized void onInstallInstalled(DictionaryInstallService.Report report) {
        final String summary = DictionaryInstallService.format(report);

        runOnUiThread(() -> {
            if (dictionaryInstallServiceIsBound) {
                dictionaryInstallService.stop();
            }

            downloadNewDictionaryPreference.setVisible(false);
            downloadNewDictionaryPreference.setSummary(summary);

            checkForDictionariesPreference.setVisible(true);
        });
    }

    private synchronized void onInstallError(DictionaryInstallService.Report report) {
        final String summary = DictionaryInstallService.format(report);

        runOnUiThread(() -> {
            if (dictionaryInstallServiceIsBound) {
                dictionaryInstallService.stop();
            }

            downloadNewDictionaryPreference.setSummary(summary);
            downloadNewDictionaryPreference.setVisible(true);

            checkForDictionariesPreference.setVisible(true);
        });
    }
}
