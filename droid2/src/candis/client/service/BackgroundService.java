package candis.client.service;

import candis.client.ClientFSM;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import candis.client.DeviceProfiler;
import candis.client.DroidContext;
import candis.client.R;
import candis.client.activity.CandisNotification;
import candis.client.comm.ReloadableX509TrustManager;
import candis.client.comm.ServerConnection;
import candis.client.comm.ServerConnection.Status;
import candis.common.DroidID;
import candis.common.Message;
import candis.common.Settings;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Background service.
 *
 * @author Enrico Joerns
 */
public class BackgroundService extends Service {

  private static String TAG = BackgroundService.class.getName();
  private boolean mRunning = false;
  private SystemStatusReceiver mSystemStatusController;
  private SharedPreferences mSharedPref;
  private ServerConnection mConnection;
  private ActivityCommunicator mActivityCommunicator;
  private StatusUpdater mStatusUpdater;

  /**
   * When binding to the service, we return an interface to our messenger
   * for sending messages to the service.
   */
  @Override
  public IBinder onBind(Intent intent) {
    Log.v(TAG, "onBind()");
    return mActivityCommunicator.getBinder();
  }

  @Override
  public boolean onUnbind(Intent intent) {
    Log.v(TAG, "onUnbind()");
    return mActivityCommunicator.doUnbind();
  }

  @Override
  public void onCreate() {
    super.onCreate();

    mStatusUpdater = new StatusUpdater(getApplicationContext());

    // Load settings from .properties
    Settings.load(this.getResources().openRawResource(R.raw.settings));

    try {
      DroidContext.getInstance().setID(DroidID.readFromFile(
              new File(this.getFilesDir(), Settings.getString("idstore"))));
      DroidContext.getInstance().setProfile(DeviceProfiler.readProfile(
              new File(this.getFilesDir(), Settings.getString("profilestore"))));
    }
    catch (FileNotFoundException ex) {
      mStatusUpdater.notify("Failed to load profile data");
    }


    // loader shared preferences
    mSharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

    // start this process as a foreground service so that it will not be
    // killed, even if it does cpu intensive operations etc.
    startForeground(
            CandisNotification.NOTIFICATION_ID,
            CandisNotification.getNotification(this, "Running..."));

    mActivityCommunicator = new ActivityCommunicator(this);

    init();

    // register receiver for battery and wifi status updates
    mSystemStatusController = new SystemStatusReceiver();
    mSystemStatusController.addListener(new SystemStatusReceiver.Listener() {
      public void OnSystemStatusUpdate() {
        if (SystemStatusReceiver.network_connected) {
          Log.e(TAG, "CONNECT IT DUDE!");
          mConnection.connect();
        }
        else {
          Log.e(TAG, "DISCONNECT IT DUDE!");
          mConnection.disconnect();
        }
      }
    });
    registerReceiver(
            mSystemStatusController,
            new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    registerReceiver(
            mSystemStatusController,
            new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    registerReceiver(
            mSystemStatusController,
            new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.v(TAG, "onStartCommand(" + intent.getAction() + ")");
    super.onStartCommand(intent, flags, startId);

    // if service running, only handle intent
    if (mRunning) {
      return START_STICKY;
    }
    mRunning = true;

    // We want this service to continue running until it is explicitly
    // stopped, so return sticky.
    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    Log.v(TAG, "onDestroy()");
    super.onDestroy();
    unregisterReceiver(mSystemStatusController);
    mConnection.disconnect();
  }

  public void init() {
    // Init trustmanager and connection
    ReloadableX509TrustManager trustmanager;
    try {
      // init trustmanager
      trustmanager = new ReloadableX509TrustManager(
              new File(getApplicationContext().getFilesDir(),
                       Settings.getString("truststore")));
      trustmanager.setCertAcceptHandler(mActivityCommunicator);
      // init connection
      mConnection = new ServerConnection(mSharedPref.getString("pref_key_servername", "not found"),
                                         Integer.valueOf(mSharedPref.getString("pref_key_serverport", "0")),
                                         trustmanager);
      mConnection.addReceiver(mStatusUpdater);
      // init state machine
      final ClientFSM mStateMachine = new ClientFSM(getApplicationContext(), mConnection, mActivityCommunicator);
      mStateMachine.init();
      // fsm must receive messages
      mConnection.addReceiver(mStateMachine);
      // fsm must receive activity messages
      mActivityCommunicator.setFSM(mStateMachine);

      // we want some status updates about execution of tasks
      mStateMachine.getJobCenter().addHandler(mStatusUpdater);

      /* finally add handler to register at master automatically at incoming
       * CONNECTED event.*/
      mConnection.addReceiver(new ServerConnection.Receiver() {
        public void OnNewMessage(Message msg) {
        }

        public void OnStatusUpdate(Status status) {
          if (status == Status.CONNECTED) {
            System.out.println("FUCK WE got it");
            mStateMachine.process(ClientFSM.Transitions.REGISTER);
          }
        }
      });
    }
    catch (Exception ex) {
      Logger.getLogger(BackgroundService.class.getName()).log(Level.SEVERE, null, ex);
    }
    // 
  }
}
