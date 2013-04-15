package candis.client.activity;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import candis.client.R;
import candis.client.service.BackgroundService;
import candis.common.Settings;
import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainActivity extends FragmentActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

  private static final int EDIT_ID = Menu.FIRST + 2;
  private static final String TAG = MainActivity.class.getName();
  private SharedPreferences mSharedPref;
  private ServiceCommunicator mServiceCommunicator;
  private boolean mServiceRunning = false;

  /**
   * Called when the activity is first created.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

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

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    menu.add(Menu.NONE, EDIT_ID, Menu.NONE, "Edit Prefs")
            .setIcon(R.drawable.action_settings)
            .setAlphabeticShortcut('e');

    return (super.onCreateOptionsMenu(menu));
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case EDIT_ID:
        // use fallback version of preference activity if OS is too old
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
          startActivity(new Intent(this, EditPreferences.class));
        }
        else {
          startActivity(new Intent(this, EditPreferencesHC.class));
        }

        return (true);
    }

    return (super.onOptionsItemSelected(item));
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

  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    System.out.println("***MainActivity.onSharedPreferenceChanged");
    if (key.equals("pref_key_run_service")) {
      if (sharedPreferences.getBoolean("pref_key_run_service", false)) {
        startService(new Intent(this, BackgroundService.class));
//        mServiceCommunicator.doBindService();        // start service
      }
      else {
        // stop service
//        mServiceCommunicator.doUnbindService();
        stopService(new Intent(this, BackgroundService.class));
      }
    }
  }
}
