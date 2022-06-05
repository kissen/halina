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

import java.util.Date;
import java.util.Optional;

import me.schaertl.halina.remote.MetaDownloadService;
import me.schaertl.halina.remote.DictionaryInstallService;
import me.schaertl.halina.storage.Wiktionary;
import me.schaertl.halina.support.Caller;
import me.schaertl.halina.support.FileSizeFormatter;
import me.schaertl.halina.support.RFC3399;

public class SettingsActivity extends AppCompatActivity {
    private static final int BIND_DO_NOT_CREATE_AUTOMATICALLY = 0;

    private MetaDownloadService metaDownloadService;
    private boolean metaDownloadServiceIsBound;

    private DictionaryInstallService dictionaryInstallService;
    private boolean dictionaryInstallServiceIsBound;
    private String dictionaryInstallServiceUrl;

    private Preference currentDictionaryPreference;
    private Preference currentDictionaryCopyingPreference;
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
        setContentView(R.layout.activity_settings);
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

        unbindService(metaConnection);
        metaDownloadServiceIsBound = false;

        unbindService(installConnection);
        dictionaryInstallServiceIsBound = false;

        syncWithServices();
    }

    @Override
    protected synchronized void onResume() {
        super.onResume();

        final IntentFilter metaFilter = new IntentFilter(MetaDownloadService.BROADCAST_FILTER);
        registerReceiver(metaReceiver, metaFilter);

        final IntentFilter installFilter = new IntentFilter(DictionaryInstallService.BROADCAST_FILTER);
        registerReceiver(installReceiver, installFilter);

        syncWithServices();
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
            syncWithServices();
        }
    };

    private final ServiceConnection metaConnection = new ServiceConnection() {
        private final SettingsActivity parent = SettingsActivity.this;

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            synchronized (parent) {
                final MetaDownloadService.MetaDownloadBinder binder = (MetaDownloadService.MetaDownloadBinder) iBinder;
                metaDownloadService = binder.getService();
                metaDownloadServiceIsBound = true;

                syncWithServices();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            metaDownloadServiceIsBound = false;
        }
    };

    private final BroadcastReceiver installReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            syncWithServices();
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

                syncWithServices();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            synchronized (parent) {
                dictionaryInstallServiceIsBound = false;
            }
        }
    };

    private synchronized boolean installServiceIsActive() {
        if (!dictionaryInstallServiceIsBound) {
            return false;
        }

        final DictionaryInstallService.Report report = dictionaryInstallService.getReport();

        switch (report.state) {
            case READY:
            case INSTALLED:
            case ERROR:
                return false;
            default:
                return true;
        }
    }

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

    private synchronized void onViewCopyingClicked() {
        final Context context = getApplicationContext();
        final Optional<String> copying = Wiktionary.getCopying(context);

        if (copying.isPresent()) {
            Caller.callTextViewActivityFrom(this, "Copying", copying.get());
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

            // [View copying]
            parent.currentDictionaryCopyingPreference = findPreference("preference_current_dictionary_copying");
            parent.currentDictionaryCopyingPreference.setOnPreferenceClickListener(preference -> {
                parent.runOnUiThread(parent::onViewCopyingClicked);
                return false;
            });

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

    private synchronized void syncWithServices() {
        syncWithStorage();

        if (installServiceIsActive()) {
            syncWithInstallService();
        } else {
            syncWithMetaDownloadService();
        }
    }

    private synchronized void syncWithStorage() {
        final Context context = getApplicationContext();
        final Optional<Date> installed = Wiktionary.getCreatedOn(context);

        if (installed.isPresent()) {
            // We have some version installed.

            final String summary = new RFC3399().format(installed.get());

            currentDictionaryPreference.setSummary(summary);
            currentDictionaryPreference.setVisible(true);
            currentDictionaryCopyingPreference.setVisible(true);
        } else {
            // We have no dictionary installed. The user should probably install one...

            currentDictionaryCopyingPreference.setVisible(false);
            currentDictionaryPreference.setVisible(false);
            currentDictionaryPreference.setSummary("");
        }
    }

    //
    // Callbacks triggered by Meta Service.
    //

    private synchronized void syncWithMetaDownloadService() {
        if (!metaDownloadServiceIsBound) {
            onMetaDisconnected();
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

    private synchronized void onMetaDisconnected() {
        runOnUiThread(() -> {
            checkForDictionariesPreference.setVisible(false);
            downloadNewDictionaryPreference.setVisible(false);
        });
    }

    private synchronized void onMetaReady() {
        runOnUiThread(() -> {
            checkForDictionariesPreference.setSummary(checkForDictionariesSummary);
            checkForDictionariesPreference.setVisible(true);

            downloadNewDictionaryPreference.setSummary("");
            downloadNewDictionaryPreference.setVisible(false);
        });
    }

    private synchronized void onMetaDownloading() {
        runOnUiThread(() -> {
            checkForDictionariesPreference.setSummary("Downloading...");
            checkForDictionariesPreference.setVisible(true);

            downloadNewDictionaryPreference.setSummary("");
            downloadNewDictionaryPreference.setVisible(false);
        });
    }

    private synchronized void onMetaDownloaded(MetaDownloadService.RemoteDictionaryMeta meta) {
        if (hasNewerDictionary(meta)) {
            // meta contains a new dictionary the user might want to download

            final String fileSize = FileSizeFormatter.format(meta.nbytes);
            final String summary = String.format("%s (%s)", meta.version, fileSize);

            runOnUiThread(() -> {
                checkForDictionariesPreference.setSummary(checkForDictionariesSummary);
                checkForDictionariesPreference.setVisible(true);

                downloadNewDictionaryPreference.setSummary(summary);
                downloadNewDictionaryPreference.setVisible(true);
            });
        } else {
            // meta contains an older/equal version of the dictionary

            final String summary = "Latest version already installed";

            runOnUiThread(() -> {
                checkForDictionariesPreference.setSummary(summary);
                checkForDictionariesPreference.setVisible(true);

                downloadNewDictionaryPreference.setVisible(false);
                downloadNewDictionaryPreference.setSummary("");
            });
        }
    }

    private synchronized void onMetaFailed(Exception error) {
        final String name = error.getClass().getSimpleName();
        final String summary = String.format("%s: %s", name, error.getMessage());

        runOnUiThread(() -> {
            checkForDictionariesPreference.setSummary(summary);
            checkForDictionariesPreference.setVisible(true);

            downloadNewDictionaryPreference.setVisible(false);
            downloadNewDictionaryPreference.setSummary("");
        });
    }

    private synchronized boolean hasNewerDictionary(MetaDownloadService.RemoteDictionaryMeta meta) {
        final Context context = getApplicationContext();

        final Optional<Date> installed = Wiktionary.getCreatedOn(context);
        final Optional<Date> available = Wiktionary.parseDateString(meta.version);

        if (!installed.isPresent()) {
            return true;
        }

        if (!available.isPresent()) {
            return false;
        }

        return available.get().after(installed.get());
    }

    //
    // Callbacks triggered by Install Service.
    //

    private synchronized void syncWithInstallService() {
        if (!dictionaryInstallServiceIsBound) {
            onInstallDisconnected();
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
    }

    private synchronized void onInstallDisconnected() {
        runOnUiThread(() -> {
            checkForDictionariesPreference.setVisible(false);
            downloadNewDictionaryPreference.setVisible(false);
        });
    }

    private synchronized void onInstallDownloading(DictionaryInstallService.Report report) {
        final String summary = DictionaryInstallService.format(report);

        runOnUiThread(() -> {
            checkForDictionariesPreference.setSummary("");
            checkForDictionariesPreference.setVisible(false);

            downloadNewDictionaryPreference.setSummary(summary);
            downloadNewDictionaryPreference.setVisible(true);
        });
    }

    @SuppressLint("DefaultLocale")
    private synchronized void onInstallExtracting(DictionaryInstallService.Report report) {
        final String summary = DictionaryInstallService.format(report);

        runOnUiThread(() -> {
            checkForDictionariesPreference.setSummary("");
            checkForDictionariesPreference.setVisible(false);

            downloadNewDictionaryPreference.setSummary(summary);
            downloadNewDictionaryPreference.setVisible(true);
        });
    }

    private synchronized void onInstallInstalling(DictionaryInstallService.Report report) {
        final String summary = DictionaryInstallService.format(report);

        runOnUiThread(() -> {
            checkForDictionariesPreference.setSummary("");
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

            checkForDictionariesPreference.setSummary(checkForDictionariesSummary);
            checkForDictionariesPreference.setVisible(true);

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

            checkForDictionariesPreference.setSummary(checkForDictionariesSummary);
            checkForDictionariesPreference.setVisible(true);

            downloadNewDictionaryPreference.setSummary(summary);
            downloadNewDictionaryPreference.setVisible(true);
        });
    }
}
