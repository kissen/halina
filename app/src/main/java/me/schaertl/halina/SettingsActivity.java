package me.schaertl.halina;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceFragmentCompat;

import me.schaertl.halina.R;
import me.schaertl.halina.support.SettingsFragment;

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
        fragmentManger.beginTransaction().replace(R.id.settings, new SettingsFragment()).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}