package candis.client.gui.settings;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import candis.client.R;

/**
 *
 * @author Enrico Joerns
 */
public class SettingsFragment extends PreferenceFragment {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);
	}
}