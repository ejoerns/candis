package candis.client.activity;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import candis.client.R;

public class EditPreferencesHC extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.e("FOO", "Starting EditPreferencesHC");

    // Display the fragment as the main content.
    getFragmentManager().beginTransaction()
            .replace(android.R.id.content, new SettingsFragment())
            .commit();
  }

  @Override
  protected void onResume() {
    super.onResume();
    PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
            .registerOnSharedPreferenceChangeListener(EditPreferencesListener.getInstance());
  }

  @Override
  protected void onPause() {
    super.onPause();
    
    PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
            .unregisterOnSharedPreferenceChangeListener(EditPreferencesListener.getInstance());
  }

  public static class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      // Load the preferences from an XML resource
      addPreferencesFromResource(R.xml.preferences);
      addPreferencesFromResource(R.xml.preferences2);
    }
  }
}
