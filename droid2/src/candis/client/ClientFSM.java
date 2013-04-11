package candis.client;

import candis.client.comm.ServerConnection;
import candis.client.comm.ServerConnection.Status;
import candis.common.Instruction;
import candis.common.Message;
import candis.common.fsm.ActionHandler;
import candis.common.fsm.FSM;
import candis.common.fsm.StateEnum;

/**
 * Handles *all* communication with server and JobCenter.
 *
 * @author Enrico Joerns
 */
public class ClientFSM extends FSM implements ServerConnection.Receiver {

  private final JobCenter mJobCenter;

  public ClientFSM(ServerConnection sconn) {
    mJobCenter = new JobCenter(null);
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
  public void init() {
    addState(ClientStates.IDLE)
            .addTransition(
            Transitions.REGISTER,
            ClientStates.REGISTRATING,
            null);
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
  }

  private static class RegisterHandler extends ActionHandler {

    @Override
    public void handle(Object... obj) {
      throw new UnsupportedOperationException("Not supported yet.");
    }
  }

  // handles incoming message
  @Override
  public void OnNewMessage(Message msg) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void OnStatusUpdate(Status status) {
    throw new UnsupportedOperationException("Not supported yet.");
  }
}
