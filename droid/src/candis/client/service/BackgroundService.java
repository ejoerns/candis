package candis.client.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import candis.client.ClientStateMachine;
import candis.client.DroidContext;
import candis.client.MainActivity;
import candis.client.R;
import candis.client.comm.CertAcceptHandler;
import candis.client.comm.CertAcceptRequestHandler;
import candis.client.comm.CommRequestBroker;
import candis.client.comm.ReloadableX509TrustManager;
import candis.client.comm.SecureConnection;
import candis.client.gui.settings.SettingsActivity;
import candis.common.Settings;
import candis.common.fsm.FSM;
import java.io.File;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.X509TrustManager;

/**
 * Background service that manages connection to master, receives tasks,
 * calculates and sends back results.
 *
 * @author Enrico Joerns
 */
public class BackgroundService extends Service {

	private static final String TAG = BackgroundService.class.getName();
	private ConnectTask mConnectTask;
	private final DroidContext mDroidContext;
	private boolean mRunning = false;
	private final AtomicBoolean cert_check_result = new AtomicBoolean(false);

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

		// if service running, only handle intent
		if (mRunning) {
			onNewIntent(intent);
			return START_STICKY;
		}

		mRunning = true;

		Log.i("LocalService", "Received start id " + startId + ": " + intent);
		mDroidContext.init((DroidContext) intent.getExtras().getSerializable("DROID_CONTEXT"));

		mConnectTask = new ConnectTask(
						cert_check_result,
						mDroidContext,
						getApplicationContext(),
						new File(getApplicationContext().getFilesDir(), Settings.getString("truststore")), this);

		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		Log.i(TAG, "CONNECTING...");
		ReloadableX509TrustManager tmanager;
		try {
			mConnectTask.execute(
							Settings.getString("masteraddress"),
							Settings.getInt("masterport"));
		}
		catch (Exception ex) {
			Log.i(TAG, ex.toString());
		}
		return START_STICKY;
	}

	// NOTE: not an override!
	public void onNewIntent(Intent intent) {
		System.out.println("onNewIntent() " + intent.getAction());
		if (intent.getAction().equals(RESULT_CHECK_SERVERCERT)) {
			synchronized (cert_check_result) {
				cert_check_result.set(intent.getBooleanExtra("RESULT", false));// TODO...
				cert_check_result.notifyAll();
				System.out.println("cert_check_result.notifyAll()");
			}
		}
	}

	@Override
	public void onDestroy() {
	}
	public static final String CHECK_SERVERCERT = "CHECK_SERVERCERT";
	public static final String RESULT_CHECK_SERVERCERT = "RESULT_CHECK_SERVERCERT";
}
