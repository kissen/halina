package me.schaertl.halina.support;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import me.schaertl.halina.R;

public class SettingsFragment extends PreferenceFragmentCompat  {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference_screen, rootKey);
    }
}
