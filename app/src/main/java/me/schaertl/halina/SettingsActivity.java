package me.schaertl.halina;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.apache.commons.lang3.NotImplementedException;

import me.schaertl.halina.storage.RemoteDictionaryChecker;
import me.schaertl.halina.storage.RemoteDictionaryMeta;
import me.schaertl.halina.storage.Toaster;
import me.schaertl.halina.support.FileSizeFormatter;
import me.schaertl.halina.support.Result;

public class SettingsActivity extends AppCompatActivity {
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

        // Register handler for updated meta information.
        final IntentFilter remoteDictionaryCheckerFilter = new IntentFilter(RemoteDictionaryChecker.INTENT_ACTION);
        this.registerReceiver(onRemoteDictionaryChecker, remoteDictionaryCheckerFilter);
    }

    @Override
    protected void onDestroy() {
        // Unregister all broadcast handlers.
        this.unregisterReceiver(onRemoteDictionaryChecker);

        super.onDestroy();
    }

    private final BroadcastReceiver onRemoteDictionaryChecker = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final Result<RemoteDictionaryMeta> result = RemoteDictionaryChecker.getResultsFor(intent);

            if (result.isError()) {
                final String summary = String.format("Error: %s", result.getErrorMessage());

                runOnUiThread(() -> {
                    downloadNewDictionaryPreference.setVisible(false);
                    checkForDictionariesPreference.setSummary(summary);
                });
            } else {
                final RemoteDictionaryMeta meta = result.getResult();
                final String nbytes = FileSizeFormatter.format(meta.nbytes);
                final String summary = String.format("%s (%S)", meta.version, nbytes);

                runOnUiThread(() -> {
                    downloadNewDictionaryPreference.setVisible(true);
                    checkForDictionariesPreference.setSummary(summary);
                });
            }
        }
    };

    private void onCheckForNewDictionaryClicked() {
        final Thread checker = new RemoteDictionaryChecker(this.getApplicationContext());
        checker.start();

        Toaster.toastFrom(this, "checking for new dictionary...");
    }

    private void onDownloadNewDictionaryClicked() {
        throw new NotImplementedException();
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
