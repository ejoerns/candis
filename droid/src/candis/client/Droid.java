package candis.client;

import android.app.Activity;
import android.app.ActivityManager;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import candis.client.comm.RandomID;
import candis.client.comm.ReloadableX509TrustManager;
import candis.client.comm.SecureConnection;
import candis.common.Utilities;
import candis.distributed.droid.StaticProfile;
import candis.system.StaticProfiler;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 *
 * @author Enrico Joerns
 */
public class Droid {

	private final Activity app;
	private final StaticProfile profile = null;
	private final RandomID id = null;
	private final TrustManager tm = null;
	private final GetIDTask idg;
	private final ProfilerTask ptask;
	private final ConnectTask ctask;

	public Droid(Activity a) {
		app = a;
		idg = new GetIDTask(app, new File(app.getFilesDir() + "/" + Settings.getString("idstore")), id);
		ptask = new ProfilerTask(a, new File(app.getFilesDir() + "/" + Settings.getString("profilestore")));
		ctask = new ConnectTask(a);
	}

	public void start() {
		idg.execute();
		ptask.execute();
		Log.i("Droid", "CONNECTING...");
		try {
			ConnectTask ct = new ConnectTask(app);
			ct.execute("10.0.2.2", 9999,
							(X509TrustManager) new ReloadableX509TrustManager(new File(app.getFilesDir() + "/" + Settings.getString("truststore")), null));
		} catch (Exception ex) {
			Log.i("Droid", ex.toString());
		}
	}

	// load profile
	// load random id
	// load truststore
	/**
	 * Loads client ID and generates it if necessary
	 */
	private class GetIDTask extends AsyncTask<Void, Object, RandomID> {

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
				Log.v("BLA", "ID file " + idfile + " does not exist. Will be generated...");
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
			} else {
				try {
					randID = RandomID.readFromFile(idfile);
				} catch (FileNotFoundException ex) {
					Logger.getLogger(Droid.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
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
							"SHA1: " + Utilities.toSHA1String(randID.getBytes()),
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
								app.getApplicationContext(),
								"Starting Profiler...",
								Toast.LENGTH_SHORT).show();
			}
		}

		@Override
		protected StaticProfile doInBackground(Void... params) {
			StaticProfile profile;
			if (do_generate) {
				profile = StaticProfiler.profile((ActivityManager) act.getSystemService(Activity.ACTIVITY_SERVICE));
				StaticProfiler.writeProfile(pfile, profile);
			} else {
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

		Activity activity;

		public ConnectTask(Activity act) {
			activity = act;
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
			SecureConnection sc = new SecureConnection(
							(String) params[0], ((Integer) params[1]).intValue(),
							(X509TrustManager) params[2]);
			sc.connect();
			return sc;
		}

		@Override
		protected void onProgressUpdate(Object... arg) {
		}

		@Override
		protected void onPostExecute(SecureConnection args) {
//			try {
//				sconn = get();
//			} catch (InterruptedException ex) {
//				Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
//			} catch (ExecutionException ex) {
//				Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
//			}
			Log.w("Droid", "CONNECTED!!!!");
		}
	}
}
