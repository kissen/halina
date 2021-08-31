package me.schaertl.halina;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import me.schaertl.halina.support.Caller;

public class SettingsActivity extends AppCompatActivity {

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

    public static class ClickableSettingsFragment extends PreferenceFragmentCompat  {
        private final AppCompatActivity parentActivity;

        public ClickableSettingsFragment(AppCompatActivity parentActivity) {
            this.parentActivity = parentActivity;
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            // Load preferences from XML.
            setPreferencesFromResource(R.xml.preference_screen, rootKey);

            // Set up event handlers.
            final Preference dictionaryManagementPreference = findPreference("preference_download_new_dictionary");
            if (dictionaryManagementPreference != null) {
                dictionaryManagementPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        parentActivity.runOnUiThread(() -> Caller.callDictionaryManagementFrom(parentActivity));
                        return false;
                    }
                });
            }
        }
    }
}