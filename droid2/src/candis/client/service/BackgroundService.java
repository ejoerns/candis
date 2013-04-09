package candis.client.service;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import candis.client.CandisApp;
import candis.client.activity.CandisNotification;

/**
 * Background service.
 *
 * @author Enrico Joerns
 */
public class BackgroundService extends Service {

  private static String TAG = BackgroundService.class.getName();
  private boolean mRunning = false;
  private SharedPreferences mSharedPref;

  @Override
  public IBinder onBind(Intent intent) {
    throw new UnsupportedOperationException("Not supported yet.");
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

    // loader shared preferences
    mSharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

    // start this process as a foreground service so that it will not be
    // killed, even if it does cpu intensive operations etc.
    startForeground(
            CandisNotification.NOTIFICATION_ID,
            CandisNotification.getNotification(this, "Running..."));

    // We want this service to continue running until it is explicitly
    // stopped, so return sticky.
    return START_STICKY;
  }
}
