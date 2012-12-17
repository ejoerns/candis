package candis.client.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import candis.client.DroidContext;
import candis.client.R;
import candis.common.Settings;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Background service that manages connection to master, receives tasks,
 * calculates and sends back results.
 *
 * @author Enrico Joerns
 */
public class BackgroundService extends Service {

	private static final String TAG = BackgroundService.class.getName();
	// Intent Actions
	public static final String CHECK_SERVERCERT = "CHECK_SERVERCERT";
	public static final String RESULT_CHECK_SERVERCERT = "RESULT_CHECK_SERVERCERT";
	private final DroidContext mDroidContext;
	private final AtomicBoolean mCertCheckResult = new AtomicBoolean(false);
	/// Used to avoid double-call of service initialization
	private boolean mRunning = false;

	public BackgroundService() {
		mDroidContext = DroidContext.getInstance();
	}

	@Override
	public IBinder onBind(Intent intent) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void onCreate() {
		System.out.println("Backgroundservice: onCreate()");
		Settings.load(this.getResources().openRawResource(R.raw.settings));
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		System.out.println("Backgroundservice: onStartCommand()");
		final ConnectTask connectTask;

		// if service running, only handle intent
		if (mRunning) {
			onNewIntent(intent);
			return START_STICKY;
		}

		mRunning = true;

		Log.i("LocalService", "Received start id " + startId + ": " + intent);
		mDroidContext.init((DroidContext) intent.getExtras().getSerializable("DROID_CONTEXT"));

		connectTask = new ConnectTask(
						mCertCheckResult,
						mDroidContext,
						getApplicationContext(),
						new File(getApplicationContext().getFilesDir(), Settings.getString("truststore")));

		Log.i(TAG, "CONNECTING...");
		try {
			connectTask.execute(
							Settings.getString("masteraddress"),
							Settings.getInt("masterport"));
		}
		catch (Exception ex) {
			Log.e(TAG, ex.toString());
		}

		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	// NOTE: not an override!
	public void onNewIntent(Intent intent) {
		System.out.println("onNewIntent() " + intent.getAction());
		if (intent.getAction().equals(RESULT_CHECK_SERVERCERT)) {
			synchronized (mCertCheckResult) {
				mCertCheckResult.set(intent.getBooleanExtra("RESULT", false));// TODO...
				mCertCheckResult.notifyAll();
				System.out.println("cert_check_result.notifyAll()");
			}
		}
	}

	@Override
	public void onDestroy() {
		// TODO: send terminate...
	}
}
