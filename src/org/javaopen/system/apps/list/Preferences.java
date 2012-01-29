package org.javaopen.system.apps.list;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Preferences extends PreferenceActivity {

    EditTextPreference pathPreference = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        addPreferencesFromResource(R.xml.preferences);
        pathPreference = 
            (EditTextPreference)this.getPreferenceScreen().findPreference(
                    getString(R.string.preference_su_path_key));

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String key = getString(R.string.preference_su_path_key);
        String defValue = getString(R.string.preference_su_path_default);
        String value = sp.getString(key, defValue);
        
        pathPreference.setText(value);
        pathPreference.setSummary(value);
        
        pathPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary(newValue.toString());
                return true;
            }
        });
    }
    

}
