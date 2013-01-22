package candis.client.gui.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import candis.client.R;
import candis.client.gui.InfoActivity;

/**
 *
 * @author Enrico Joerns
 */
public class SettingsActivity extends FragmentActivity implements OnSharedPreferenceChangeListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Display the fragment as the main content.
// TODO: fix api compatibility
//		getFragmentManager().beginTransaction()
//						.replace(android.R.id.content, new SettingsFragment())
//						.commit();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent newintent;
//		switch (item.getItemId()) {
//			// app icon in action bar clicked; go home
//			case android.R.id.home:
//				finish();
//				return true;
//			case R.id.menu_info:
//				newintent = new Intent(this, InfoActivity.class);
//				newintent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//				startActivity(newintent);
//				return true;
//			case R.id.menu_settings:
//				finish();
//				return true;
//			default:
				return super.onOptionsItemSelected(item);
//		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
//		inflater.inflate(R.menu.settings, menu);
		return true;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		System.out.println("Preference " + key + " changed...");
	}
}
