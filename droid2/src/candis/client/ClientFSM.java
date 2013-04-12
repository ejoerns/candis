package candis.client;

import candis.client.comm.ServerConnection;
import candis.client.comm.ServerConnection.Status;
import candis.common.Instruction;
import candis.common.Message;
import candis.common.fsm.ActionHandler;
import candis.common.fsm.FSM;
import candis.common.fsm.StateEnum;

/**
 * Handles *all* communication with server and delegates JobCenter.
 *
 * @author Enrico Joerns
 */
public class ClientFSM extends FSM implements ServerConnection.Receiver {

  private final ServerConnection mServerConnection;
  private final JobCenter mJobCenter;

  public ClientFSM(ServerConnection sconn) {
    mServerConnection = sconn;
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

    REGISTER
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
            null)
            .addTransition(
            Instruction.REQUEST_CHECKCODE,
            ClientStates.ENTER_CHECKCODE,
            null);
    addState(ClientStates.ENTER_CHECKCODE)
            .addTransition(
            Instruction.SEND_CHECKCODE,
            ClientStates.REGISTRATING,
            null);
    setState(ClientStates.IDLE);
  }

  private class RegisterHandler extends ActionHandler {

    @Override
    public void handle(Object... obj) {
      // send registration message to master
      System.out.println("Sending registration message...");

      mServerConnection.sendMessage(
              Message.create(Instruction.REGISTER,
                             DroidContext.getInstance().getID(),
                             DroidContext.getInstance().getProfile()));
    }
  }

  // handles incoming message
  @Override
  public void OnNewMessage(Message msg) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void OnStatusUpdate(Status status) {
  }
}
