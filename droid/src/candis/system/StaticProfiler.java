package candis.system;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import candis.client.CertAcceptDialog;
import candis.client.R;
import candis.distributed.droid.StaticProfile;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * Not to be run in GUI thread!
 *
 * @author Enrico Joerns
 */
public class StaticProfiler {

	private static final String TAG = "Profiler";
	private final Activity activity;
	private final StaticProfile profile = null;
	private final ActivityManager activityManager;
	private final Handler handler;
	private final AtomicBoolean accepted = new AtomicBoolean(false);

	public StaticProfiler(final Activity act, final Handler handler) {
		this.activity = act;
		this.handler = handler;
		activityManager = (ActivityManager) activity.getSystemService(Activity.ACTIVITY_SERVICE);
	}

	public void profile() {
	}

	public void benchmark() {
		// get number of running processes / services
		handler.post(new Runnable() {
			public void run() {
				KillProcessesDialogFragment kpdf = new KillProcessesDialogFragment();
				kpdf.show(activity.getFragmentManager(), TAG);
			}
		});

		Log.i(TAG, "Waiting for accept");
		synchronized (accepted) {
			try {
				accepted.wait();
			} catch (InterruptedException ex) {
				Logger.getLogger(StaticProfiler.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		Log.i(TAG, "Received accept: " + accepted);

	}

	private void killBackgroundProcess() {
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

	private long getMemorySize() {
		MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
		activityManager.getMemoryInfo(memoryInfo);
		Log.i(TAG, "Memory: " + memoryInfo.availMem);

		return memoryInfo.availMem;
	}

	private class KillProcessesDialogFragment extends DialogFragment {

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
}
