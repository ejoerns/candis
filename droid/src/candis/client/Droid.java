package candis.client;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import candis.client.comm.CertAcceptRequest;
import candis.client.comm.CommRequestBroker;
import candis.client.comm.ReloadableX509TrustManager;
import candis.client.comm.SecureConnection;
import candis.common.RandomID;
import candis.common.Settings;
import candis.common.fsm.FSM;
import candis.distributed.droid.StaticProfile;
import candis.system.StaticProfiler;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.X509TrustManager;

/**
 *
 * @author Enrico Joerns
 */
public class Droid {

	private static final String TAG = "Droid";
	private final Activity mApp;
	private final InitTask mInitTask;
//	private final ProfilerTask mProfilerTask;
	private final ConnectTask mConnectTask;
//	private StaticProfile mProfile = null;
//	private RandomID mID = null;
	private DroidContext mDroidContext = null;
	private SecureConnection mSConn = null;
	private CommRequestBroker mCRB;
	private FSM mFSM;

	public Droid(Activity a) {
		mApp = a;
		mInitTask = new InitTask(
						mApp,
						new File(mApp.getFilesDir() + "/" + Settings.getString("idstore")),
						new File(mApp.getFilesDir() + "/" + Settings.getString("profilestore")));
		mConnectTask = new ConnectTask(mApp, new File(mApp.getFilesDir() + "/" + Settings.getString("truststore")));
	}

	public void start() {
		mInitTask.execute();
		Log.i(TAG, "CONNECTING...");
		try {
			mConnectTask.execute(
							Settings.getString("masteraddress"),
							Settings.getInt("masterport"),
							(X509TrustManager) new ReloadableX509TrustManager(
							new File(mApp.getFilesDir() + "/" + Settings.getString("truststore")), null));
		}
		catch (Exception ex) {
			Log.i(TAG, ex.toString());
		}
		try {
			mSConn = mConnectTask.get();
			mDroidContext = mInitTask.get();
			Log.i("Droid", "Starting CommRequestBroker");
			mFSM = new ClientStateMachine(
							mSConn,
							mDroidContext.getID(),
							mDroidContext.getProfile(),
							new Handler(),
							mApp.getFragmentManager());// TODO: check handler usage
			mCRB = new CommRequestBroker(
							mSConn.getInputStream(),
							mFSM);
			new Thread(mCRB).start();
			Log.i("Droid", "[DONE]");
		}
		catch (StreamCorruptedException ex) {
			Logger.getLogger(Droid.class.getName()).log(Level.SEVERE, null, ex);
		}
		catch (IOException ex) {
			Logger.getLogger(Droid.class.getName()).log(Level.SEVERE, null, ex);
		}
		catch (InterruptedException ex) {
			Logger.getLogger(Droid.class.getName()).log(Level.SEVERE, null, ex);
		}
		catch (ExecutionException ex) {
			Logger.getLogger(Droid.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * InitTask, checks for ID, profile, ?truststore? and generates if not
	 * existing.
	 */
	private class InitTask extends AsyncTask<Void, Object, DroidContext> {

		private static final String TAG = "GetIDTask";
		private final Activity mActivity;
		private final File mIDFile;
		private final File mProfileFile;
		private boolean mGenerateID = false;
		private boolean mGeneradeProfile = false;

		public InitTask(
						final Activity act,
						final File idfile,
						final File profilefile) {
			mActivity = act;
			mIDFile = idfile;
			mProfileFile = profilefile;
		}

		@Override
		protected void onPreExecute() {
		}

		/**
		 * Loads ID from file or creates a new if not exists.
		 *
		 * @param params
		 * @return
		 */
		@Override
		protected DroidContext doInBackground(Void... params) {
			// check for profile file
			if (!mIDFile.exists()) {
				Log.v(TAG, "ID file " + mIDFile + " does not exist. Will be generated...");
				mGenerateID = true;
				publishProgress("Generating ID...", Toast.LENGTH_SHORT);
			}

			// load or generate ID
			RandomID id = null;
			if (mGenerateID) {
				id = RandomID.init(mIDFile);
				publishProgress("ID (SHA-1): " + id.toSHA1(), Toast.LENGTH_LONG);
			}
			else {
				try {
					id = RandomID.readFromFile(mIDFile);
				}
				catch (FileNotFoundException ex) {
					Log.e(TAG, ex.toString());
				}
			}

			// check for profile file
			if (!mProfileFile.exists()) {
				Log.v(TAG, "Pofile file " + mProfileFile + " does not exist. Run Profiling...");
				mGeneradeProfile = true;
				publishProgress("Starting Profiler...", Toast.LENGTH_SHORT);
			}

			// load or generate profile
			StaticProfile profile;
			if (mGeneradeProfile) {
				profile = new StaticProfiler(mActivity).profile();
				StaticProfiler.writeProfile(mProfileFile, profile);
				publishProgress("Profile generated", Toast.LENGTH_SHORT);
			}
			else {
				profile = StaticProfiler.readProfile(mProfileFile);
			}
			return new DroidContext(id, profile);//TODO!!!Q
		}

		/**
		 * Shows Toasts in UI thread.
		 *
		 * @param msg String / Integer pait for message and show duration
		 */
		@Override
		protected void onProgressUpdate(Object... msg) {
			Toast.makeText(
							mActivity.getApplicationContext(),
							(String) msg[0],
							(Integer) msg[1]).show();
		}
	}

	/**
	 *
	 */
	private class ConnectTask extends AsyncTask<Object, Object, SecureConnection> {

		private static final String TAG = "ConnectTask";
		Activity activity;
		private final File tstore;
		CertAcceptRequest cad;

		public ConnectTask(final Activity act, final File ts) {
			activity = act;
			tstore = ts;
		}

		@Override
		protected void onPreExecute() {
			Toast.makeText(
							activity.getApplicationContext(),
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
