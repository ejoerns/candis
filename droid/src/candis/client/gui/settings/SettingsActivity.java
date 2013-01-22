package candis.client.gui.settings;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import candis.client.R;

/**
 *
 * @author Enrico Joerns
 */
public class SettingsActivity extends PreferenceActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Load the preferences from an XML resource
    addPreferencesFromResource(R.xml.preferences);
  }
}