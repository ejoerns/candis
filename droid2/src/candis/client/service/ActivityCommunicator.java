package candis.client.service;

import android.content.Context;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import candis.common.fsm.StateMachineException;

/**
 * Handles communication with activity.
 *
 * @author Enrico Joerns
 */
public class ActivityCommunicator {

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

  public ActivityCommunicator(Context context) {
    mContext = context;
  }

  public IBinder getBinder() {
    return mSelfMessenger.getBinder();
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
        // show check code
//        case RESULT_SHOW_CHECKCODE:
//          try {
//            mFSM.process(
//                    ClientStateMachine.ClientTrans.CHECKCODE_ENTERED,
//                    BackgroundService.this.mDroidContext.getID().toSHA1(),
//                    msg.getData().getString("checkcode"));
//          }
//          catch (StateMachineException ex) {
//            Log.e(TAG, ex.toString());
//          }
//          break;
//        // result of server certificate check
//        case RESULT_CHECK_SERVERCERT:
//          synchronized (mCertCheckResult) {
//            mCertCheckResult.set(msg.arg1 == 1 ? true : false);// TODO...
//            mCertCheckResult.notifyAll();
//          }
//          break;
        default:
          super.handleMessage(msg);
      }
    }
  }
}
