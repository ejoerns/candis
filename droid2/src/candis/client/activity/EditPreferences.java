package candis.client.activity;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;
import candis.client.R;

public class EditPreferences extends PreferenceActivity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.e("FOO", "Starting EditPreferences");

    addPreferencesFromResource(R.xml.preferences);
    addPreferencesFromResource(R.xml.preferences2);
  }

  @Override
  protected void onResume() {
    super.onResume();
    getPreferenceScreen().getSharedPreferences()
            .registerOnSharedPreferenceChangeListener(EditPreferencesListener.getInstance());
  }

  @Override
  protected void onPause() {
    super.onPause();
    getPreferenceScreen().getSharedPreferences()
            .unregisterOnSharedPreferenceChangeListener(EditPreferencesListener.getInstance());
  }
}
