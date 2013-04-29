package candis.client;

import candis.client.comm.ServerConnection;
import candis.client.comm.ServerConnection.Status;
import candis.common.Instruction;
import candis.common.Message;
import candis.common.fsm.ActionHandler;
import candis.common.fsm.FSM;
import candis.common.fsm.StateEnum;
import candis.distributed.DistributedJobResult;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles *all* communication with server and delegates JobCenter.
 *
 * @author Enrico Joerns
 */
public class ClientFSM extends FSM implements ServerConnection.Receiver, JobCenterHandler {

  private static final String TAG = ClientFSM.class.getName();
  private static final Logger LOGGER = Logger.getLogger(TAG);
  private final ServerConnection mServerConnection;
  private final JobCenter mJobCenter;
  /* State to restore after registration. */
  private StateEnum mRestoreState;
//  private ActivityCommunicator mActivityComm;

  public ClientFSM(JobCenter jobCenter, ServerConnection sconn) {
    mServerConnection = sconn;
//    mActivityComm = acomm;
    mJobCenter = jobCenter;
  }

  private enum ClientStates implements StateEnum {

    IDLE,
    REGISTRATING,
    ENTER_CHECKCODE,
    CHECK_LISTEN,
    LISTENING,
    JOB_RECEIVED,
    JOB_PROCESSING,
    INIT_RECEIVED,
    BINARY_REQUESTED,
    BINARY_RECEIVED
  }

  public enum Transitions implements candis.common.fsm.Transition {

    REGISTER,
    UNREGISTER,
    CHECKCODE_ENTERED,
    BINARY_REQUIRED,
    JOB_DONE,
    JOB_STARTED,
    JOB_REJECTED
  }

  public JobCenter getJobCenter() {
    return mJobCenter;
  }

