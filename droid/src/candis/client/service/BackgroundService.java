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
import candis.client.R;
import candis.client.comm.CertAcceptRequest;
import candis.client.comm.CommRequestBroker;
import candis.client.comm.ReloadableX509TrustManager;
import candis.client.comm.SecureConnection;
import candis.common.Settings;
import candis.common.fsm.FSM;
import java.io.File;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.util.concurrent.ExecutionException;
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
		Log.i("LocalService", "Received start id " + startId + ": " + intent);
		mDroidContext.init((DroidContext) intent.getExtras().getSerializable("DROID_CONTEXT"));

		mConnectTask = new ConnectTask(
						getApplicationContext(),
						new File(getApplicationContext().getFilesDir(), Settings.getString("truststore")));
//		Intent testIntent = new Intent();
//		testIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
//		startActivity(testIntent);
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		Log.i(TAG, "CONNECTING...");
		try {
			mConnectTask.execute(
							Settings.getString("masteraddress"),
							Settings.getInt("masterport"),
							(X509TrustManager) new ReloadableX509TrustManager(
							new File(getApplicationContext().getFilesDir() + "/" + Settings.getString("truststore")), null));
		}
		catch (Exception ex) {
			Log.i(TAG, ex.toString());
		}
		try {
			mSConn = mConnectTask.get();
//			mDroidContext = mInitTask.get();
			if (mDroidContext == null) {
				System.out.println("mDroidContext = null");
			}
			else {
				if (mDroidContext.getID() == null) {
					System.out.println("mDroidContext.getID() = null");
				}
				if (mDroidContext.getProfile() == null) {
					System.out.println("mDroidContext.getProfile() = null");
				}
			}
			Log.i(TAG, "Starting CommRequestBroker");
			mFSM = new ClientStateMachine(
							mSConn,
							mDroidContext.getID(),
							mDroidContext.getProfile(),
							new Handler(),
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
		catch (InterruptedException ex) {
			Log.e(TAG, ex.toString());
		}
		catch (ExecutionException ex) {
			Log.e(TAG, ex.toString());
		}
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
	}

	private class ConnectTask extends AsyncTask<Object, Object, SecureConnection> {

		private static final String TAG = "ConnectTask";
		Context mContext;
		private final File tstore;
		CertAcceptRequest cad;

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
			SecureConnection sc = new SecureConnection((X509TrustManager) params[2]);
			sc.connect((String) params[0], ((Integer) params[1]).intValue());
			return sc;
		}

		@Override
		protected void onProgressUpdate(Object... arg) {
		}

		@Override
		protected void onPostExecute(SecureConnection args) {
			Log.w(TAG, "CONNECTED!!!!");
		}
	}
}
