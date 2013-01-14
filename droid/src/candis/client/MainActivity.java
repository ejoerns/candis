package candis.client;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import candis.client.gui.CertAcceptDialog;
import candis.client.gui.CheckcodeInputDialog;
import candis.client.gui.InfoActivity;
import candis.client.gui.settings.SettingsActivity;
import candis.client.service.BackgroundService;
import candis.common.CandisLog;
import candis.common.Settings;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class MainActivity extends Activity
        implements OnClickListener {

  private static final String TAG = MainActivity.class.getName();
  private Button mServiceButton;
  private TextView mLogView;
  private InitTask mInitTask;
  private DroidContext mDroidContext;
  private Handler mHandler;
  private boolean mServiceRunning = false;

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.settings, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    Intent newintent;
    switch (item.getItemId()) {
      case android.R.id.home:
        return true;
      case R.id.menu_info:
        newintent = new Intent(this, InfoActivity.class);
        newintent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(newintent);
        return true;
      case R.id.menu_settings:
        newintent = new Intent(this, SettingsActivity.class);
        newintent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(newintent);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    CandisLog.level(CandisLog.DEBUG);
    System.out.println("onCreate()");

    // Check if saved bundle can be found...
    if (savedInstanceState == null) {
      Log.i(TAG, "No savedInstanceState found!");
    }
    else {
      Log.i(TAG, "Found savedInstanceState!");
    }

    mHandler = new Handler();

    // Load settings from R.raw.settings
    Settings.load(this.getResources().openRawResource(R.raw.settings));
    // load Shared Preferences
    PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

    // load logger.properties from /res/raw/logger.properties
    // Must be loaded in order to get default logging levels below INFO working 
    final InputStream inputStream = getResources().openRawResource(R.raw.logger);
    try {
      LogManager.getLogManager().readConfiguration(inputStream);
    }
    catch (final IOException e) {
      Logger.getAnonymousLogger().severe("Could not load default logging.properties file");
      Logger.getAnonymousLogger().severe(e.getMessage());
    }

    // Check for background service
    mServiceRunning = isBackgroundServiceRunning();

    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    // init button
    mServiceButton = (Button) findViewById(R.id.service_button);
    mServiceButton.setOnClickListener(this);
    if (mServiceRunning) {
      mServiceButton.setText(getResources().getString(R.string.service_button_stop));
      ((TextView) findViewById(R.id.servicetext)).setText(R.string.service_text_started);
      ((TextView) findViewById(R.id.servicetext)).setTextColor(Color.rgb(0, 255, 0));
    }
    else {
      mServiceButton.setText(getResources().getString(R.string.service_button_start));
      ((TextView) findViewById(R.id.servicetext)).setText(R.string.service_text_stopped);
      ((TextView) findViewById(R.id.servicetext)).setTextColor(Color.rgb(255, 0, 0));
    }

    // init text view
    mLogView = (TextView) findViewById(R.id.logtext);
    mLogView.setMovementMethod(new ScrollingMovementMethod());

    mDroidContext = DroidContext.getInstance();
    // Init droid
    mInitTask = new InitTask(
            this,
            new File(this.getFilesDir(), Settings.getString("idstore")),
            new File(this.getFilesDir(), Settings.getString("profilestore")));
    mInitTask.execute();
  }

  @Override
  public void onNewIntent(Intent intent) {
    System.out.println("onNewIntent() " + intent.getAction());
    if (intent.getAction().equals(BackgroundService.CHECK_SERVERCERT)) {
      X509Certificate cert = (X509Certificate) intent.getSerializableExtra("X509Certificate");
      CertAcceptDialog cad = new CertAcceptDialog(cert, this);
      cad.show(getFragmentManager(), "");
    }
    else if (intent.getAction().equals(BackgroundService.SHOW_CHECKCODE)) {
      DialogFragment checkDialog = new CheckcodeInputDialog(this);
      checkDialog.show(getFragmentManager(), TAG);
    }
    else if (intent.getAction().equals(BackgroundService.JOB_CENTER_HANDLER)) {
      mLogView.append(intent.getStringExtra("Message").concat("\n"));
    }
  }

  public void onClick(View v) {

    if (v == mServiceButton) {
      if (mServiceRunning) {
        mServiceRunning = false; // TODO: replace by real test?
        Log.d(TAG, "onClick: stopping service");
        stopService(new Intent(this, BackgroundService.class));
        mServiceButton.setText(getResources().getString(R.string.service_button_start));
        ((TextView) findViewById(R.id.servicetext)).setText(R.string.service_text_stopped);
        ((TextView) findViewById(R.id.servicetext)).setTextColor(Color.rgb(255, 0, 0));
      }
      else {
        mServiceRunning = true; // TODO: replace by real test?
        Log.d(TAG, "onClick: starting service");
        startService(new Intent(this, BackgroundService.class).putExtra("DROID_CONTEXT", mDroidContext));
        mServiceButton.setText(getResources().getString(R.string.service_button_stop));
        ((TextView) findViewById(R.id.servicetext)).setText(R.string.service_text_started);
        ((TextView) findViewById(R.id.servicetext)).setTextColor(Color.rgb(0, 255, 0));
      }
    }
  }

  /**
   * Tests if the background service ist running.
   *
   * @return true if it is running, false otherwise
   */
  private boolean isBackgroundServiceRunning() {
    ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
      if (BackgroundService.class.getName().equals(service.service.getClassName())) {
        return true;
      }
    }
    return false;
  }
}
