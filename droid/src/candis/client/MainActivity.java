package candis.client;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import candis.client.comm.CertAcceptRequest;
import candis.client.comm.SecureConnection;
import java.util.logging.Logger;

public class MainActivity extends Activity
				implements OnClickListener {

	private static final String TAG = "MainActivity";
	private static final Logger logger = Logger.getLogger(TAG);
	private Button startButton;
	private Button stopButton;
	SecureConnection sconn = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		final Handler mHandler = new Handler();

		startButton = (Button) findViewById(R.id.start_button);
		startButton.setOnClickListener(this);

		stopButton = (Button) findViewById(R.id.stop_button);
		stopButton.setOnClickListener(this);


		// Load settings from R.raw.settings
		Settings.load(this, R.raw.settings);

		// Run droid
		new Droid(this).start();

		CertAcceptRequest cad = new CertAcceptDialog(this, mHandler);
	}

	public void onClick(View v) {

		if (v == startButton) {
			Log.d(TAG, "onClick: starting service");
			startService(new Intent(this, MyService.class));
			String feedback = getResources().getString(R.string.start_msg);
			Toast.makeText(this, feedback, Toast.LENGTH_LONG).show();
		} else if (v == stopButton) {
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
