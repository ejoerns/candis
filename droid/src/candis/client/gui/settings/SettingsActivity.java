package candis.client.gui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.widget.Toast;
import candis.client.R;

/**
 *
 * @author Enrico Joerns
 */
public class SettingsActivity extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

  // Keys as defined in xml/preferences.xml
  public static final String HOSTNAME = "pref_host";
  public static final String PORTNAME = "pref_port";
  public static final String POWERMODE = "pref_powermode";
  // 
  private EditTextPreference mHostnamePreference;
  private EditTextPreference mPortnamePreference;
  private ListPreference mPowermodePreference;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Load the preferences from an XML resource
    addPreferencesFromResource(R.xml.preferences);

    // 
    mHostnamePreference = (EditTextPreference) getPreferenceScreen().findPreference(HOSTNAME);
    mPortnamePreference = (EditTextPreference) getPreferenceScreen().findPreference(PORTNAME);
    mPowermodePreference = (ListPreference) getPreferenceScreen().findPreference(POWERMODE);
  }

  @Override
  protected void onResume() {
    super.onResume();
    getPreferenceScreen().getSharedPreferences()
            .registerOnSharedPreferenceChangeListener(this);

    SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
    mHostnamePreference.setSummary(sharedPreferences.getString(HOSTNAME, ""));
    mPortnamePreference.setSummary(sharedPreferences.getString(PORTNAME, ""));
    mPowermodePreference.setSummary(sharedPreferences.getString(POWERMODE, ""));
  }

  @Override
  protected void onPause() {
    super.onPause();
    getPreferenceScreen().getSharedPreferences()
            .unregisterOnSharedPreferenceChangeListener(this);
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

    Log.i("SettingsActivity", "onSharedPreferenceChanged");
    // Let's do something a preference value changes
    if (key.equals(HOSTNAME)) {
      Preference hostPref = findPreference(key);
      hostPref.setSummary(sharedPreferences.getString(key, ""));
    }
  }
}
