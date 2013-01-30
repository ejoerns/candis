package candis.client;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import candis.client.gui.CertAcceptDialog;
import candis.client.gui.CheckcodeInputDialog;
import candis.client.gui.InfoActivity;
import candis.client.gui.LogActivity;
import candis.client.gui.settings.SettingsActivity;
import candis.client.service.BackgroundService;
import candis.common.CandisLog;
import candis.common.Settings;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class MainActivity extends FragmentActivity
        implements OnClickListener {

  private static final String TAG = MainActivity.class.getName();
  //-- GUI Elements
  private Button mServiceButton;
  private Button mInfoButton;
  private Button mOptionsButton;
  private Button mLogButton;
  private TextView mServiceState;
  private TextView mConnectionState;
  ///
  private InitTask mInitTask;
//  private DroidContext mDroidContext;
  private Handler mHandler;
  private boolean mServiceRunning = false;
  private NotificationManager mNotificationManager;
//  private static int MOOD_NOTIFICATIONS = 12341234;
  Notification notification;
  /// Target we publish for clients to send messages to IncomingHandler.
  final Messenger mSelfMessenger = new Messenger(new IncomingHandler());
  /// Messenger for communicating with service.
  Messenger mServiceMessenger = null;
  /// Flag indicating whether we have called bind on the service.
  boolean mIsBound;
  SharedPreferences mSharedPrefs;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    System.out.println("onCreate()");
    
    // Check if saved bundle can be found...
    if (savedInstanceState == null) {
      Log.i(TAG, "No savedInstanceState found!");
    }
    else {
      Log.i(TAG, "Found savedInstanceState!");
    }
    mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    mHandler = new Handler();

    // Load settings from R.raw.settings
    Settings.load(this.getResources().openRawResource(R.raw.settings));
    // load Shared Preferences
    PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
    mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

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

    // Check for background service and bind if running
    mServiceRunning = isBackgroundServiceRunning();
    if (mServiceRunning) {
      doBindService();
    }

    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    // init buttons
    mServiceButton = (Button) findViewById(R.id.service_button);
    mInfoButton = (Button) findViewById(R.id.info_button);
    mOptionsButton = (Button) findViewById(R.id.settings_button);
    mLogButton = (Button) findViewById(R.id.log_button);
    mServiceState = (TextView) findViewById(R.id.servicetext);
    mConnectionState = (TextView) findViewById(R.id.connectiontext);
    // add handlers
    mServiceButton.setOnClickListener(this);
    mInfoButton.setOnClickListener(this);
    mOptionsButton.setOnClickListener(this);
    mLogButton.setOnClickListener(this);

    mServiceButton.setEnabled(false);
    if (mServiceRunning) {
      mServiceButton.setText(getResources().getString(R.string.service_button_stop));
      mServiceState.setText(R.string.service_text_started);
      mServiceState.setTextColor(Color.rgb(0, 255, 0));
    }
    else {
      mServiceButton.setText(getResources().getString(R.string.service_button_start));
      mServiceState.setText(R.string.service_text_stopped);
      mServiceState.setTextColor(Color.rgb(255, 0, 0));
    }

    // Init droid
    mInitTask = new InitTask(
            this,
            new File(this.getFilesDir(), Settings.getString("idstore")),
            new File(this.getFilesDir(), Settings.getString("profilestore")),
            mHandler);
    mInitTask.execute();
    // Wait for InitTask to finish to enable Service button
    new Thread(new Runnable() {
      public void run() {
        try {
          mInitTask.get();
          mHandler.post(new Runnable() {
            public void run() {
              mServiceButton.setEnabled(true);
            }
          });
        }
        catch (InterruptedException ex) {
          Log.e(TAG, null, ex);
        }
        catch (ExecutionException ex) {
          Log.wtf(TAG, null, ex);
        }
      }
    }).start();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    Log.e(TAG, "onCreateOptionsMenu()");
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.settings, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    Intent newintent;
    switch (item.getItemId()) {
//      case android.R.id.home:
//        return true;
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
  public void onNewIntent(Intent intent) {
    Log.v(TAG, "onNewIntent() " + intent.getAction());
  }

  @Override
  public void onPause() {
    Log.d(TAG, "onPause()");
    super.onPause();
    doUnbindService();
  }

  @Override
  public void onResume() {
    super.onResume();
    Log.v("BUH", "value is: " + PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(SettingsActivity.HOSTNAME, "not found"));
  }

  /**
   *
   * @param v
   */
  public void onClick(View v) {

    if (v == mServiceButton) {
      if (mServiceRunning) {
        Log.d(TAG, "onClick: stopping service");
        doUnbindService();
        stopService(new Intent(this, BackgroundService.class));
      }
      else {
        Log.d(TAG, "onClick: starting service");
        startService(new Intent(this, BackgroundService.class));
        doBindService();
        sendSharedPreferences();
      }
      if (isBackgroundServiceRunning()) {
        mServiceRunning = true;
        mServiceButton.setText(getResources().getString(R.string.service_button_stop));
        mServiceState.setText(R.string.service_text_started);
        mServiceState.setTextColor(Color.rgb(0, 255, 0));
      }
      else {
        mServiceRunning = false;
        mServiceButton.setText(getResources().getString(R.string.service_button_start));
        mServiceState.setText(R.string.service_text_stopped);
        mServiceState.setTextColor(Color.rgb(255, 0, 0));
      }
    }
    else if (v == mInfoButton) {
//      Intent newintent = new Intent(this, JobViewActivity.class);
      Intent newintent = new Intent(this, InfoActivity.class);
      newintent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      startActivity(newintent);
    }
    else if (v == mOptionsButton) {
      Intent newintent = new Intent(this, SettingsActivity.class);
      newintent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      startActivity(newintent);
    }
    else if (v == mLogButton) {
      Intent newintent = new Intent(this, LogActivity.class);
      newintent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      startActivity(newintent);
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

  /**
   * Handler of incoming messages from service.
   */
  class IncomingHandler extends Handler {

    @Override
    public void handleMessage(Message msg) {
      Log.i("IncomingHandler", "--> Got message: " + msg.toString());
      switch (msg.what) {
        case BackgroundService.CHECK_SERVERCERT:
          Bundle myBundle = msg.getData();
          X509Certificate cert = (X509Certificate) myBundle.getSerializable("cert");
          CertAcceptDialog cad = new CertAcceptDialog(cert, mServiceMessenger);
          cad.show(getSupportFragmentManager(), "");
          break;
        case BackgroundService.SHOW_CHECKCODE:
          String yourID = msg.getData().getString("ID");
          DialogFragment checkDialog = new CheckcodeInputDialog(mServiceMessenger, yourID);
          checkDialog.show(getSupportFragmentManager(), TAG);
          break;
        case BackgroundService.INVALID_CHECKCODE:
          Toast.makeText(getApplicationContext(), "The entered checkcode was invalid", Toast.LENGTH_LONG).show();
          mConnectionState.setText("Invalid checkcode entered");
          mConnectionState.setTextColor(Color.rgb(255, 0, 0));
          break;
        case BackgroundService.CONNECTING:
          mConnectionState.setText("Connecting...");
          mConnectionState.setTextColor(Color.rgb(255, 255, 0));
          break;
        case BackgroundService.CONNECTED:
          mConnectionState.setText("Connected");
          mConnectionState.setTextColor(Color.rgb(0, 255, 0));
          break;
        case BackgroundService.CONNECT_FAILED:
          mConnectionState.setText("Connection failed");
          mConnectionState.setTextColor(Color.rgb(255, 0, 0));
          break;
        case BackgroundService.DISCONNECTED:
          mConnectionState.setText("Disconnected");
          mConnectionState.setTextColor(Color.rgb(170, 170, 170));
          break;
        default:
          super.handleMessage(msg);
      }
    }
  }
  /**
   * Class for interacting with the main interface of the service.
   */
  private ServiceConnection mConnection = new ServiceConnection() {
    public void onServiceConnected(ComponentName className,
                                   IBinder service) {
      // This is called when the connection with the service has been
      // established, giving us the service object we can use to
      // interact with the service.  We are communicating with our
      // service through an IDL interface, so get a client-side
      // representation of that from the raw service object.
      mServiceMessenger = new Messenger(service);
      Log.v(TAG, "Attached.");

      // We want to monitor the service for as long as we are
      // connected to it.
      try {
        Message msg = Message.obtain(null, BackgroundService.MSG_REGISTER_CLIENT);
        msg.replyTo = mSelfMessenger;
        mServiceMessenger.send(msg);
      }
      catch (RemoteException e) {
        // In this case the service has crashed before we could even
        // do anything with it; we can count on soon being
        // disconnected (and then reconnected if it can be restarted)
        // so there is no need to do anything here.
        Log.e(TAG, e.getMessage());
      }

      // As part of the sample, tell the user what happened.
      Toast.makeText(MainActivity.this, R.string.remote_service_connected,
                     Toast.LENGTH_SHORT).show();
    }

    public void onServiceDisconnected(ComponentName className) {
      // This is called when the connection with the service has been
      // unexpectedly disconnected -- that is, its process crashed.
      mServiceMessenger = null;
      Log.e(TAG, "Disconnected.");

      // As part of the sample, tell the user what happened.
      Toast.makeText(MainActivity.this, R.string.remote_service_disconnected,
                     Toast.LENGTH_SHORT).show();
    }
  };

  /**
   *
   */
  void doBindService() {
    // Establish a connection with the service.  We use an explicit
    // class name because there is no reason to be able to let other
    // applications replace our component.
    bindService(new Intent(MainActivity.this,
                           BackgroundService.class), mConnection, Context.BIND_AUTO_CREATE);
    mIsBound = true;
  }

  /**
   *
   */
  void doUnbindService() {
    if (mIsBound) {
      // If we have received the service, and hence registered with
      // it, then now is the time to unregister.
      if (mServiceMessenger != null) {
        try {
          Message msg = Message.obtain(null,
                                       BackgroundService.MSG_UNREGISTER_CLIENT);
          msg.replyTo = mSelfMessenger;
          mServiceMessenger.send(msg);
        }
        catch (RemoteException e) {
          // There is nothing special we need to do if the service
          // has crashed.
          Log.w(TAG, null, e);
        }
      }

      // Detach our existing connection.
      unbindService(mConnection);
      mIsBound = false;
      Log.i(TAG, "Unbinding.");
    }
  }

  void sendSharedPreferences() {

    Bundle bundle = new Bundle();

    for (Map.Entry<String, ?> pref : mSharedPrefs.getAll().entrySet()) {
      if (pref.getValue() instanceof String) {
        bundle.putString(pref.getKey(), (String) pref.getValue());
        Log.e(TAG, "putString: " + pref.getKey() + ", " + pref.getValue());
      }
      else if (pref.getValue() instanceof Integer) {
        bundle.putInt(pref.getKey(), (Integer) pref.getValue());
        Log.e(TAG, "putInteger: " + pref.getKey() + ", " + pref.getValue());
      }
      else {
        Log.e(TAG, "Unknown preference");
      }
    }
    // send
    Message msg = Message.obtain(null, BackgroundService.PREFERENCE_UPDATE);
    msg.setData(bundle);
    try {
      if (mServiceMessenger != null) {
        mServiceMessenger.send(msg);
      }
    }
    catch (RemoteException ex) {
      Logger.getLogger(SettingsActivity.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
}
