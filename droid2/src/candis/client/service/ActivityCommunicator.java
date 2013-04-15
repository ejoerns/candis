package candis.client.service;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import candis.client.ClientFSM;
import candis.client.activity.CandisNotification;
import candis.client.comm.ReloadableX509TrustManager;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
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
  private boolean mIsBound = false;
  private NotificationManager mNM;
  /// Holds all messages not yet sent because activity is not bound.
  List<Message> mPendingMessages = new LinkedList<Message>();

  public ActivityCommunicator(Context context) {
    mContext = context;
    mNM = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
  }

  public IBinder getBinder() {
    mIsBound = true;
    return mSelfMessenger.getBinder();
  }

  public boolean doUnbind() {
    mIsBound = false;
    return false;
  }
  private ReloadableX509TrustManager mTrusmanager;

  public void setFSM(ClientFSM fsm) {
    mClientFSM = fsm;
  }

  public void OnCheckServerCert(X509Certificate cert, ReloadableX509TrustManager tm) {
    // send certificate to activity
    Message msg = Message.obtain(null, CHECK_SERVERCERT);
    Bundle certData = new Bundle();
    certData.putSerializable("cert", cert);
    msg.setData(certData);
    // if bound, send instantly, otherwise send notification and add to queue
    if (mIsBound) {
      try {
        mRemoteMessenger.send(msg);
      }
      catch (RemoteException ex) {
        Logger.getLogger(BackgroundService.class
                .getName()).log(Level.SEVERE, null, ex);
      }
    }
    else {
      Log.e(TAG, "*** Bad... i am not bound so i will save this message for later use...");
      mPendingMessages.add(msg);
      mNM.notify(
              CandisNotification.NOTIFICATION_ID,
              CandisNotification.getNotification(mContext, "Certificate authentification required"));

    }
    mTrusmanager = tm;
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
    if (mIsBound) {
      try {
        mRemoteMessenger.send(message);
      }
      catch (RemoteException ex) {
        Logger.getLogger(ActivityCommunicator.class
                .getName()).log(Level.SEVERE, null, ex);
      }
    }
    else {
      mPendingMessages.add(message);
      mNM.notify(
              CandisNotification.NOTIFICATION_ID,
              CandisNotification.getNotification(mContext, "Checkcode required"));
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
          for (Message pmsg : mPendingMessages) {
            try {
              mRemoteMessenger.send(pmsg);
            }
            catch (RemoteException ex) {
              Logger.getLogger(ActivityCommunicator.class.getName()).log(Level.SEVERE, null, ex);
            }
          }
          Log.e(TAG, "*** received MSG_REGISTER_CLIENT");
          Log.v(TAG, "reply to: " + mRemoteMessenger);
          break;
        // unregister client (Activity)
        case MSG_UNREGISTER_CLIENT:
          Log.e(TAG, "*** received MSG_UNREGISTER_CLIENT");
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
