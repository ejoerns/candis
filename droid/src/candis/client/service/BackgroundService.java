package candis.client.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import candis.client.ClientStateMachine;
import candis.client.CurrentSystemStatus;
import candis.client.DroidContext;
import candis.client.JobCenter;
import candis.client.MainActivity;
import candis.client.R;
import candis.client.comm.CertAcceptRequestHandler;
import candis.client.comm.ReloadableX509TrustManager;
import candis.client.comm.ServerConnection;
import candis.client.gui.settings.SettingsActivity;
import candis.common.DroidID;
import candis.common.Settings;
import candis.common.fsm.FSM;
import candis.common.fsm.StateMachineException;
import candis.system.StaticProfiler;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLHandshakeException;

/**
 * Background service that manages connection to master, receives tasks,
 * calculates and sends back results.
 *
 * @author Enrico Joerns
 */
public class BackgroundService extends Service implements CertAcceptRequestHandler {

  private static final String TAG = BackgroundService.class.getName();
  // Intent Actions
  ///
  public static final int MSG_REGISTER_CLIENT = 1;
  ///
  public static final int MSG_UNREGISTER_CLIENT = 2;
  ///
  public static final int MSG_SET_VALUE = 3;
  ///
  public static final int CHECK_SERVERCERT = 10;
  ///
  public static final int RESULT_CHECK_SERVERCERT = 20;
  ///
  public static final int SHOW_CHECKCODE = 30;
  ///
  public static final int RESULT_SHOW_CHECKCODE = 40;
  /// User entered invalid checkcode
  public static final int INVALID_CHECKCODE = 45;
  ///
  public static final int LOG_MESSAGE = 50;
  /// Indicates that connection to server is in progress
  public static final int CONNECTING = 100;
  /// Indicates that connection to server succeeded
  public static final int CONNECTED = 105;
  /// Indicates that connection to server failed
  public static final int CONNECT_FAILED = 110;
  /// Indicates thate connection was closed
  public static final int DISCONNECTED = 115;
  //---
  public static final int NOTIFICATION_ID = 4711;
  /// For showing and hiding our notification.
  private NotificationManager mNM;
  /// Remote target to send messsages to.
  private Messenger mRemoteMessenger;
  /// Target we publish for clients to send messages to IncomingHandler.
  final Messenger mSelfMessenger = new Messenger(new IncomingHandler());
  private final DroidContext mDroidContext;
  private final AtomicBoolean mCertCheckResult = new AtomicBoolean(false);
  /// Used to avoid double-call of service initialization
  private boolean mRunning = false;
  private FSM mFSM;
  private PowerConnectionReceiver mPowerConnectionReceiver;
  private SharedPreferences mSharedPref;
  Thread mConnectThread;
  ServerConnection sconn;

  public BackgroundService() {
    mDroidContext = DroidContext.getInstance();
  }

  /**
   * When binding to the service, we return an interface to our messenger
   * for sending messages to the service.
   */
  @Override
  public IBinder onBind(Intent intent) {
    Log.v(TAG, "onBind()");
    return mSelfMessenger.getBinder();
  }

  @Override
  public void onCreate() {
    Log.v(TAG, "onCreate()");
    super.onCreate();

    mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    Settings.load(this.getResources().openRawResource(R.raw.settings));
    mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
    // Setup DroidContext
    try {
      mDroidContext.setID(DroidID.readFromFile(
              new File(this.getFilesDir(), Settings.getString("idstore"))));
      mDroidContext.setProfile(StaticProfiler.readProfile(
              new File(this.getFilesDir(), Settings.getString("profilestore"))));
    }
    catch (FileNotFoundException ex) {
      mNM.notify(NOTIFICATION_ID, getNotification(this, "Failed to load profile data"));
    }
    // register receiver for battery updates
    mPowerConnectionReceiver = new PowerConnectionReceiver(this);// TODO...
    registerReceiver(
            mPowerConnectionReceiver,
            new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.v(TAG, "onStartCommand()");
    super.onStartCommand(intent, flags, startId);

    // if service running, only handle intent
    if (mRunning) {
      return START_STICKY;
    }
    mRunning = true;

    // start this process as a foreground service so that it will not be
    // killed, even if it does cpu intensive operations etc.
    startForeground(NOTIFICATION_ID, getNotification(this, "Started..."));

    // We want this service to continue running until it is explicitly
    // stopped, so return sticky.
    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    Log.v(TAG, "onDestroy()");
    super.onDestroy();

    // first inform master about termination
    if (mFSM != null) {
      try {
        mFSM.process(ClientStateMachine.ClientTrans.DISCONNECT);
      }
      catch (StateMachineException ex) {
        Log.w(TAG, "FSM failed to processs DISCONNECT Transition");
      }
    }

    // unregister registered receiver
    unregisterReceiver(mPowerConnectionReceiver);

    // stop running threads
    mConnectThread.interrupt();
    new Thread(new Runnable() {
      public void run() {
        if (sconn.getSocket() != null) {
          try {
            sconn.getSocket().close();
          }
          catch (IOException ex) {
            Log.e(TAG, null, ex);
          }
        }
      }
    }).start();

    // wait for stopped threads to finish
    try {
      mConnectThread.join();
    }
    catch (InterruptedException ex) {
      Logger.getLogger(BackgroundService.class.getName()).log(Level.SEVERE, null, ex);
    }

    // Cancel notification
//    mNM.cancel(NOTIFICATION_ID);
    // Tell the user we stopped.
    Toast.makeText(
            this, R.string.remote_service_stopped, Toast.LENGTH_SHORT).show();
    mRunning = false;
  }
//----------------------------------------------------------------------------

  /**
   * Show a notification while this service is running.
   */
  public static Notification getNotification(Context context, CharSequence text) {

    // Set the icon, scrolling text and timestamp
    Notification notification = new Notification(R.drawable.candis_logo, text,
                                                 System.currentTimeMillis());

    // The PendingIntent to launch our activity if the user selects this notification
    PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                                                            new Intent(context, MainActivity.class), 0);

    // Set the info for the views that show in the notification panel.
    notification.setLatestEventInfo(
            context, "Candis client",
            text, contentIntent);

    // return the generated notification.
    return notification;
  }

