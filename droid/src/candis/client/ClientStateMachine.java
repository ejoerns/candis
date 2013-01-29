package candis.client;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Messenger;
import android.os.RemoteException;
import candis.client.comm.ServerConnection;
import candis.client.service.BackgroundService;
import candis.common.Instruction;
import candis.common.Message;
import candis.common.fsm.ActionHandler;
import candis.common.fsm.FSM;
import candis.common.fsm.StateEnum;
import candis.common.fsm.StateMachineException;
import candis.common.fsm.Transition;
import candis.distributed.DistributedJobParameter;
import candis.distributed.DistributedJobResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Enrico Joerns
 */
public final class ClientStateMachine extends FSM {

  private static final String TAG = ClientStateMachine.class.getName();
  private static final Logger LOGGER = Logger.getLogger(TAG);
  private final DroidContext mDroitContext;
  private final ServerConnection mSConn;
  private final Context mContext;
  private final Messenger mMessenger;
  private final NotificationManager mNotificationManager;
  private final JobCenter mJobCenter;
  private SharedPreferences.Editor mSystemStateEditor;

  private enum ClientStates implements StateEnum {

    UNCONNECTED,
    WAIT_ACCEPT,
    CHECKCODE_ENTER,
    CHECKCODE_SENT,
    PROFILE_SENT,
    WAIT_FOR_JOB,
    JOB_RECEIVED,
    JOB_BINARY_REQUESTED,
    JOB_INIT_REQUESTED,
    JOB_PROCESSING,
    INIT_RECEIVED,
    INIT_BINARY_REQUESTED;
  }

  public enum ClientTrans implements Transition {

    SOCKET_CONNECTED,
    CHECKCODE_ENTERED,
    JOB_FINISHED,
    DISCONNECT,
    KNOWN_TASK,
    UNKNOWN_TASK;
  }

