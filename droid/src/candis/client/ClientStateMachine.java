package candis.client;

import android.app.FragmentManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import candis.client.comm.ServerConnection;
import candis.client.service.BackgroundService;
import candis.common.Instruction;
import candis.common.Message;
import candis.common.fsm.ActionHandler;
import candis.common.fsm.FSM;
import candis.common.fsm.HandlerID;
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

  private static final String TAG = "ClientStateMachine";
  private static final Logger LOGGER = Logger.getLogger(TAG);
  private final DroidContext mDroitContext;
  private final ServerConnection mSConn;
  private final Context mContext;
  private final NotificationManager mNotificationManager;
  private final JobCenter mJobCenter;

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
    KNOWN,
    UNKNOWN;
  }

  private enum ClientHandlerID implements HandlerID {

    MY_ID;
  }

  public ClientStateMachine(
          ServerConnection sconn,
          final DroidContext dcontext,
          final Context context,
          final FragmentManager fragmanager,
          final JobCenter jobcenter) {
    mDroitContext = dcontext;
    mContext = context;
    mSConn = sconn;
    mJobCenter = jobcenter;

    mNotificationManager =
            (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
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
            null)
            .addTransition(
            Instruction.REJECT_CONNECTION,
            ClientStates.UNCONNECTED,
            new ConnectionRejectedHandler())
            .addTransition(
            Instruction.REQUEST_CHECKCODE,
            ClientStates.CHECKCODE_ENTER,
            new CheckcodeInputHandler());
    addState(ClientStates.CHECKCODE_ENTER)
            .addTransition(
            ClientTrans.CHECKCODE_ENTERED,
            ClientStates.CHECKCODE_SENT,
            new CheckcodeSendHandler());
    addState(ClientStates.CHECKCODE_SENT)
            .addTransition(
            Instruction.REQUEST_PROFILE,
            ClientStates.PROFILE_SENT,
            new ProfileRequestHandler());
    addState(ClientStates.PROFILE_SENT)
            .addTransition(
            Instruction.ACCEPT_CONNECTION,
            ClientStates.WAIT_FOR_JOB,
            null);
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
            ClientTrans.KNOWN,
            ClientStates.JOB_PROCESSING,
            new ProcessJobHandler())
            .addTransition(
            ClientTrans.UNKNOWN,
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
            ClientTrans.KNOWN,
            ClientStates.WAIT_FOR_JOB,
            new AddInitialParameterHandler())
            .addTransition(
            ClientTrans.UNKNOWN,
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
    setState(ClientStates.UNCONNECTED);
  }

  /*--------------------------------------------------------------------------*/
  /**
   * Shows Error Dialog with short message.
   */
  private class ConnectionRejectedHandler implements ActionHandler {

    @Override
    public void handle(final Object... obj) {
      System.out.println("ConnectionRejectedHandler() called");
      Notification noti = new Notification.Builder(mContext)
              .setContentTitle("Connection Rejected")
              .setContentText("Server rejected connection")
              .setSmallIcon(R.drawable.ic_launcher)
              .setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_launcher))
              .build();
      mNotificationManager.notify(42, noti);
    }
  }

  /**
   * Sends disconnect instruction to master.setAccessingle
   */
  private class DisconnectHandler implements ActionHandler {

    @Override
    public void handle(final Object... o) {
      System.out.println("DisconnectHandler() called");
      mSConn.sendMessage(new Message(Instruction.DISCONNECT));
    }
  }

  /**
   * Requests connection to server.
   */
  private class SocketConnectedHandler implements ActionHandler {

    @Override
    public void handle(final Object... obj) {
      System.out.println("SocketConnectedHandler() called");
      mSConn.sendMessage(new Message(Instruction.REQUEST_CONNECTION, mDroitContext.getID()));
    }
  }

  /**
   * Sends Profile data to Server.
   */
  private class ProfileRequestHandler implements ActionHandler {

    @Override
    public void handle(final Object... obj) {
      System.out.println("ProfileRequestHandler() called");
      mSConn.sendMessage(new Message(Instruction.SEND_PROFILE, mDroitContext.getProfile()));
    }
  }

  /**
   * Shows input dialog to enter checkcode.
   */
  private class CheckcodeInputHandler implements ActionHandler {

    @Override
    public void handle(final Object... o) {
      System.out.println("CheckcodeInputHandler() called");
      Intent newintent = new Intent(mContext, MainActivity.class);
      newintent.setAction(BackgroundService.SHOW_CHECKCODE)
              .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
              .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      mContext.startActivity(newintent);
    }
  }

  /**
   * Sends entered checkcode to server.
   */
  private class CheckcodeSendHandler implements ActionHandler {

    @Override
    public void handle(final Object... o) {
      System.out.println("CheckcodeSendHandler() called");
      mSConn.sendMessage(new Message(Instruction.SEND_CHECKCODE, (String) o[0]));
    }
  }

  /*--------------------------------------------------------------------------*/
  /**
   *
   */
  private class CheckJobHandler implements ActionHandler {

    public void handle(Object... obj) {
      assert obj[0] instanceof String;
      assert (obj[1] instanceof DistributedJobParameter) || (obj[1] instanceof byte[]);

      System.out.println("CheckJobHandler...");
      try {
        if (mJobCenter.isTaskAvailable((String) obj[0])) {
          process(ClientTrans.KNOWN, obj);
        }
        else {
          try {
            mJobCenter.setLastUnserializedJob((byte[]) obj[1]);
          }
          // we may fail if we loaded the same task with different IDs
          // then we simply serialize the result again
          catch (ClassCastException ccex) {
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
            mJobCenter.setLastUnserializedJob(baos.toByteArray());
          }
          process(ClientTrans.UNKNOWN, obj);
        }
      }
      catch (StateMachineException ex) {
        Logger.getLogger(ClientStateMachine.class.getName()).log(Level.SEVERE, null, ex);
      }
      System.out.println("done.");
    }
  }

  /**
   *
   */
  private class JobFinishedHandler implements ActionHandler {

    @Override
    public void handle(final Object... obj) {
      assert obj[0] instanceof String;
      assert obj[1] instanceof DistributedJobResult;
      assert obj.length == 2;

      System.out.println("JobFinishedHandler() called");
      mSConn.sendMessage(new Message(Instruction.SEND_RESULT, (String) obj[0], (Serializable) obj[1]));
    }
  }

  /**
   * Checks if a binary for this parameter is already available.
   *
   * param[0]: id (String)
   * param[1]: parameter
   */
  private class CheckInitialParameterHandler implements ActionHandler {

    public void handle(Object... obj) {
      try {
        if (mJobCenter.isTaskAvailable((String) obj[0])) {
          process(ClientTrans.KNOWN, obj[0], obj[1]);
        }
        else {
          process(ClientTrans.UNKNOWN, obj[0], obj[1]);
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
  private class ProcessJobHandler implements ActionHandler {

    public void handle(Object... obj) {
      mSConn.sendMessage(new Message(Instruction.ACK));
      DistributedJobResult djr = mJobCenter.executeTask((String) obj[0], (DistributedJobParameter) obj[1]);
      try {
        process(ClientTrans.JOB_FINISHED, obj[0], djr);
      }
      catch (StateMachineException ex) {
        Logger.getLogger(ClientStateMachine.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }

  /**
   *
   */
  private class AddBinaryHandler implements ActionHandler {

    public void handle(Object... obj) {

      mJobCenter.loadBinary((String) obj[0], (byte[]) obj[1]);

      mSConn.sendMessage(new Message(Instruction.ACK));
    }
  }

  /*
   * 
   */
  private class AddInitialParameterHandler implements ActionHandler {

    public void handle(Object... obj) {
      mJobCenter.setInitialParameter((String) obj[0], (DistributedJobParameter) obj[1]);
      mSConn.sendMessage(new Message(Instruction.ACK));
    }
  }

  /**
   *
   */
  private class AddInitialAndProcessHandler implements ActionHandler {

    public void handle(Object... obj) {
      assert obj[0] instanceof String;
      assert obj[1] instanceof DistributedJobParameter;
      assert obj.length == 2;

      mJobCenter.setInitialParameter((String) obj[0], (DistributedJobParameter) obj[1]);
      mSConn.sendMessage(new Message(Instruction.ACK));
      DistributedJobParameter djp = mJobCenter.serializeJob();
      DistributedJobResult djr = mJobCenter.executeTask((String) obj[0], (DistributedJobParameter) djp);
      try {
        process(ClientTrans.JOB_FINISHED, obj[0], djr);
      }
      catch (StateMachineException ex) {
        Logger.getLogger(ClientStateMachine.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }

  /**
   * Simply sends a request command.
   * param[0] - id (String)
   */
  private class RequestBinaryHandler implements ActionHandler {

    public void handle(Object... obj) {
      assert obj[0] instanceof String;

      System.out.println("Sending request for a shit fucking binary...");
      mSConn.sendMessage(new Message(Instruction.REQUEST_BINARY, (Serializable) obj[0]));
      System.out.println("Request for a shit fucking binary done...");
    }
  }
}
