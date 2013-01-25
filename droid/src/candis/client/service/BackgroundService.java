package candis.client.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.widget.Toast;
import candis.client.ClientStateMachine;
import candis.client.DroidContext;
import candis.client.JobCenter;
import candis.client.JobCenterHandler;
import candis.client.MainActivity;
import candis.client.R;
import candis.client.comm.SecureConnection;
import candis.client.comm.ServerConnection;
import candis.common.ClassLoaderWrapper;
import candis.common.Settings;
import candis.common.fsm.FSM;
import candis.common.fsm.StateMachineException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
  /**
   *
   */
  public static final int MSG_REGISTER_CLIENT = 1;
  /**
   *
   */
  public static final int MSG_UNREGISTER_CLIENT = 2;
  /**
   *
   */
  public static final int MSG_SET_VALUE = 3;
  /**
   *
   */
  public static final int CHECK_SERVERCERT = 10;
  /**
   *
   */
  public static final int RESULT_CHECK_SERVERCERT = 20;
  /**
   *
   */
  public static final int SHOW_CHECKCODE = 30;
  /**
   *
   */
  public static final int RESULT_SHOW_CHECKCODE = 40;
  /**
   *
   */
  public static final int JOB_CENTER_HANDLER = 50;
  /**
   * For showing and hiding our notification.
   */
  NotificationManager mNM;
  Messenger mRemoteMessenger;
  private Context mContext;
  private final DroidContext mDroidContext;
  private final AtomicBoolean mCertCheckResult = new AtomicBoolean(false);
  /// Used to avoid double-call of service initialization
  private boolean mRunning = false;
  private FSM mFSM;
  private ClassLoaderWrapper mClassloaderWrapper;

  public BackgroundService() {
    mDroidContext = DroidContext.getInstance();
  }

  /**
   * When binding to the service, we return an interface to our messenger
   * for sending messages to the service.
   */
  @Override
  public IBinder onBind(Intent intent) {
    Log.e("BackgroundService", "onBind()");
    return mLocalMessenger.getBinder();
  }

  @Override
  public void onCreate() {
    mContext = getApplicationContext();
    System.out.println("Backgroundservice: onCreate()");
    Settings.load(this.getResources().openRawResource(R.raw.settings));
    mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    // register receiver for battery updates
    registerReceiver(
            new PowerConnectionReceiver(null),
            new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    showNotification();
  }

  /**
   * Show a notification while this service is running.
   */
  private void showNotification() {
    // In this sample, we'll use the same text for the ticker and the expanded notification
    CharSequence text = getText(R.string.remote_service_started);

    // Set the icon, scrolling text and timestamp
    Notification notification = new Notification(R.drawable.ic_launcher, text,
                                                 System.currentTimeMillis());

    // The PendingIntent to launch our activity if the user selects this notification
    PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                                                            new Intent(this, MainActivity.class), 0);

    // Set the info for the views that show in the notification panel.
    notification.setLatestEventInfo(this, "Yeah, Service started...",
                                    text, contentIntent);

    // Send the notification.
    // We use a string id because it is a unique number.  We use it later to cancel.
    mNM.notify(R.string.remote_service_started, notification);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    System.out.println("Backgroundservice: onStartCommand()");
    super.onStartCommand(intent, flags, startId);

    // if service running, only handle intent
    if (mRunning) {
      onNewIntent(intent);
      return START_STICKY;
    }

    Log.i("LocalService", "Received start id " + startId + ": " + intent);
    mRunning = true;

    mDroidContext.init((DroidContext) intent.getExtras().getSerializable("DROID_CONTEXT"));

    // We want this service to continue running until it is explicitly
    // stopped, so return sticky.
    return START_STICKY;
  }

  public void doConnect() {
    final ConnectTask connectTask;
    connectTask = new ConnectTask(
            mCertCheckResult,
            getApplicationContext(),
            new File(getApplicationContext().getFilesDir(), Settings.getString("truststore")),
            mRemoteMessenger);

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
        ServerConnection crb = null;
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

        mClassloaderWrapper = new ClassLoaderWrapper();// init empty

        JobCenterHandler jobCenterHandler = new ActivityLogger(mRemoteMessenger);
        final JobCenter jobcenter = new JobCenter(mContext, mClassloaderWrapper);
        jobcenter.addHandler(jobCenterHandler);
        try {
          crb = new ServerConnection(
                  secureConn.getSocket(),
                  mClassloaderWrapper,
                  mDroidContext,
                  mContext,
                  mRemoteMessenger,
                  null,
                  jobcenter);
          mFSM = crb.getFSM(); // TODO: check where to place the fsm!
        }
        catch (IOException ex) {
          Logger.getLogger(BackgroundService.class.getName()).log(Level.SEVERE, null, ex);
        }

        new Thread(crb).start();
        System.out.println("[THREAD DONE]");

      }
    }).start();

  }

  // NOTE: not an override!
  public void onNewIntent(Intent intent) {
    System.out.println("onNewIntent() " + intent.getAction());
    if (intent.getAction() == null) {
      Log.w(TAG, "Intent Action is null");
    }
    else if (intent.getAction().equals(RESULT_CHECK_SERVERCERT)) {
      synchronized (mCertCheckResult) {
        mCertCheckResult.set(intent.getBooleanExtra("RESULT", false));// TODO...
        mCertCheckResult.notifyAll();
        System.out.println("cert_check_result.notifyAll()");
      }
    }
    else if (intent.getAction().equals(RESULT_SHOW_CHECKCODE)) {
      System.out.println("RESULT_SHOW_CHECKCODE");
//      try {
//        mFSM.process(
//                ClientStateMachine.ClientTrans.CHECKCODE_ENTERED,
//                intent.getStringExtra("candis.client.RESULT"));
//      }
//      catch (StateMachineException ex) {
//        Log.e(TAG, ex.toString());
//      }
    }
    else {
      Log.w(TAG, "Unknown Intent");
    }
  }

  @Override
  public void onDestroy() {
    mNM.cancel(R.string.remote_service_started);
    // Tell the user we stopped.
    Toast.makeText(this, R.string.remote_service_stopped, Toast.LENGTH_SHORT).show();
    try {
      if (mFSM != null) {
        mFSM.process(ClientStateMachine.ClientTrans.DISCONNECT);
      }
    }
    catch (StateMachineException ex) {
      Logger.getLogger(BackgroundService.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  /**
   * Handler of incoming messages from clients.
   */
  class IncomingHandler extends Handler {

    @Override
    public void handleMessage(Message msg) {
      Log.i("IncomingHandler", "<-- Got message: " + msg.toString());

      switch (msg.what) {
        case MSG_REGISTER_CLIENT:
          mRemoteMessenger = msg.replyTo;
          Log.e(TAG, "reply to: " + mRemoteMessenger);
          doConnect();
          break;
        case MSG_UNREGISTER_CLIENT:
          mRemoteMessenger = null;
          break;
        case CHECK_SERVERCERT:
          break;
        case RESULT_SHOW_CHECKCODE:
          try {
            mFSM.process(
                    ClientStateMachine.ClientTrans.CHECKCODE_ENTERED,
                    msg.getData().getString("checkcode"));
          }
          catch (StateMachineException ex) {
            Log.e(TAG, ex.toString());
          }

          break;
        case RESULT_CHECK_SERVERCERT:
          synchronized (mCertCheckResult) {
            mCertCheckResult.set(msg.arg1 == 1 ? true : false);// TODO...
            mCertCheckResult.notifyAll();
            System.out.println("cert_check_result.notifyAll()");
          }
          break;
        default:
          super.handleMessage(msg);
      }
    }
  }
  /**
   * Target we publish for clients to send messages to IncomingHandler.
   */
  final Messenger mLocalMessenger = new Messenger(new IncomingHandler());
}
