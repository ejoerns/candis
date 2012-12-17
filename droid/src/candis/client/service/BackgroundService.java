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
	private SecureConnection mSConn = null;
	private CommRequestBroker mCRB;
	private FSM mFSM;
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

		// if service running, only handle intent
		if (mRunning) {
			onNewIntent(intent);
			return START_STICKY;
		}

		mRunning = true;

		Log.i("LocalService", "Received start id " + startId + ": " + intent);
		mDroidContext.init((DroidContext) intent.getExtras().getSerializable("DROID_CONTEXT"));

		mConnectTask = new ConnectTask(
						getApplicationContext(),
						new File(getApplicationContext().getFilesDir(), Settings.getString("truststore")));

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
	private final AtomicBoolean cert_check_result = new AtomicBoolean(false);

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

	private class ConnectTask extends AsyncTask<Object, Object, SecureConnection> implements CertAcceptRequestHandler {

		private static final String TAG = "ConnectTask";
		Context mContext;
		private final File tstore;
		CertAcceptRequestHandler cad;
		private X509TrustManager tmanager;

		public ConnectTask(final Context context, final File ts) {
			mContext = context;
			tstore = ts;
		}

		@Override
		protected void onPreExecute() {
			Toast.makeText(
							mContext.getApplicationContext(),
							"Connecting...",
							Toast.LENGTH_SHORT).show();
		}

		@Override
		protected SecureConnection doInBackground(Object... params) {
			try {
				tmanager = new ReloadableX509TrustManager(
								new File(getApplicationContext().getFilesDir(), Settings.getString("truststore")),
								this);
			}
			catch (Exception ex) {
				Logger.getLogger(BackgroundService.class.getName()).log(Level.SEVERE, null, ex);
			}
			SecureConnection sc = new SecureConnection(tmanager);
			sc.connect((String) params[0], ((Integer) params[1]).intValue());

			try {
				mSConn = sc;

				Log.i(TAG, "Starting CommRequestBroker");
				mFSM = new ClientStateMachine(
								mSConn,
								mDroidContext.getID(),
								mDroidContext.getProfile(),
								//new Handler(),
								null,
								//							null,
								//							getApplication().getFragmentManager()
								null);// TODO: check handler usage
				mCRB = new CommRequestBroker(
								mSConn.getInputStream(),
								mFSM);
				new Thread(mCRB).start();
				Log.i("Droid", "[DONE]");
			}
			catch (StreamCorruptedException ex) {
				Log.e(TAG, ex.toString());
			}
			catch (IOException ex) {
				Log.e(TAG, ex.toString());
			}
//			catch (InterruptedException ex) {
//				Log.e(TAG, ex.toString());
//			}
//			catch (ExecutionException ex) {
//				Log.e(TAG, ex.toString());
//			}
			return sc;

		}

		@Override
		protected void onProgressUpdate(Object... arg) {
		}

		@Override
		protected void onPostExecute(SecureConnection args) {
			Log.w(TAG, "CONNECTED!!!!");
		}

		public boolean hasResult() {
			throw new UnsupportedOperationException("Not supported yet.");
		}

		public boolean userCheckAccept(X509Certificate cert) {
			Intent newintent = new Intent(mContext, MainActivity.class);
			newintent.setAction(CHECK_SERVERCERT)
							.putExtra("X509Certificate", cert)
							.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
							.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(newintent);

			synchronized (cert_check_result) {
				try {
					System.out.println("cert_check_result.wait()");
					cert_check_result.wait();
					System.out.println("cert_check_result.wait() done");
				}
				catch (InterruptedException ex) {
					Logger.getLogger(BackgroundService.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
			return cert_check_result.get(); // TODO...
		}
	}
	public static final String CHECK_SERVERCERT = "CHECK_SERVERCERT";
	public static final String RESULT_CHECK_SERVERCERT = "RESULT_CHECK_SERVERCERT";
}
