package candis.client.service;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import candis.client.CandisApp;
import candis.client.R;
import candis.client.activity.CandisNotification;
import candis.client.comm.ReloadableX509TrustManager;
import candis.client.comm.ServerConnection;
import candis.common.Settings;
import java.io.File;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.X509TrustManager;

/**
 * Background service.
 *
 * @author Enrico Joerns
 */
public class BackgroundService extends Service implements ReloadableX509TrustManager.Handler {

  private static String TAG = BackgroundService.class.getName();
  private boolean mRunning = false;
  private SystemStatusController mSystemStatusController;
  private SharedPreferences mSharedPref;
  private ServerConnection mConnection;

  @Override
  public IBinder onBind(Intent intent) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void onCreate() {
    super.onCreate();

    // Load settings from .properties
    Settings.load(this.getResources().openRawResource(R.raw.settings));
    // loader shared preferences
    mSharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

    // start this process as a foreground service so that it will not be
    // killed, even if it does cpu intensive operations etc.
    startForeground(
            CandisNotification.NOTIFICATION_ID,
            CandisNotification.getNotification(this, "Running..."));

    init();

    // register receiver for battery and wifi status updates
    mSystemStatusController = new SystemStatusController();
    mSystemStatusController.addListener(new SystemStatusController.Listener() {
      public void OnSystemStatusUpdate(boolean match) {
        if (match) {
          mConnection.connect();
        }
        else {
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
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.v(TAG, "onStartCommand()");
    super.onStartCommand(intent, flags, startId);

    Log.v("BAAAH", "value is: " + PreferenceManager.getDefaultSharedPreferences(CandisApp.getAppContext()).getString("pref_key_servername", "not found"));
//    CandisApp.getAppContext().getSharedPreferences(CandisApp.getAppContext().getPackageName() + "_preferences",
//                                                   MODE_MULTI_PROCESS);

    // if service running, only handle intent
    if (mRunning) {
      return START_STICKY;
    }
    mRunning = true;


    // We want this service to continue running until it is explicitly
    // stopped, so return sticky.
    return START_STICKY;
  }

  public void init() {
    X509TrustManager trustmanager;
    try {
      trustmanager = new ReloadableX509TrustManager(
              new File(getApplicationContext().getFilesDir(),
                       Settings.getString("truststore")), this);
      mConnection = new ServerConnection(mSharedPref.getString("pref_key_servername", "not found"),
                                         Integer.valueOf(mSharedPref.getString("pref_key_serverport", "0")),
                                         trustmanager);
      mConnection.start();
    }
    catch (Exception ex) {
      Logger.getLogger(BackgroundService.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  @Override
  public void OnCheckServerCert(X509Certificate cert) {
    throw new UnsupportedOperationException("Not supported yet.");
  }
}
