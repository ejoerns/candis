package candis.droid;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import recandis.droid.R;

public class EditPreferences extends PreferenceActivity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    addPreferencesFromResource(R.xml.preferences);
    addPreferencesFromResource(R.xml.preferences2);
  }
}
