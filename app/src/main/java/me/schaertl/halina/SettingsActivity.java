package me.schaertl.halina;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Optional;

import me.schaertl.halina.remote.structs.Phase;
import me.schaertl.halina.remote.structs.Progress;
import me.schaertl.halina.remote.structs.RemoteDictionaryHandler;
import me.schaertl.halina.remote.RemoteDictionaryService;
import me.schaertl.halina.remote.RemoteDictionaryMeta;
import me.schaertl.halina.support.Toaster;
import me.schaertl.halina.support.FileSizeFormatter;

public class SettingsActivity extends AppCompatActivity implements RemoteDictionaryHandler {
    private Preference checkForDictionariesPreference;
    private Preference downloadNewDictionaryPreference;

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

        // Get the associated service ready.
        final Intent next = new Intent(this, RemoteDictionaryService.class);
        this.startService(next);

        // Set up handlers for the service.
        RemoteDictionaryService.registerHandlerFrom(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        final Intent remoteDictionaryService = new Intent(this, RemoteDictionaryService.class);
        this.stopService(remoteDictionaryService);

        super.onDestroy();
    }

    @Override
    public synchronized void onNewMeta(RemoteDictionaryMeta meta) {
        runOnUiThread(() -> {
            final String nbytes = FileSizeFormatter.format(meta.nbytes);
            final String summary = String.format("%s (%s)", meta.version, nbytes);
            Toaster.toastFrom(SettingsActivity.this, "found dictionary to download");

            downloadNewDictionaryPreference.setSummary(summary);
            downloadNewDictionaryPreference.setVisible(true);
        });
    }

    @Override
    public synchronized void onNewMetaFailed(String errorMessage) {
        runOnUiThread(() -> {
            final String message = String.format("error: could not determine new dictionary: %s", errorMessage);
            Toaster.toastFrom(this, message);

            downloadNewDictionaryPreference.setVisible(false);
            downloadNewDictionaryPreference.setSummary("");
        });
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

    private synchronized void onCheckForNewDictionaryClicked() {
        final Optional<RemoteDictionaryService> service = RemoteDictionaryService.getInstance();

        if (service.isPresent()) {
            Toaster.toastFrom(this, "checking for new dictionary...");
            service.get().startNewMetaDownload();
        }
    }

    private synchronized void onDownloadNewDictionaryClicked() {
        final Optional<RemoteDictionaryService> service = RemoteDictionaryService.getInstance();

        if (service.isPresent()) {
            final String hardCodedUrl = "https://halina.schaertl.me/dictionaries/enwiktionary-latest-pages-articles.sqlite3.gz";

            Toaster.toastFrom(this, "downloading new dictionary...");
            service.get().startNewDownloadFrom(hardCodedUrl);
        }
    }

    public static class ClickableSettingsFragment extends PreferenceFragmentCompat  {
        private final SettingsActivity parentActivity;

        public ClickableSettingsFragment(SettingsActivity parentActivity) {
            this.parentActivity = parentActivity;
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            // Load preferences from XML.
            setPreferencesFromResource(R.xml.preference_screen, rootKey);

            // [Check for new dictionary]
            parentActivity.checkForDictionariesPreference = findPreference("preference_download_meta");
            parentActivity.checkForDictionariesPreference.setOnPreferenceClickListener(preference -> {
                parentActivity.runOnUiThread(parentActivity::onCheckForNewDictionaryClicked);
                return false;
            });

            // [Download new dictionary]
            parentActivity.downloadNewDictionaryPreference = findPreference("preference_download_new_dictionary");
            parentActivity.downloadNewDictionaryPreference.setOnPreferenceClickListener(perference -> {
                parentActivity.runOnUiThread(parentActivity::onDownloadNewDictionaryClicked);
                return false;
            });
        }
    }
}
