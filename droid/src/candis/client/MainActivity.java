package candis.client;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
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
import android.widget.TextView;
import android.widget.Toast;
import candis.client.comm.SecureConnection;
import candis.client.gui.InfoActivity;
import candis.client.gui.settings.SettingsActivity;
import candis.client.service.BackgroundService;
import candis.common.Settings;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class MainActivity extends Activity
				implements OnClickListener {

	private static final String TAG = "MainActivity";
	private static final Logger LOGGER = Logger.getLogger(TAG);
	private Button startButton;
	private Button mServiceButton;
	private InitTask mInitTask;
	private DroidContext mDroidContext;
	SecureConnection sconn = null;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.settings, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent newintent;
		switch (item.getItemId()) {
			case android.R.id.home:
				return true;
			case R.id.menu_info:
				newintent = new Intent(this, InfoActivity.class);
				newintent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(newintent);
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

		final Handler mHandler = new Handler();

		mServiceButton = (Button) findViewById(R.id.service_button);
		mServiceButton.setOnClickListener(this);

		// Load settings from R.raw.settings
		Settings.load(this.getResources().openRawResource(R.raw.settings));

		// TODO: fix, check, do, bla...
		mDroidContext = DroidContext.getInstance();
		// Init droid
		mInitTask = new InitTask(
						this,
						new File(this.getFilesDir(), Settings.getString("idstore")),
						new File(this.getFilesDir(), Settings.getString("profilestore")));
		mInitTask.execute();
//		CertAcceptRequest cad = new CertAcceptDialog(this, mHandler);
	}
	private boolean mToggleServiceButton = true;

	public void onClick(View v) {

		if (v == mServiceButton) {
			if (mToggleServiceButton) {
				mToggleServiceButton = false;
				Log.d(TAG, "onClick: starting service");
				startService(new Intent(this, BackgroundService.class).putExtra("DROID_CONTEXT", mDroidContext));
				mServiceButton.setText(getResources().getString(R.string.service_button_stop));
				((TextView) findViewById(R.id.servicetext)).setText(R.string.service_text_started);
				((TextView) findViewById(R.id.servicetext)).setTextColor(Color.rgb(0, 255, 0));
				String feedback = getResources().getString(R.string.start_msg);
				Toast.makeText(this, feedback, Toast.LENGTH_SHORT).show();
			}
			else {
				mToggleServiceButton = true;
				Log.d(TAG, "onClick: stopping service");
				stopService(new Intent(this, BackgroundService.class));
				mServiceButton.setText(getResources().getString(R.string.service_button_start));
				((TextView) findViewById(R.id.servicetext)).setText(R.string.service_text_stopped);
				((TextView) findViewById(R.id.servicetext)).setTextColor(Color.rgb(255, 0, 0));
				String feedback = getResources().getString(R.string.stop_msg);
				Toast.makeText(this, feedback, Toast.LENGTH_SHORT).show();
			}
		}

//		if (name.length() == 0) {
//			new AlertDialog.Builder(this).setMessage(
//							R.string.error_name_missing).setNeutralButton(
//							R.string.error_ok,
//							null).show();
//			return;
//		}

		if (v == startButton || v == mServiceButton) {
			int resourceId = v == startButton ? R.string.start_msg
							: R.string.stop_msg;
		}
	}
}
