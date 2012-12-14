package candis.client;

import candis.client.gui.CertAcceptDialog;
import candis.common.Settings;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import candis.client.comm.CertAcceptRequest;
import candis.client.comm.SecureConnection;
import candis.client.gui.settings.SettingsActivity;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class MainActivity extends Activity
				implements OnClickListener {

	private static final String TAG = "MainActivity";
	private static final Logger LOGGER = Logger.getLogger(TAG);
	private Button startButton;
	private Button stopButton;
	SecureConnection sconn = null;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.settings, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
//			case android.R.id.home:
//				// app icon in action bar clicked; go home
//				Intent intent = new Intent(this, MainActivity.class);
//				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//				startActivity(intent);
//				return true;
			case R.id.menu_settings:
				// app icon in action bar clicked; go home
				Intent newintent = new Intent(this, SettingsActivity.class);
				newintent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(newintent);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// load Shared Preferences
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

		// load logger.properties from /raw/res
		final InputStream inputStream = getResources().openRawResource(R.raw.logger);
		try {
			LogManager.getLogManager().readConfiguration(inputStream);
		}
		catch (final IOException e) {
			Logger.getAnonymousLogger().severe("Could not load default logging.properties file");
			Logger.getAnonymousLogger().severe(e.getMessage());
		}

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

//		if (true) {
//			return;
//		}
		final Handler mHandler = new Handler();

		stopButton = (Button) findViewById(R.id.stop_button);
		stopButton.setOnClickListener(this);

		// Load settings from R.raw.settings
		Settings.load(this.getResources().openRawResource(R.raw.settings));

		// Run droid
		new Droid(this).start();

//		CertAcceptRequest cad = new CertAcceptDialog(this, mHandler);
	}

	public void onClick(View v) {

		if (v == startButton) {
			Log.d(TAG, "onClick: starting service");
			startService(new Intent(this, MyService.class));
			String feedback = getResources().getString(R.string.start_msg);
			Toast.makeText(this, feedback, Toast.LENGTH_LONG).show();
		}
		else if (v == stopButton) {
			Log.d(TAG, "onClick: stopping service");
			stopService(new Intent(this, MyService.class));
			String feedback = getResources().getString(R.string.stop_msg);
			Toast.makeText(this, feedback, Toast.LENGTH_LONG).show();
		}

//		if (name.length() == 0) {
//			new AlertDialog.Builder(this).setMessage(
//							R.string.error_name_missing).setNeutralButton(
//							R.string.error_ok,
//							null).show();
//			return;
//		}

		if (v == startButton || v == stopButton) {
			int resourceId = v == startButton ? R.string.start_msg
							: R.string.stop_msg;
		}
	}
}
