package candis.client;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.os.Parcel;
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
import candis.common.CandisLog.CandisLogLevel;
import candis.common.Settings;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class MainActivity extends FragmentActivity
        implements OnClickListener {

  private static final String TAG = MainActivity.class.getName();
  private Button mServiceButton;
  private Button mInfoButton;
  private Button mOptionsButton;
  private Button mLogButton;
  ///
  private InitTask mInitTask;
  private DroidContext mDroidContext;
  private Handler mHandler;
  private boolean mServiceRunning = false;

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
//    inflater.inflate(R.menu.settings, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    Intent newintent;
//    switch (item.getItemId()) {
//      case android.R.id.home:
//        return true;
//      case R.id.menu_info:
//        newintent = new Intent(this, InfoActivity.class);
//        newintent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        startActivity(newintent);
//        return true;
//      case R.id.menu_settings:
//        newintent = new Intent(this, SettingsActivity.class);
//        newintent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        startActivity(newintent);
//        return true;
//      default:
    return super.onOptionsItemSelected(item);
//    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    CandisLog.level(CandisLogLevel.DEBUG);
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

    // init buttons
    mServiceButton = (Button) findViewById(R.id.service_button);
    mInfoButton = (Button) findViewById(R.id.info_button);
    mOptionsButton = (Button) findViewById(R.id.settings_button);
    mLogButton = (Button) findViewById(R.id.log_button);
    // add handlers
    mServiceButton.setOnClickListener(this);
    mInfoButton.setOnClickListener(this);
    mOptionsButton.setOnClickListener(this);
    mLogButton.setOnClickListener(this);


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


    setDefault(Notification.DEFAULT_LIGHTS);

    mDroidContext = DroidContext.getInstance();
    // Init droid
    mInitTask = new InitTask(
            this,
            new File(this.getFilesDir(), Settings.getString("idstore")),
            new File(this.getFilesDir(), Settings.getString("profilestore")));
    mInitTask.execute();
  }
  private NotificationManager mNotificationManager;
  private static int MOOD_NOTIFICATIONS = 12341234;
  Notification notification;

  private void setDefault(int defaults) {

    // This method sets the defaults on the notification before posting it.

    // This is who should be launched if the user selects our notification.
    PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                                                            new Intent(this, MainActivity.class), 0);

    // In this sample, we'll use the same text for the ticker and the expanded notification
    CharSequence text = "Warum bin ich so fröhlich?";

    notification = new Notification(
            R.drawable.ic_launcher, // the icon for the status bar
            text, // the text to display in the ticker
            System.currentTimeMillis()); // the timestamp for the notification

    notification.setLatestEventInfo(
            this, // the context to use
            "Fröhlicher Titel",
            // the title for the notification
            text, // the details to display in the notification
            contentIntent);              // the contentIntent (see above)

    notification.defaults = defaults;

    mNotificationManager.notify(
            MOOD_NOTIFICATIONS, // we use a string id because it is a unique
            // number.  we use it later to cancel the notification
            notification);
  }

  @Override
  public void onNewIntent(Intent intent) {
    System.out.println("onNewIntent() " + intent.getAction());
    if (intent.getAction() == null) {
      mNotificationManager.notify(
              MOOD_NOTIFICATIONS, // we use a string id because it is a unique
              // number.  we use it later to cancel the notification
              notification);      // do nothing
    }
    else if (intent.getAction().equals(BackgroundService.CHECK_SERVERCERT)) {
//      X509Certificate cert = (X509Certificate) intent.getSerializableExtra("X509Certificate");
//      CertAcceptDialog cad = new CertAcceptDialog(cert, mMessenger);
//      cad.show(getSupportFragmentManager(), "");
    }
    else if (intent.getAction().equals(BackgroundService.SHOW_CHECKCODE)) {
//      DialogFragment checkDialog = new CheckcodeInputDialog(this);
//      checkDialog.show(getSupportFragmentManager(), TAG);
    }
    else if (intent.getAction().equals(BackgroundService.JOB_CENTER_HANDLER)) {
//      mLogView.append(intent.getStringExtra("Message").concat("\n"));
    }
  }

  /**
   *
   * @param v
   */
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
        doBindService();
        mServiceButton.setText(getResources().getString(R.string.service_button_stop));
        ((TextView) findViewById(R.id.servicetext)).setText(R.string.service_text_started);
        ((TextView) findViewById(R.id.servicetext)).setTextColor(Color.rgb(0, 255, 0));
      }
    }
    else if (v == mInfoButton) {
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
   * Messenger for communicating with service.
   */
  Messenger mServiceMessenger = null;
  /**
   * Flag indicating whether we have called bind on the service.
   */
  boolean mIsBound;

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
          DialogFragment checkDialog = new CheckcodeInputDialog(mServiceMessenger);
          checkDialog.show(getSupportFragmentManager(), TAG);
          break;
        default:
          super.handleMessage(msg);
      }
    }
  }
  /**
   * Target we publish for clients to send messages to IncomingHandler.
   */
  final Messenger mSelfMessenger = new Messenger(new IncomingHandler());
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
      Log.e(TAG, "Attached.");

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

  void doBindService() {
    // Establish a connection with the service.  We use an explicit
    // class name because there is no reason to be able to let other
    // applications replace our component.
    bindService(new Intent(MainActivity.this,
                           BackgroundService.class), mConnection, Context.BIND_AUTO_CREATE);
    mIsBound = true;
//    mCallbackText.setText("Binding.");
    Log.e(TAG, "BONDAGE!");
  }

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
        }
      }

      // Detach our existing connection.
      unbindService(mConnection);
      mIsBound = false;
      Log.e(TAG, "Unbinding.");
    }
  }
}
