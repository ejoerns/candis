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
import candis.client.gui.CertAcceptDialog;
import candis.common.RandomID;
import candis.common.Settings;
import candis.common.Utilities;
import candis.common.fsm.FSM;
import candis.distributed.droid.StaticProfile;
import candis.system.StaticProfiler;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
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
	private final GetIDTask mIDTask;
	private final ProfilerTask mProfilerTask;
	private final ConnectTask mConnectTask;
	private StaticProfile mProfile = null;
	private RandomID mID = null;
	private SecureConnection mSConn = null;
	private CommRequestBroker mCRB;
	private FSM mFSM;

	public Droid(Activity a) {
		mApp = a;
		mIDTask = new GetIDTask(mApp, new File(mApp.getFilesDir() + "/" + Settings.getString("idstore")), mID);
		mProfilerTask = new ProfilerTask(a, new File(mApp.getFilesDir() + "/" + Settings.getString("profilestore")));
		mConnectTask = new ConnectTask(mApp, new File(mApp.getFilesDir() + "/" + Settings.getString("truststore")));
	}

	public void start() {
		mIDTask.execute();
		mProfilerTask.execute();
		Log.i(TAG, "CONNECTING...");
		try {
			mConnectTask.execute(
							Settings.getString("masteraddress"),
							Settings.getInt("masterport"),
							(X509TrustManager) new ReloadableX509TrustManager(
							new File(mApp.getFilesDir() + "/" + Settings.getString("truststore")), null));
		} catch (Exception ex) {
//							new File(mApp.getFilesDir() + "/" + Settings.getString("truststore")),
//							new CertAcceptDialog(mApp, new Handler())));
//		}
//		catch (Exception ex) {
			Log.i(TAG, ex.toString());
		}
		try {
			mSConn = mConnectTask.get();
			mProfile = mProfilerTask.get();
			Log.i("Droid", "Starting CommRequestBroker");
			mFSM = new ClientStateMachine(mSConn, mID, mProfile, new Handler(), mApp.getFragmentManager());// TODO: check handler usage
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

	// load profile
	// load random id
	// load truststore
	/**
	 * InitTask, checks for ID, profile, truststore and generates if not existing.
	 */
	/**
	 * Loads client ID and generates it if necessary
	 */
	private class GetIDTask extends AsyncTask<Void, Object, RandomID> {

		private static final String TAG = "GetIDTask";
		private final Activity app;
		private final File idfile;
		private RandomID randID;
		private boolean do_generate = false;

		public GetIDTask(final Activity a, final File f, RandomID id) {
			app = a;
			idfile = f;
			randID = id;
		}

		@Override
		protected void onPreExecute() {
			if (!idfile.exists()) {
				Log.v(TAG, "ID file " + idfile + " does not exist. Will be generated...");
				do_generate = true;
				Toast.makeText(
								app.getApplicationContext(),
								"Generating ID...",
								Toast.LENGTH_SHORT).show();
			}
		}

		/**
		 * Loads ID from file or creates a new if not exists.
		 *
		 * @param params
		 * @return
		 */
		@Override
		protected RandomID doInBackground(Void... params) {
			if (do_generate) {
				randID = RandomID.init(idfile);
			}
			else {
				try {
					randID = RandomID.readFromFile(idfile);
				}
				catch (FileNotFoundException ex) {
					Log.e(TAG, ex.toString());
				}
			}
			mID = randID;
			return null;
		}

		/**
		 * Shows Toast with SHA1 if new ID was created
		 *
		 * @param args
		 */
		@Override
		protected void onPostExecute(RandomID args) {
			if ((!do_generate) || (randID == null)) {
				return;
			}
			Toast.makeText(
							app.getApplicationContext(),
							"ClientID SHA1: " + Utilities.toSHA1String(randID.getBytes()),
							Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Loads profile data, runs profiler if no data available
	 */
	private class ProfilerTask extends AsyncTask<Void, Integer, StaticProfile> {

		private static final String TAG = "ProfilerTask";
		private final Activity act;
		private final File pfile;
		private boolean do_generate;

		public ProfilerTask(final Activity a, final File f) {
			act = a;
			pfile = f;
		}

		@Override
		protected void onPreExecute() {
			if (!pfile.exists()) {
				Log.v(TAG, "Pofile file " + pfile + " does not exist. Run Profiling...");
				do_generate = true;
				Toast.makeText(
								mApp.getApplicationContext(),
								"Starting Profiler...",
								Toast.LENGTH_SHORT).show();
			}
		}

		@Override
		protected StaticProfile doInBackground(Void... params) {
			StaticProfile profile;
			if (do_generate) {
				profile = new StaticProfiler(act).profile();
				StaticProfiler.writeProfile(pfile, profile);
			}
			else {
				profile = StaticProfiler.readProfile(pfile);
			}
			return profile;
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
//         setProgressPercent(progress[0]);
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
