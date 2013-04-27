package candis.client.android.activity;

import android.app.ActivityManager;
import android.content.Context;
import static android.content.Context.MODE_MULTI_PROCESS;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.EditTextPreference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import candis.client.R;
import candis.client.android.service.ActivityCommunicator;
import candis.client.android.service.BackgroundService;
import candis.common.Settings;
import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainActivity extends FragmentActivity implements PreferenceFragment.OnPreferenceAttachedListener, SharedPreferences.OnSharedPreferenceChangeListener {

  private static final int EDIT_ID = Menu.FIRST + 2;
  private static final String TAG = MainActivity.class.getName();
  private SharedPreferences mSharedPref;
  private ServiceCommunicator mServiceCommunicator;
  private boolean mServiceRunning = false;
  private SettingsFragment mSettingsFragment;

  @Override
  public void onResume() {
    super.onResume();
    PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
            .registerOnSharedPreferenceChangeListener(this);

  }

  @Override
  public void onPause() {
    super.onPause();

    PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
            .unregisterOnSharedPreferenceChangeListener(this);
  }

  /**
   * Called when the activity is first created.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mServiceCommunicator = new ServiceCommunicator(this, getSupportFragmentManager());

    mSettingsFragment = SettingsFragment.newInstance();

    getSupportFragmentManager().beginTransaction()
            .replace(android.R.id.content, mSettingsFragment)
            .commit();

    // Load settings from R.raw.settings
    Settings.load(this.getResources().openRawResource(R.raw.settings));

    // loader shared preferences
//    mSharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    mSharedPref = getApplicationContext().getSharedPreferences(
            getApplicationContext().getPackageName() + "_preferences",
            MODE_MULTI_PROCESS);

    // TODO: check for initial call
//    PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences, false);
//    PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences2, false);
//    mSharedPref.registerOnSharedPreferenceChangeListener(this);

    /* Does some potential initialization and interaction with the user
     * needed to start the service etc.
     */
    InitTask initTask = new InitTask(
            this,
            new File(this.getFilesDir(), Settings.getString("idstore")),
            new File(this.getFilesDir(), Settings.getString("profilestore")),
            new Handler());
    initTask.execute();
    try {
      initTask.get();
    }
    catch (InterruptedException ex) {
      Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
    }
    catch (ExecutionException ex) {
      Logger.getLogger(MainActivity.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  @Override
  public void onStart() {
    super.onStart();
    System.out.println("*** onStart() was called");
    // start service and bind if enabled
    if (mSharedPref.getBoolean("pref_key_run_service", false)) {
      Log.i("foo", "Starting service..");
      startService(new Intent(this, BackgroundService.class));
      // Check for background service and bind if running
      mServiceRunning = isBackgroundServiceRunning();
      if (mServiceRunning) {
        mServiceCommunicator.doBindService();
      }
    }
  }

  @Override
  public void onStop() {
    super.onStop();
    System.out.println("*** onStop() was called");
    if (mServiceRunning) {
      System.out.println("UNBINDING FROM SERVICE at onDestroy");
      mServiceCommunicator.doUnbindService();
    }
  }

  /**
   * Tests if the background service ist running.
   *
   * @return true if it is running, false otherwise
   */
  private boolean isBackgroundServiceRunning() {
    ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
    for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
      if (BackgroundService.class.getName().equals(service.service.getClassName())) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    System.out.println("***MainActivity.onSharedPreferenceChanged");
    // server settings changed
    if (key.equals("pref_key_servername") || (key.equals("pref_key_serverport"))) {
      EditTextPreference preference = (EditTextPreference) mSettingsFragment.getPreferenceManager().findPreference(key);
      preference.setSummary(preference.getText());
      // restart service if activated
      if (sharedPreferences.getBoolean("pref_key_run_service", false)) {
        mServiceCommunicator.doUnbindService();
        stopService(new Intent(getApplicationContext(), BackgroundService.class));
        startService(new Intent(getApplicationContext(), BackgroundService.class));
        mServiceCommunicator.doBindService();
      }
    }
    // service activation status changed
    else if (key.equals("pref_key_run_service")) {
      if (sharedPreferences.getBoolean("pref_key_run_service", false)) {
        Log.d(TAG, "startService()");
        startService(new Intent(getApplicationContext(), BackgroundService.class));
        mServiceCommunicator.doBindService();
      }
      else {
        Log.d(TAG, "stopService()");
        mServiceCommunicator.doUnbindService();
        stopService(new Intent(getApplicationContext(), BackgroundService.class));
      }
    }
    // enable/disable notification
    else if (key.equals("pref_key_notifications")) {
      System.out.println("pref_key_notifications");
      Message msg = Message.obtain(null, ActivityCommunicator.PREF_UPDATE_NOTIFITCATIONS);
      msg.arg1 = sharedPreferences.getBoolean("pref_key_notifications", true) ? 1 : 0;
      mServiceCommunicator.sendMessage(msg);
    }
    else if (key.equals("pref_key_multithread")) {
      System.out.println("pref_key_multithread");
      Message msg = Message.obtain(null, ActivityCommunicator.PREF_UPDATE_MULTICORE);
      msg.arg1 = sharedPreferences.getBoolean("pref_key_multithread", true) ? 1 : 0;
      mServiceCommunicator.sendMessage(msg);
    }
  }

  public void onPreferenceAttached(PreferenceScreen root, int xmlId) {
  }
}
