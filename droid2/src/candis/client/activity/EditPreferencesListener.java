package candis.client.activity;

import android.content.SharedPreferences;
import android.util.Log;

/**
 * Singleton class to listen to EditPreference Updates
 *
 * @author Enrico Joerns
 */
public class EditPreferencesListener implements SharedPreferences.OnSharedPreferenceChangeListener {

  private static String TAG = EditPreferencesListener.class.getName();
  private static EditPreferencesListener instance = new EditPreferencesListener();

  private EditPreferencesListener() {
  }

  public static SharedPreferences.OnSharedPreferenceChangeListener getInstance() {
    return instance;
  }

  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    Log.i(TAG, "onSharedPreferenceChanged");
  }
}