  public ClientStateMachine(
          ServerConnection sconn,
          final DroidContext dcontext,
          final Context context,
          final Messenger messenger,
          final JobCenter jobcenter) {
    mDroitContext = dcontext;
    mContext = context;
    mMessenger = messenger;
    mSConn = sconn;
    mJobCenter = jobcenter;

    mNotificationManager =
            (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
    mSystemStateEditor = mContext.getSharedPreferences(
            CurrentSystemStatus.CURRENT_SYSTEM_STATUS,
            Context.MODE_PRIVATE).edit();

  }

  @Override
  public void init() {
    addState(ClientStates.UNCONNECTED)
            .addTransition(
            ClientTrans.SOCKET_CONNECTED,
            ClientStates.WAIT_ACCEPT,
            new SocketConnectedHandler());
    addState(ClientStates.WAIT_ACCEPT)
            .addTransition(
            Instruction.REQUEST_PROFILE,
            ClientStates.PROFILE_SENT,
            new ProfileRequestHandler())
            .addTransition(
            Instruction.ACCEPT_CONNECTION,
            ClientStates.WAIT_FOR_JOB,
            new ConnectionAcceptedHandler())
            .addTransition(
            Instruction.REQUEST_CHECKCODE,
            ClientStates.CHECKCODE_ENTER,
            new CheckcodeInputHandler())
            .addTransition(
            Instruction.ERROR,
            ClientStates.UNCONNECTED,
            new ActionHandler() {
              public void handle(Object... obj) {
                LOGGER.severe("An Error occured while connecting to the server");
              }
            });
    addState(ClientStates.CHECKCODE_ENTER)
            .addTransition(
            ClientTrans.CHECKCODE_ENTERED,
            ClientStates.CHECKCODE_SENT,
            new CheckcodeSendHandler());
    addState(ClientStates.CHECKCODE_SENT)
            .addTransition(
            Instruction.REQUEST_PROFILE,
            ClientStates.PROFILE_SENT,
            new ProfileRequestHandler())
            .addTransition(
            Instruction.INVALID_CHECKCODE,
            ClientStates.UNCONNECTED,
            new InvalidCheckcodeHandler());
    addState(ClientStates.PROFILE_SENT)
            .addTransition(
            Instruction.ACCEPT_CONNECTION,
            ClientStates.WAIT_FOR_JOB,
            new ConnectionAcceptedHandler());
    addState(ClientStates.WAIT_FOR_JOB)
            .addTransition(
            Instruction.SEND_JOB,
            ClientStates.JOB_RECEIVED,
            new CheckJobHandler())
            .addTransition(
            Instruction.SEND_INITIAL,
            ClientStates.INIT_RECEIVED,
            new CheckInitialParameterHandler());
    addState(ClientStates.JOB_RECEIVED)
            .addTransition(
            ClientTrans.KNOWN_TASK,
            ClientStates.JOB_PROCESSING,
            new ProcessJobHandler())
            .addTransition(
            ClientTrans.UNKNOWN_TASK,
            ClientStates.JOB_BINARY_REQUESTED,
            new RequestBinaryHandler());
    addState(ClientStates.JOB_BINARY_REQUESTED)
            .addTransition(
            Instruction.SEND_BINARY,
            ClientStates.JOB_INIT_REQUESTED,
            new AddBinaryHandler());
    addState(ClientStates.JOB_INIT_REQUESTED)
            .addTransition(
            Instruction.SEND_INITIAL,
            ClientStates.JOB_PROCESSING,
            new AddInitialAndProcessHandler());
    addState(ClientStates.INIT_RECEIVED)
            .addTransition(
            ClientTrans.KNOWN_TASK,
            ClientStates.WAIT_FOR_JOB,
            new AddInitialParameterHandler())
            .addTransition(
            ClientTrans.UNKNOWN_TASK,
            ClientStates.INIT_BINARY_REQUESTED,
            new RequestBinaryHandler());
    addState(ClientStates.INIT_BINARY_REQUESTED)
            .addTransition(
            Instruction.SEND_BINARY,
            ClientStates.WAIT_FOR_JOB,
            new AddBinaryHandler());
    addState(ClientStates.JOB_PROCESSING)
            .addTransition(
            ClientTrans.JOB_FINISHED,
            ClientStates.WAIT_FOR_JOB,
            new JobFinishedHandler());

    addGlobalTransition(
            ClientTrans.DISCONNECT,
            ClientStates.UNCONNECTED,
            new DisconnectHandler());
    addGlobalTransition(
            Instruction.REJECT_CONNECTION,
            ClientStates.UNCONNECTED,
            new ConnectionRejectedHandler());
    setState(ClientStates.UNCONNECTED);
  }

  /*--------------------------------------------------------------------------*/
  /**
   * Shows Error Dialog with short message.
   */
  private class ConnectionRejectedHandler extends ActionHandler {

    @Override
    public void handle(final Object... obj) {
      gotCalled();
//      Notification noti = new NotificationCompat.Builder(mContext)
//              //      Notification noti = new Notification.Builder(mContext)
//              .setContentTitle("Connection Rejected")
//              .setContentText("Server rejected connection")
//              .setSmallIcon(R.drawable.ic_launcher)
//              .setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_launcher))
//              .build();
//      mNotificationManager.notify(42, noti);
      try {
        mMessenger.send(android.os.Message.obtain(null, BackgroundService.CONNECT_FAILED));
      }
      catch (RemoteException ex) {
        Logger.getLogger(ClientStateMachine.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }

  /**
   * Sends disconnect instruction to master.
   */
  private class DisconnectHandler extends ActionHandler {

    @Override
    public void handle(final Object... o) {
      gotCalled();
      mSConn.sendMessage(Message.create(Instruction.DISCONNECT));
      try {
        mMessenger.send(android.os.Message.obtain(null, BackgroundService.DISCONNECTED));
      }
      catch (RemoteException ex) {
        Logger.getLogger(ClientStateMachine.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }

  /**
   * Requests connection to server.
   */
  private class SocketConnectedHandler extends ActionHandler {

    @Override
    public void handle(final Object... obj) {
      assert obj != null;

      gotCalled();
      if (mDroitContext.getID() == null) {
        LOGGER.severe("Will not request connection for an empty ID");
        return;
      }
      System.out.println("mDroitContext.getID():" + mDroitContext.getID());
      mSConn.sendMessage(Message.create(Instruction.REQUEST_CONNECTION, mDroitContext.getID()));
    }
  }

  /**
   * Sends Profile data to Server.
   */
  private class ProfileRequestHandler extends ActionHandler {

    @Override
    public void handle(final Object... obj) {
      gotCalled();
      mSConn.sendMessage(Message.create(Instruction.SEND_PROFILE, mDroitContext.getProfile()));
    }
  }

  /**
   * Shows input dialog to enter checkcode.
   */
  private class CheckcodeInputHandler extends ActionHandler {

    @Override
    public void handle(final Object... o) {
      gotCalled();
      try {
        //      Intent newintent = new Intent(mContext, MainActivity.class);
        //      newintent.setAction(BackgroundService.SHOW_CHECKCODE)
        //              .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        //              .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //      mContext.startActivity(newintent);
        mMessenger.send(android.os.Message.obtain(null, BackgroundService.SHOW_CHECKCODE));
      }
      catch (RemoteException ex) {
        Logger.getLogger(ClientStateMachine.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }

  /**
   * Sends entered checkcode to server.
   */
  private class CheckcodeSendHandler extends ActionHandler {

    @Override
    public void handle(final Object... o) {
      gotCalled();
      mSConn.sendMessage(Message.create(Instruction.SEND_CHECKCODE, (String) o[0]));
    }
  }

  /**
   * Sends entered checkcode to server.
   */
  private class InvalidCheckcodeHandler extends ActionHandler {

    @Override
    public void handle(final Object... o) {
      gotCalled();
      try {
        mMessenger.send(android.os.Message.obtain(null, BackgroundService.INVALID_CHECKCODE));
      }
      catch (RemoteException ex) {
        Logger.getLogger(ClientStateMachine.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }

  /**
   * Called when connection was accepted.
   * Notifies listener about this.
   */
  private class ConnectionAcceptedHandler extends ActionHandler {

    public void handle(Object... obj) {
      try {
        mSystemStateEditor
                .putString(CurrentSystemStatus.SERVER_NAME, mSConn.getSocket().getInetAddress().toString())
                .putInt(CurrentSystemStatus.SERVER_PORT, mSConn.getSocket().getPort())
                .commit();
        mMessenger.send(android.os.Message.obtain(null, BackgroundService.CONNECTED));
        mNotificationManager.notify(
                BackgroundService.NOTIFICATION_ID,
                BackgroundService.getNotification(mContext, mContext.getText(R.string.status_connected)));
      }
      catch (RemoteException ex) {
        Logger.getLogger(ClientStateMachine.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }
  /*--------------------------------------------------------------------------*/

  /**
   *
   */
  private class CheckJobHandler extends ActionHandler {

    public void handle(Object... obj) {
      assert obj[0] instanceof String;
      assert (obj[1] instanceof DistributedJobParameter) || (obj[1] instanceof byte[]);
      gotCalled();

      mJobCenter.setCurrentRunnableID((String) obj[0]);
      mJobCenter.setCurrentUndeserializedJob((byte[]) obj[1]);
      if (mJobCenter.isTaskAvailable((String) obj[0])) {
        LOGGER.info(String.format("Task for ID %s available in cache", (String) obj[0]));
        process(ClientTrans.KNOWN_TASK, obj[0], mJobCenter.deserializeCurrentJob());
      }
      else {
        LOGGER.info(String.format("Task for ID %s not found in cache", (String) obj[0]));
        process(ClientTrans.UNKNOWN_TASK, obj[0]);
      }
    }
  }

  /**
   *
   */
  private class JobFinishedHandler extends ActionHandler {

    @Override
    public void handle(final Object... obj) {
      assert obj[0] instanceof String;
      assert obj[1] instanceof DistributedJobResult;
      assert obj.length == 2;
			
      gotCalled();
      // Serialize Result
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos;
      try {
        oos = new ObjectOutputStream(baos);
        oos.writeObject(obj[1]);
        oos.close();
      }
      catch (IOException ex) {
        LOGGER.log(Level.SEVERE, null, ex);
      }
      byte[] bytes = baos.toByteArray();

      // Send result
      mSConn.sendMessage(Message.create(
              Instruction.SEND_RESULT,
              (String) obj[0],
              bytes));
    }
  }

  /**
   * Checks if a binary for this parameter is already available.
   *
   * param[0]: id (String)
   * param[1]: parameter
   */
  private class CheckInitialParameterHandler extends ActionHandler {

    public void handle(Object... obj) {
      try {
        if (mJobCenter.isTaskAvailable((String) obj[0])) {
          process(ClientTrans.KNOWN_TASK, obj[0], obj[1]);
        }
        else {
          process(ClientTrans.UNKNOWN_TASK, obj[0], obj[1]);
        }
      }
      catch (StateMachineException ex) {
        Logger.getLogger(ClientStateMachine.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }

  /**
   * Executes the job with given ID.
   */
  private class ProcessJobHandler extends ActionHandler {

    public void handle(Object... obj) {
      mSConn.sendMessage(Message.create(Instruction.ACK));
      mJobCenter.executeTask((String) obj[0], (DistributedJobParameter) obj[1]);
    }
  }

  /**
   *
   */
  private class AddBinaryHandler extends ActionHandler {

    public void handle(Object... obj) {

      mJobCenter.loadBinary((String) obj[0], (byte[]) obj[1]);

      mSConn.sendMessage(Message.create(Instruction.ACK));
    }
  }

  /*
   *
   */
  private class AddInitialParameterHandler extends ActionHandler {

    public void handle(Object... obj) {
      mJobCenter.setInitialParameter((String) obj[0], (DistributedJobParameter) obj[1]);
      mSConn.sendMessage(Message.create(Instruction.ACK));
    }
  }

  /**
   *
   */
  private class AddInitialAndProcessHandler extends ActionHandler {

    public void handle(Object... obj) {
      assert obj[0] instanceof String;
      assert obj[1] instanceof DistributedJobParameter;
      assert obj.length == 2;

      mJobCenter.setInitialParameter((String) obj[0], mJobCenter.deserializeJobParameter((byte[]) obj[1]));
      mSConn.sendMessage(Message.create(Instruction.ACK));
      DistributedJobParameter djp = mJobCenter.deserializeCurrentJob();
      mJobCenter.executeTask((String) obj[0], (DistributedJobParameter) djp);
    }
  }

  /**
   * Simply sends a request command.
   * param[0] - id (String)
   */
  private class RequestBinaryHandler extends ActionHandler {

    public void handle(Object... obj) {
      assert obj[0] instanceof String;

      mSConn.sendMessage(Message.create(Instruction.REQUEST_BINARY, (Serializable) obj[0]));
    }
  }
}
