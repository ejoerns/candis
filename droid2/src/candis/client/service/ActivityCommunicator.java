package candis.client.service;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import candis.client.ClientFSM;
import candis.client.comm.ReloadableX509TrustManager;
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles communication with activity.
 *
 * @author Enrico Joerns
 */
public class ActivityCommunicator implements ReloadableX509TrustManager.Handler {

  private static final String TAG = ActivityCommunicator.class.getName();
  ///
  public static final int MSG_REGISTER_CLIENT = 1;
  ///
  public static final int MSG_UNREGISTER_CLIENT = 2;
  ///
  public static final int MSG_SET_VALUE = 3;
  ///
  public static final int CHECK_SERVERCERT = 10;
  ///
  public static final int RESULT_CHECK_SERVERCERT = 20;
  ///
  public static final int SHOW_CHECKCODE = 30;
  ///
  public static final int RESULT_SHOW_CHECKCODE = 40;
  /// User entered invalid checkcode
  public static final int INVALID_CHECKCODE = 45;
  ///
  public static final int LOG_MESSAGE = 50;
  /// Indicates that connection to server is in progress
  public static final int CONNECTING = 100;
  /// Indicates that connection to server succeeded
  public static final int CONNECTED = 105;
  /// Indicates that connection to server failed
  public static final int CONNECT_FAILED = 110;
  /// Indicates thate connection was closed
  public static final int DISCONNECTED = 115;
  private Messenger mRemoteMessenger;
  final Messenger mSelfMessenger = new Messenger(new IncomingHandler());
  private final Context mContext;
  private ClientFSM mClientFSM;

  public ActivityCommunicator(Context context) {
    mContext = context;
  }

  public IBinder getBinder() {
    return mSelfMessenger.getBinder();
  }
  private ReloadableX509TrustManager mTrusmanager;

  public void setFSM(ClientFSM fsm) {
    mClientFSM = fsm;
  }

  public void OnCheckServerCert(X509Certificate cert, ReloadableX509TrustManager tm) {
    try {
      // send certificate to activity
      Message msg = Message.obtain(null, CHECK_SERVERCERT);
      Bundle certData = new Bundle();
      certData.putSerializable("cert", cert);
      msg.setData(certData);
      mRemoteMessenger.send(msg);
      mTrusmanager = tm;
    }
    catch (RemoteException ex) {
      Logger.getLogger(BackgroundService.class
              .getName()).log(Level.SEVERE, null, ex);
    }
  }

  /**
   * Send Message to service.
   *
   * @param msg
   */
  public void displayCheckcode(String code) {
    Bundle bundle = new Bundle();
    bundle.putString("ID", code);
    android.os.Message message = android.os.Message.obtain(null, ActivityCommunicator.SHOW_CHECKCODE);
    message.setData(bundle);
    try {
      mRemoteMessenger.send(message);
    }
    catch (RemoteException ex) {
      Logger.getLogger(ActivityCommunicator.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  /**
   * Handler of incoming messages from clients.
   */
  class IncomingHandler extends Handler {

    @Override
    public void handleMessage(Message msg) {
      Log.v("IncomingHandler", "<-- Got message: " + msg.toString());

      switch (msg.what) {
        // register client (Activity)
        case MSG_REGISTER_CLIENT:
          mRemoteMessenger = msg.replyTo;
          Log.v(TAG, "reply to: " + mRemoteMessenger);
          break;
        // unregister client (Activity)
        case MSG_UNREGISTER_CLIENT:
          mRemoteMessenger = null;
          break;
        case RESULT_CHECK_SERVERCERT:
          mTrusmanager.acceptCertificate(msg.arg1 == 1 ? true : false);
          break;
        // show check code
        case RESULT_SHOW_CHECKCODE:
          mClientFSM.process(
                  ClientFSM.Transitions.CHECKCODE_ENTERED,
//                  BackgroundService.this.mDroidContext.getID().toSHA1(),
                  msg.getData().getString("checkcode"));
          break;
//        // result of server certificate check
        default:
          super.handleMessage(msg);
      }
    }
  }
}
