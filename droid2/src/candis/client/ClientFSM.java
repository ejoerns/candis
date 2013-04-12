package candis.client;

import android.os.Bundle;
import candis.client.comm.ServerConnection;
import candis.client.comm.ServerConnection.Status;
import candis.client.service.ActivityCommunicator;
import candis.common.Instruction;
import candis.common.Message;
import candis.common.fsm.ActionHandler;
import candis.common.fsm.FSM;
import candis.common.fsm.StateEnum;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles *all* communication with server and delegates JobCenter.
 *
 * @author Enrico Joerns
 */
public class ClientFSM extends FSM implements ServerConnection.Receiver {

  private final ServerConnection mServerConnection;
  private final JobCenter mJobCenter;
  /* State to restore after registration. */
  private StateEnum mRestoreState;
  private ActivityCommunicator mActivityComm;

  public ClientFSM(ServerConnection sconn, ActivityCommunicator acomm) {
    mServerConnection = sconn;
    mActivityComm = acomm;
    mJobCenter = new JobCenter(null);
    init();
  }

  private enum ClientStates implements StateEnum {

    IDLE,
    REGISTRATING,
    ENTER_CHECKCODE,
    CHECK_LISTEN,
    LISTENING,
    JOB_RECEIVED,
    JOB_PROCESSING
  }

  public enum Transitions implements candis.common.fsm.Transition {

    REGISTER,
    CHECKCODE_ENTERED
  }

  public JobCenter getJobCenter() {
    return mJobCenter;
  }

  @Override
  public final void init() {
    addState(ClientStates.IDLE)
            .addTransition(
            Transitions.REGISTER,
            ClientStates.REGISTRATING,
            new RegisterHandler());
    addState(ClientStates.REGISTRATING)
            .addTransition(
            Instruction.NACK,
            ClientStates.IDLE,
            null)
            .addTransition(
            Instruction.ACK,
            ClientStates.IDLE,
            new RestoreStateHandler())
            .addTransition(
            Instruction.REQUEST_CHECKCODE,
            ClientStates.ENTER_CHECKCODE,
            new CheckcodeEnterHandler());
    addState(ClientStates.ENTER_CHECKCODE)
            .addTransition(
            Transitions.CHECKCODE_ENTERED,
            ClientStates.REGISTRATING,
            new SendCheckcodeHandler());
    setState(ClientStates.IDLE);
  }

  private class CheckcodeEnterHandler extends ActionHandler {

    @Override
    public void handle(Object... data) {
      mActivityComm.displayCheckcode((String) data[0]);
    }
  }

  private class SendCheckcodeHandler extends ActionHandler {

    @Override
    public void handle(Object... data) {
      gotCalled();
      // send checkcode(droidID, code)
      mServerConnection.sendMessage(
              Message.create(Instruction.SEND_CHECKCODE,
                             DroidContext.getInstance().getID().toSHA1(),
                             (String) data[0]));
    }
  }

  private class RegisterHandler extends ActionHandler {

    @Override
    public void handle(Object... data) {
      // send registration message to master
      mRestoreState = getPreviousState();
      System.out.println("Sending registration message...");

      mServerConnection.sendMessage(
              Message.create(Instruction.REGISTER,
                             DroidContext.getInstance().getID(),
                             DroidContext.getInstance().getProfile()));
    }
  }

  private class RestoreStateHandler extends ActionHandler {

    // restore state that was active before register
    @Override
    public void handle(Object... data) {
      setState(mRestoreState);
    }
  }

  // handles incoming message
  @Override
  public void OnNewMessage(Message msg) {
    if (msg.getData() == null) {
      Logger.getLogger(ClientFSM.class.getName()).log(Level.SEVERE, "Got Message: " + msg.getRequest());
      process(((Message) msg).getRequest());
    }
    else {
      Logger.getLogger(ClientFSM.class.getName()).log(Level.SEVERE, "Got Message: " + msg.getRequest());
      process(msg.getRequest(), (Object[]) msg.getData());
    }
  }

  @Override
  public void OnStatusUpdate(Status status) {
  }
}
