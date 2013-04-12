package candis.client.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.widget.Toast;
import candis.client.service.ActivityCommunicator;
import candis.client.service.BackgroundService;
import java.security.cert.X509Certificate;

/**
 * Does *all* communication with Service.
 *
 * @author Enrico Joerns
 */
public class ServiceCommunicator {

  private static final String TAG = ServiceCommunicator.class.getName();
  boolean mIsBound;
  Messenger mServiceMessenger = null;
  final Messenger mSelfMessenger = new Messenger(new IncomingHandler());
  private final Context mContext;
  private final FragmentManager mFragManager;

  public ServiceCommunicator(Context context, FragmentManager fm) {
    mContext = context;
    mFragManager = fm;
  }

  /**
   *
   */
  public void doBindService() {
    // Establish a connection with the service.  We use an explicit
    // class name because there is no reason to be able to let other
    // applications replace our component.
    mContext.bindService(new Intent(mContext, BackgroundService.class),
                         mConnection,
                         Context.BIND_AUTO_CREATE);
    mIsBound = true;
  }

  /**
   *
   */
  public void doUnbindService() {
    if (mIsBound) {
      // If we have received the service, and hence registered with
      // it, then now is the time to unregister.
      if (mServiceMessenger != null) {
        try {
          Message msg = Message.obtain(null,
                                       ActivityCommunicator.MSG_UNREGISTER_CLIENT);
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
      mContext.unbindService(mConnection);
      mIsBound = false;
      Log.i(TAG, "Unbinding.");
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
        Message msg = Message.obtain(null, ActivityCommunicator.MSG_REGISTER_CLIENT);
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
      Toast.makeText(mContext, "R.string.remote_service_connected",
                     Toast.LENGTH_SHORT).show();
    }

    public void onServiceDisconnected(ComponentName className) {
      // This is called when the connection with the service has been
      // unexpectedly disconnected -- that is, its process crashed.
      mServiceMessenger = null;
      Log.e(TAG, "Disconnected.");

      // As part of the sample, tell the user what happened.
      Toast.makeText(mContext, "R.string.remote_service_disconnected",
                     Toast.LENGTH_SHORT).show();
    }
  };

  /**
   * Handler of incoming messages from service.
   */
  class IncomingHandler extends Handler {

    @Override
    public void handleMessage(Message msg) {
      Log.e("IncomingHandler", "--> Got message: " + msg.toString());
      switch (msg.what) {
        // show certificate if new one is posted
        case ActivityCommunicator.CHECK_SERVERCERT:
          Bundle myBundle = msg.getData();
          X509Certificate cert = (X509Certificate) myBundle.getSerializable("cert");
          CertAcceptDialog cad = new CertAcceptDialog(cert, mServiceMessenger);
          cad.show(mFragManager, "");
          break;
        case ActivityCommunicator.SHOW_CHECKCODE:
          String yourID = msg.getData().getString("ID");
          DialogFragment checkDialog = new CheckcodeInputDialog(mServiceMessenger, yourID);
          checkDialog.show(mFragManager, TAG);
          break;
//        case BackgroundService.INVALID_CHECKCODE:
//          Toast.makeText(getApplicationContext(), "The entered checkcode was invalid", Toast.LENGTH_LONG).show();
//          mConnectionState.setText("Invalid checkcode entered");
//          mConnectionState.setTextColor(Color.rgb(255, 0, 0));
//          break;
//        case BackgroundService.CONNECTING:
//          mConnectionState.setText("Connecting...");
//          mConnectionState.setTextColor(Color.rgb(255, 255, 0));
//          break;
//        case BackgroundService.CONNECTED:
//          mConnectionState.setText("Connected");
//          mConnectionState.setTextColor(Color.rgb(0, 255, 0));
//          break;
//        case BackgroundService.CONNECT_FAILED:
//          mConnectionState.setText("Connection failed");
//          mConnectionState.setTextColor(Color.rgb(255, 0, 0));
//          break;
//        case BackgroundService.DISCONNECTED:
//          mConnectionState.setText("Disconnected");
//          mConnectionState.setTextColor(Color.rgb(170, 170, 170));
//          break;
        default:
          super.handleMessage(msg);
      }
    }
  }
}