  @Override
  public final void init() {
    super.init();
    addState(ClientStates.IDLE);
    addState(ClientStates.REGISTRATING)
            .addTransition(
            Instruction.NACK,
            ClientStates.IDLE,
            null)
            .addTransition(
            Instruction.ACK,
            ClientStates.LISTENING,
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
    addState(ClientStates.LISTENING)
            .addTransition(
            Instruction.SEND_JOB,
            ClientStates.JOB_RECEIVED,
            new JobReceivedHandler())
            .addTransition(
            Instruction.SEND_INITIAL,
            ClientStates.INIT_RECEIVED,
            null);// TODO...
    addState(ClientStates.JOB_RECEIVED)
            .addTransition(
            Transitions.BINARY_REQUIRED,
            ClientStates.BINARY_REQUESTED,
            new RequestBinaryHandler())
            .addTransition(
            Transitions.JOB_STARTED,
            ClientStates.JOB_PROCESSING,
            new SendAckHandler());
    addState(ClientStates.BINARY_REQUESTED)
            .addTransition(
            Instruction.SEND_BINARY,
            ClientStates.BINARY_RECEIVED,
            new BinaryReceivedHandler());
    addState(ClientStates.BINARY_RECEIVED)
            .addTransition(
            Transitions.JOB_STARTED,
            ClientStates.JOB_PROCESSING,
            new SendAckHandler());
    addState(ClientStates.JOB_PROCESSING)
            .addTransition(
            Transitions.JOB_DONE,
            ClientStates.LISTENING,
            new JobDoneHandler());
    setState(ClientStates.IDLE);
    // Note: placed here because of a bug in FSM
    addGlobalTransition(
            Transitions.REGISTER,
            ClientStates.REGISTRATING,
            new RegisterHandler());
    addGlobalTransition(
            Transitions.UNREGISTER,
            ClientStates.REGISTRATING,
            new UnregisterHandler());

    setErrorTransition(
            ClientStates.IDLE,
            new TransitionErrorHandler());

    mJobCenter.addHandler(this);
  }

  private class TransitionErrorHandler extends ActionHandler {

    @Override
    public void handle(Object... data) {
      LOGGER.severe("UNEXPECTED TRANSITION -> FALLBACK TO IDLE");
      mServerConnection.sendMessage(new Message(Instruction.NACK));
    }
  }

  private class SendAckHandler extends ActionHandler {

    @Override
    public void handle(Object... data) {
      mServerConnection.sendMessage(new Message(Instruction.ACK));
    }
  }

  private class CheckcodeEnterHandler extends ActionHandler {

    @Override
    public void handle(Object... data) {
      // TODO: ...
//      mActivityComm.displayCheckcode((String) data[0]);
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
      // save restore state
      if ((getPreviousState() != null) && (getState() != ClientStates.REGISTRATING)) {
        mRestoreState = getPreviousState();
        if (mRestoreState == ClientStates.IDLE) {
          mRestoreState = ClientStates.LISTENING;
        }
        System.out.println("Restore state set to " + mRestoreState);
      }
      System.out.println("Sending registration message...");

      // send registration message to master
      mServerConnection.sendMessage(
              Message.create(Instruction.REGISTER,
                             DroidContext.getInstance().getID().getBytes(),
                             DroidContext.getInstance().getProfile()));
    }
  }

  private class UnregisterHandler extends ActionHandler {

    @Override
    public void handle(Object... data) {
      // send registration message to master
      mServerConnection.sendMessage(
              Message.create(Instruction.UNREGISTER, DroidContext.getInstance().getID().toSHA1()));
    }
  }

  private class RestoreStateHandler extends ActionHandler {

    // restore state that was active before register
    @Override
    public void handle(Object... data) {
      if (mRestoreState != null) {
        System.out.println("Restoring state: " + mRestoreState);
        setState(mRestoreState);
      }
    }
  }
  private String mCurrentRunnableID;
  private String mCurrentJobID;
  private byte[] mCurrentJobParameter;

  private class JobReceivedHandler extends ActionHandler {

    @Override
    public void handle(Object... data) {
      mCurrentRunnableID = (String) data[0];
      mCurrentJobID = (String) data[1];
      mCurrentJobParameter = (byte[]) data[2];

      LOGGER.info(String.format(
              "Received Job %s, for Task %s with %d params.",
              mCurrentJobID,
              mCurrentRunnableID,
              mCurrentJobParameter.length));

      mJobCenter.processJob(mCurrentRunnableID, mCurrentJobID, mCurrentJobParameter);
    }
  }

  private class RequestBinaryHandler extends ActionHandler {

    @Override
    public void handle(Object... data) {
      mServerConnection.sendMessage(new Message(Instruction.REQUEST_BINARY, (String) data[0]));
    }
  }

  private class BinaryReceivedHandler extends ActionHandler {

    @Override
    public void handle(Object... data) {
      if (!((String) data[0]).equals(mCurrentRunnableID)) {
        System.out.println("ERROR, wrong runnable ID " + (String) data[0] + " != " + mCurrentRunnableID);
        // TODO: handle...
      }
      mJobCenter.addRunnable((String) data[0], (byte[]) data[1], (byte[]) data[2]);

      mJobCenter.processJob((String) data[0], mCurrentJobID, mCurrentJobParameter);
    }
  }

  private class JobDoneHandler extends ActionHandler {

    @Override
    public void handle(Object... data) {
      String taskID = (String) data[0];
      String jobID = (String) data[1];
      DistributedJobResult[] results = (DistributedJobResult[]) data[2];
      Long exectime = (Long) data[3];
      LOGGER.info(String.format(
              "Sending result for Job %s of Task %s with %d params.",
              jobID,
              taskID,
              (results == null) ? 0 : results.length));
      mServerConnection.sendMessage(
              new Message(Instruction.SEND_RESULT,
                          taskID,
                          jobID,
                          results,
                          exectime));
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

  //--- JobCenter callbacks
  @Override
  public void onBinaryReceived(String runnableID) {
  }

  @Override
  public void onInitialParameterReceived(String runnableID) {
  }

  @Override
  public void onJobExecutionStart(String runnableID, String jobID) {
    process(Transitions.JOB_STARTED);
  }

  @Override
  public void onJobExecutionDone(String runnableID, String jobID, DistributedJobResult[] results, long exectime) {
    process(Transitions.JOB_DONE, runnableID, jobID, results, exectime);
  }

  @Override
  public void onBinaryRequired(String taskID) {
    process(Transitions.BINARY_REQUIRED, taskID);
  }

  @Override
  public void onJobRejected(String taskID, String jobID) {
    process(Transitions.JOB_REJECTED, taskID, jobID);
  }
}
