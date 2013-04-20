package candis.client.activity;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import candis.client.DroidContext;
import candis.client.R;
import candis.client.service.BackgroundService;
import candis.common.Settings;
import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainActivity extends FragmentActivity implements SharedPreferences.OnSharedPreferenceChangeListener, PreferenceFragment.OnPreferenceAttachedListener {

  private static final int EDIT_ID = Menu.FIRST + 2;
  private static final String TAG = MainActivity.class.getName();
  private SharedPreferences mSharedPref;
  private ServiceCommunicator mServiceCommunicator;
  private boolean mServiceRunning = false;
  private SharedPreferences.OnSharedPreferenceChangeListener mPrefListener;

  public static class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      // Load the preferences from an XML resource
      addPreferencesFromResource(R.xml.preferences);
      addPreferencesFromResource(R.xml.preferences2);

      Preference preference;
      preference = getPreferenceManager().findPreference("pref_key_droid_id");
      preference.setSummary(DroidContext.getInstance().getID().toSHA1());
      // information for device profile screen
      preference = getPreferenceManager().findPreference("pref_key_device_name");
      preference.setSummary(String.valueOf(DroidContext.getInstance().getProfile().model));
      preference = getPreferenceManager().findPreference("pref_key_device_id");
      preference.setSummary(String.valueOf(DroidContext.getInstance().getProfile().id));
      preference = getPreferenceManager().findPreference("pref_key_cpu_count");
      preference.setSummary(String.valueOf(DroidContext.getInstance().getProfile().processors));
      preference = getPreferenceManager().findPreference("pref_key_memory");
      preference.setSummary(String.format("%d MB", DroidContext.getInstance().getProfile().memoryMB));
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
            .registerOnSharedPreferenceChangeListener(mPrefListener);
  }

  @Override
  public void onPause() {
    super.onPause();

    PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
            .unregisterOnSharedPreferenceChangeListener(mPrefListener);
  }

  /**
   * Called when the activity is first created.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mPrefListener = new EditPreferencesListener(this);
    getSupportFragmentManager().beginTransaction()
            .replace(android.R.id.content, new SettingsFragment())
            .commit();

    // Load settings from R.raw.settings
    Settings.load(this.getResources().openRawResource(R.raw.settings));

    // loader shared preferences
    mSharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

    mServiceCommunicator = new ServiceCommunicator(this, getSupportFragmentManager());

    // TODO: check for initial call
//    PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences, false);
//    PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.preferences2, false);
    mSharedPref.registerOnSharedPreferenceChangeListener(this);

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
    // start/stop service
    if (key.equals("pref_key_run_service")) {
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
  }

  public void onPreferenceAttached(PreferenceScreen root, int xmlId) {
  }
}
