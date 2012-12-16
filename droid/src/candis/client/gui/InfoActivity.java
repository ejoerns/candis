package candis.client.gui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import candis.client.R;
import candis.client.gui.settings.SettingsActivity;

/**
 * Shows some informations about client.
 *
 * I.e. ID, profile, etc.
 *
 * @todo...
 *
 * @author Enrico Joerns
 */
public class InfoActivity extends Activity {

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.info);
		((TextView) findViewById(R.id.info_id_id)).setText("35:EC:D9:90:BC:A8:77:92:81:E0:E0:06:7B:9C:25:6A:92:51:9A:14");
		// ToDo add your GUI initialization code here        
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent newintent;
		switch (item.getItemId()) {
			case android.R.id.home:
				finish();
				return true;
			case R.id.menu_info:
				finish();
				return true;
			case R.id.menu_settings:
				newintent = new Intent(this, SettingsActivity.class);
				newintent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(newintent);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.settings, menu);
		return true;
	}
}
