package candis.droid.activity;

import android.content.SharedPreferences;

/**
 * Singleton class to listen to EditPreference Updates
 *
 * @author Enrico Joerns
 */
public class EditPreferencesListener implements SharedPreferences.OnSharedPreferenceChangeListener {

  private static EditPreferencesListener instance = new EditPreferencesListener();

  private EditPreferencesListener() {
  }

  public static SharedPreferences.OnSharedPreferenceChangeListener getInstance() {
    return instance;
  }

  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    throw new UnsupportedOperationException("Not supported yet.");
  }
}