  /**
   *
   */
  private class ConnectRunnable implements Runnable {

    public void run() {
      sconn = null;

      // init job center
      final JobCenter jobcenter = new JobCenter(getApplicationContext());
      jobcenter.addHandler(new ActivityLogger(mRemoteMessenger, getApplicationContext()));
      // init connection
      ReloadableX509TrustManager tm = null;
      try {
        tm = new ReloadableX509TrustManager(new File(
                getApplicationContext().getFilesDir(),
                Settings.getString("truststore")));
      }
      catch (Exception ex) {
        Log.wtf(TAG, null, ex);
        return;
      }
      tm.setCertAcceptDialog(BackgroundService.this);
      try {
        sconn = new ServerConnection(
                tm,
                mDroidContext,
                getApplicationContext(),
                mRemoteMessenger,
                jobcenter);
        mFSM = sconn.getFSM();
        jobcenter.setFSM(mFSM);
        sconn.connect(mSharedPref.getString(SettingsActivity.HOSTNAME, "not found"),
                      Integer.valueOf(mSharedPref.getString(SettingsActivity.PORTNAME, "0")));
        // Notify about connecting...
        mNM.notify(NOTIFICATION_ID, getNotification(BackgroundService.this, getText(R.string.status_connecting)));
        try {
          mRemoteMessenger.send(Message.obtain(null, CONNECTING));
        }
        catch (RemoteException ex1) {
          Log.e(TAG, null, ex1);
        }
      }
      catch (ConnectException ex) {
        mNM.notify(NOTIFICATION_ID, getNotification(BackgroundService.this, getText(R.string.status_connection_failed)));
        Log.e(TAG, ex.getMessage());
        if (mRemoteMessenger == null) {
          return;
        }
        try {
          mRemoteMessenger.send(Message.obtain(null, CONNECT_FAILED));
        }
        catch (RemoteException ex1) {
          Log.w(TAG, "Failed sending Notifiaton message 'CONNECT_FAILED'");
        }
        return;
      }
      // SSL handshake failed (maybe user rejected certificate)
      catch (SSLHandshakeException ex) {
        Log.e(TAG, ex.getMessage());
        mNM.notify(NOTIFICATION_ID, getNotification(BackgroundService.this, getText(R.string.status_connection_failed)));
        if (mRemoteMessenger == null) {
          return;
        }
        try {
          mRemoteMessenger.send(Message.obtain(null, CONNECT_FAILED));
        }
        catch (RemoteException ex1) {
          Log.w(TAG, "Failed sending Notifiaton message 'CONNECT_FAILED'");
        }
        // TODO: kill process?
        return;
      }
      catch (Exception ex) {
        Log.wtf(TAG, null, ex);
      }

      // Start worker thread
      sconn.run();// TODO: catch SocketException?
    }
  }

  @Override
  public boolean userCheckAccept(X509Certificate cert) {
    try {
      Message msg = Message.obtain(null, CHECK_SERVERCERT);
      Bundle certData = new Bundle();
      certData.putSerializable("cert", cert);
      msg.setData(certData);
      mRemoteMessenger.send(msg);
    }
    catch (RemoteException ex) {
      Logger.getLogger(BackgroundService.class
              .getName()).log(Level.SEVERE, null, ex);
    }
    synchronized (mCertCheckResult) {
      try {
        mCertCheckResult.wait();
      }
      catch (InterruptedException ex) {
        Log.e(TAG, ex.toString());
      }
    }
    return mCertCheckResult.get();
  }

  /**
   * Handler of incoming messages from clients.
   */
  class IncomingHandler extends Handler {

    @Override
    public void handleMessage(Message msg) {
      Log.v("IncomingHandler", "<-- Got message: " + msg.toString());

      switch (msg.what) {
        // register client (Activity)
        case MSG_REGISTER_CLIENT:
          mRemoteMessenger = msg.replyTo;
          Log.v(TAG, "reply to: " + mRemoteMessenger);
          mConnectThread = new Thread(new ConnectRunnable());
          mConnectThread.setDaemon(true);
          mConnectThread.start();
          break;
        // unregister client (Activity)
        case MSG_UNREGISTER_CLIENT:
          mRemoteMessenger = null;
          break;
        // show check code
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
        // result of server certificate check
        case RESULT_CHECK_SERVERCERT:
          synchronized (mCertCheckResult) {
            mCertCheckResult.set(msg.arg1 == 1 ? true : false);// TODO...
            mCertCheckResult.notifyAll();
          }
          break;
        // 
        default:
          super.handleMessage(msg);
      }
    }
  }
}
