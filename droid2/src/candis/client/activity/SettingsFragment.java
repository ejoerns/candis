package candis.client.activity;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import candis.client.DroidContext;
import candis.client.R;

/**
 *
 * @author Enrico Joerns
 */
public class SettingsFragment extends PreferenceFragment {

  private static final String TAG = SettingsFragment.class.getName();

  public static SettingsFragment newInstance() {
    SettingsFragment sfrag = new SettingsFragment();
    return sfrag;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Load the preferences from an XML resource
    addPreferencesFromResource(R.xml.preferences);
    addPreferencesFromResource(R.xml.preferences2);

    Preference preference;
    preference = getPreferenceManager().findPreference("pref_key_droid_id");
    preference.setSummary(DroidContext.getInstance().getID().toSHA1());
    // information for device profile screen
    preference = getPreferenceManager().findPreference("pref_key_device_name");
    preference.setSummary(String.valueOf(DroidContext.getInstance().getProfile().model));
    preference = getPreferenceManager().findPreference("pref_key_device_id");
    preference.setSummary(String.valueOf(DroidContext.getInstance().getProfile().id));
    preference = getPreferenceManager().findPreference("pref_key_cpu_count");
    preference.setSummary(String.valueOf(DroidContext.getInstance().getProfile().processors));
    preference = getPreferenceManager().findPreference("pref_key_memory");
    preference.setSummary(String.format("%d MB", DroidContext.getInstance().getProfile().memoryMB));
    // server settings
    preference = getPreferenceManager().findPreference("pref_key_servername");
    preference.setSummary(((EditTextPreference) preference).getText());
    preference = getPreferenceManager().findPreference("pref_key_serverport");
    preference.setSummary(((EditTextPreference) preference).getText());
  }
}
