package candis.client.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.util.Log;
import candis.client.DroidContext;
import candis.client.R;
import candis.client.service.BackgroundService;

/**
 *
 * @author Enrico Joerns
 */
public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

  private static final String TAG = SettingsFragment.class.getName();
  private Context mContext;
  private ServiceCommunicator mServiceCommunicator;

  public SettingsFragment(Context context, ServiceCommunicator scomm) {
    mContext = context;
    mServiceCommunicator = scomm;
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

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    System.out.println("***MainActivity.onSharedPreferenceChanged");
    // server settings changed
    if (key.equals("pref_key_servername") || (key.equals("pref_key_serverport"))) {
      EditTextPreference preference = (EditTextPreference) getPreferenceManager().findPreference(key);
      preference.setSummary(preference.getText());
      // restart service
      mServiceCommunicator.doUnbindService();
      mContext.stopService(new Intent(mContext, BackgroundService.class));
      mContext.startService(new Intent(mContext, BackgroundService.class));
      mServiceCommunicator.doBindService();
    }
    // service activation status changed
    else if (key.equals("pref_key_run_service")) {
      if (sharedPreferences.getBoolean("pref_key_run_service", false)) {
        Log.d(TAG, "startService()");
        mContext.startService(new Intent(mContext, BackgroundService.class));
        mServiceCommunicator.doBindService();
      }
      else {
        Log.d(TAG, "stopService()");
        mServiceCommunicator.doUnbindService();
        mContext.stopService(new Intent(mContext, BackgroundService.class));
      }
    }
  }
}
