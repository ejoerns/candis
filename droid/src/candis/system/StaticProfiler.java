package candis.system;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.util.Log;
import candis.client.CertAcceptDialog;
import candis.client.R;
import candis.distributed.droid.StaticProfile;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 *
 * Not to be run in GUI thread!
 *
 * @author Enrico Joerns
 */
public class StaticProfiler {

	private static final String TAG = "Profiler";
	private final Activity mActivity;
	private final ActivityManager mActivityManager;
	private final Handler handler;
	private final AtomicBoolean accepted = new AtomicBoolean(false);

	public StaticProfiler(final Activity act, final Handler handler) {
		this.mActivity = act;
		this.handler = handler;
		this.mActivityManager = (ActivityManager) this.mActivity.getSystemService(Activity.ACTIVITY_SERVICE);
	}

	/**
	 * Runs profiling tests and returns a StaticProfile object including all
	 * collected informations.
	 *
	 * @return
	 */
	public StaticProfile profile() {
		return new StaticProfile(
						getDeviecID(),
						getModel(),
						getMemorySize(),
						getNumCores(),
						benchmark());
	}

	public static long benchmark() {
//		// get number of running processes / services
//		handler.post(new Runnable() {
//			public void run() {
//				KillProcessesDialogFragment kpdf = new KillProcessesDialogFragment();
//				kpdf.show(mActivity.getFragmentManager(), TAG);
//			}
//		});
//
//		Log.i(TAG, "Waiting for accept");
//		synchronized (accepted) {
//			try {
//				accepted.wait();
//			} catch (InterruptedException ex) {
//				Logger.getLogger(StaticProfiler.class.getName()).log(Level.SEVERE, null, ex);
//			}
//		}
//		Log.i(TAG, "Received accept: " + accepted);
		long start = SystemClock.uptimeMillis();
		Benchmark bench = new FakBenchmark();
		bench.run();
		long result = SystemClock.uptimeMillis() - start;
		Log.i(TAG, "It took me " + result + " millis to finish calculation");

		return result;
	}

	/**
	 * Kills all background processes.
	 *
	 * @param mActivityManager
	 */
	private static void killBackgroundProcesses(final ActivityManager activityManager) {
		int nr_of_killed_processes = 0;
		for (ActivityManager.RunningAppProcessInfo apps : activityManager.getRunningAppProcesses()) {
			if (apps != null) {
				Log.v(TAG, "Process: " + apps.processName);
				if ((!apps.processName.contains("com.android"))
								&& (!apps.processName.contains("system"))
								&& (!apps.processName.contains("android.process"))) {
					Log.w(TAG, "Killing background process: " + apps.processName);
					activityManager.killBackgroundProcesses(apps.processName);
				}
				nr_of_killed_processes++;
			}
		}
		Log.i(TAG, "Killed processes: " + nr_of_killed_processes);
	}

	/**
	 *
	 * @return
	 */
	public long getMemorySize() {
		MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
		// mActivityManager.getMemoryInfo();
		Log.i(TAG, "Memory: " + memoryInfo.availMem);

		return memoryInfo.availMem;
	}

	public class KillProcessesDialogFragment extends DialogFragment {

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			// Use the Builder class for convenient dialog construction
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			final Message msg = Message.obtain();
			try {
				builder.setMessage("Will terminate all current processes")
								.setTitle("Warning")
								.setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						Log.v(TAG, "User chose to accept certificate");
						synchronized (accepted) {
							accepted.set(true);
							accepted.notify();
						}
					}
				})
								.setNegativeButton(R.string.reject, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						Log.v(TAG, "User chose to reject certificate");
						synchronized (accepted) {
							accepted.set(false);
							accepted.notify();
						}
					}
				});
			} catch (Exception ex) {
				Logger.getLogger(CertAcceptDialog.class.getName()).log(Level.SEVERE, null, ex);
			}
			// Create the AlertDialog object and return it
			return builder.create();
		}
	}

	/**
	 * Gets the number of cores available in this device, across all processors.
	 * Requires: Ability to peruse the filesystem at "/sys/devices/system/cpu"
	 *
	 * @return The number of cores, or 1 if failed to get result
	 */
	public int getNumCores() {
		//Private Class to display only CPU devices in the directory listing
		class CpuFilter implements FileFilter {

			@Override
			public boolean accept(File pathname) {
				//Check if filename is "cpu", followed by a single digit number
				if (Pattern.matches("cpu[0-9]", pathname.getName())) {
					return true;
				}
				return false;
			}
		}

		try {
			//Get directory containing CPU info
			File dir = new File("/sys/devices/system/cpu/");
			//Filter to only list the devices we care about
			File[] files = dir.listFiles(new CpuFilter());
			//Return the number of cores (virtual CPU devices)
			return files.length;
		} catch (Exception e) {
			//Default to return 1 core
			return 1;
		}
	}

	/**
	 *
	 * @return
	 */
	public String getDeviecID() {
		TelephonyManager tm = (TelephonyManager) mActivity.getSystemService(Context.TELEPHONY_SERVICE);
		// get IMEI
		String imei = tm.getDeviceId();
		Log.v(TAG, "IMEI: " + imei);
		String phone = tm.getLine1Number();
		Log.v(TAG, "phone: " + phone);
		return imei;
	}

	/**
	 *
	 * @return
	 */
	public String getModel() {
		String model = android.os.Build.MODEL;
		Log.v(TAG, "Model: " + model);
		return model;
	}

	public static StaticProfile readProfile(File f) {
		ObjectInputStream ois = null;
		try {
			ois = new ObjectInputStream(new FileInputStream(f));

			Object obj = ois.readObject();

			if (obj instanceof StaticProfile) {
				return (StaticProfile) obj;
			} else {
				return null;
			}

		} catch (StreamCorruptedException ex) {
			Logger.getLogger(StaticProfiler.class.getName()).log(Level.SEVERE, null, ex);
		} catch (FileNotFoundException ex) {
			Log.e(TAG, "Profile file " + f + " not found!");
		} catch (IOException ex) {
			Logger.getLogger(StaticProfiler.class.getName()).log(Level.SEVERE, null, ex);
		} catch (ClassNotFoundException ex) {
			Logger.getLogger(StaticProfiler.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			if (ois != null) {
				try {
					ois.close();
				} catch (IOException ex) {
					Logger.getLogger(StaticProfiler.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		}

		return null;
	}

	/**
	 * Writes StaticProfile to file
	 *
	 * @param f Store destination
	 * @param p Profile to store
	 */
	public static void writeProfile(File f, StaticProfile p) {
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(new FileOutputStream(f));
			oos.writeObject(p);
		} catch (FileNotFoundException ex) {
			Logger.getLogger(StaticProfiler.class.getName()).log(Level.SEVERE, null, ex);
		} catch (IOException ex) {
			Logger.getLogger(StaticProfiler.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			if (oos != null) {
				try {
					oos.close();
				} catch (IOException ex) {
					Logger.getLogger(StaticProfiler.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		}
	}
}
