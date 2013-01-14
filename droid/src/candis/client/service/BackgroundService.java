package candis.client.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import candis.client.ClientStateMachine;
import candis.client.DroidContext;
import candis.client.JobCenter;
import candis.client.JobCenterHandler;
import candis.client.R;
import candis.client.comm.CommRequestBroker;
import candis.client.comm.SecureConnection;
import candis.common.ClassLoaderWrapper;
import candis.common.Settings;
import candis.common.fsm.FSM;
import candis.common.fsm.StateMachineException;
import java.io.File;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

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
	public static final String SHOW_CHECKCODE = "SHOW_CHECKCODE";
	public static final String RESULT_SHOW_CHECKCODE = "RESULT_SHOW_CHECKCODE";
	public static final String JOB_CENTER_HANDLER = "JOB_CENTER_HANDLER";
	private Context mContext;
	private final DroidContext mDroidContext;
	private final AtomicBoolean mCertCheckResult = new AtomicBoolean(false);
	/// Used to avoid double-call of service initialization
	private boolean mRunning = false;
	private FSM fsm;
  private ClassLoaderWrapper mClassloader;

	public BackgroundService() {
		mDroidContext = DroidContext.getInstance();
	}

	@Override
	public IBinder onBind(Intent intent) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void onCreate() {
		mContext = getApplicationContext();
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

		Log.i("LocalService", "Received start id " + startId + ": " + intent);
		mRunning = true;

		mDroidContext.init((DroidContext) intent.getExtras().getSerializable("DROID_CONTEXT"));

		final ConnectTask connectTask;
		connectTask = new ConnectTask(
						mCertCheckResult,
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

		new Thread(new Runnable() {
			public void run() {
				final CommRequestBroker crb;
				SecureConnection secureConn = null;

				// wait for connection to finish
				try {
					secureConn = connectTask.get();
          if (secureConn == null) {
            return;
          }
				}
				catch (InterruptedException ex) {
					Logger.getLogger(BackgroundService.class.getName()).log(Level.SEVERE, null, ex);
				}
				catch (ExecutionException ex) {
					Logger.getLogger(BackgroundService.class.getName()).log(Level.SEVERE, null, ex);
				}

				// init fsm
				try {

					mClassloader = new ClassLoaderWrapper();// init empty

					JobCenterHandler jobCenterHandler = new ActivityLogger(mContext);
					final JobCenter jobcenter = new JobCenter(mContext, mClassloader);
					jobcenter.addHandler(jobCenterHandler);
					fsm = new ClientStateMachine(
									secureConn,
									mDroidContext,
									mContext,
									null,
									jobcenter); // TODO: check handler usageIOException
					crb = new CommRequestBroker(
									secureConn.getInputStream(),
									fsm,
									mClassloader);
					new Thread(crb).start();
					System.out.println("[THREAD DONE]");
				}
				catch (StreamCorruptedException ex) {
					Log.e(TAG, ex.toString());
				}
				catch (IOException ex) {
					Log.e(TAG, ex.toString());
				}

			}
		}).start();

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
		else if (intent.getAction().equals(RESULT_SHOW_CHECKCODE)) {
			System.out.println("RESULT_SHOW_CHECKCODE");
			try {
				fsm.process(
								ClientStateMachine.ClientTrans.CHECKCODE_ENTERED,
								intent.getStringExtra("RESULT"));
			}
			catch (StateMachineException ex) {
				Log.e(TAG, ex.toString());
			}
		}
		else {
			Log.w(TAG, "Unknown Intent");
		}
	}

	@Override
	public void onDestroy() {
		try {
      if (fsm != null) {
        fsm.process(ClientStateMachine.ClientTrans.DISCONNECT);
      }
		}
		catch (StateMachineException ex) {
			Logger.getLogger(BackgroundService.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
