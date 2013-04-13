package candis.client;

import android.content.Context;
import candis.client.comm.ServerConnection;
import candis.client.comm.ServerConnection.Status;
import candis.client.service.ActivityCommunicator;
import candis.common.Instruction;
import candis.common.Message;
import candis.common.fsm.ActionHandler;
import candis.common.fsm.FSM;
import candis.common.fsm.StateEnum;
import candis.distributed.DistributedJobParameter;
import candis.distributed.DistributedJobResult;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles *all* communication with server and delegates JobCenter.
 *
 * @author Enrico Joerns
 */
public class ClientFSM extends FSM implements ServerConnection.Receiver, JobCenterHandler {
  
  private final ServerConnection mServerConnection;
  private final JobCenter mJobCenter;
  /* State to restore after registration. */
  private StateEnum mRestoreState;
  private ActivityCommunicator mActivityComm;
  
  public ClientFSM(Context context, ServerConnection sconn, ActivityCommunicator acomm) {
    mServerConnection = sconn;
    mActivityComm = acomm;
    mJobCenter = new JobCenter(context);
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
    BINARY_REQUESTED
  }
  
  public enum Transitions implements candis.common.fsm.Transition {
    
    REGISTER,
    CHECKCODE_ENTERED,
    BINARY_REQUIRED,
    FOO, // TODO...
    JOB_DONE
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
            new RegisterHandler());// TODO: global Transition!?
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
            Transitions.FOO,
            ClientStates.JOB_PROCESSING,
            null);
    addState(ClientStates.BINARY_REQUESTED)
            .addTransition(
            Instruction.SEND_BINARY,
            ClientStates.JOB_PROCESSING,
            new BinaryReceivedHandler());
    addState(ClientStates.JOB_PROCESSING)
            .addTransition(
            Transitions.JOB_DONE,
            ClientStates.LISTENING,
            new JobDoneHandler());
    setState(ClientStates.IDLE);
    
    mJobCenter.addHandler(this);
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
      // save restore state
      mRestoreState = getPreviousState();
      if (mRestoreState == ClientStates.IDLE) {
        mRestoreState = ClientStates.LISTENING;
      }
      System.out.println("Analyze restore state... is " + mRestoreState);
      System.out.println("Sending registration message...");

      // send registration message to master
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
      System.out.println("Restoring state: " + mRestoreState);
      setState(mRestoreState);
    }
  }
  private String mCurrentRunnableID;
  private String mCurrentJobID;
  private byte[] mCurrentJobParameter;
  
  private class JobReceivedHandler extends ActionHandler {
    
    @Override
    public void handle(Object... data) {
      System.out.println("JobReceivedHandler()");
      mCurrentRunnableID = (String) data[0];
      mCurrentJobID = (String) data[1];
      mCurrentJobParameter = (byte[]) data[2];
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
      mServerConnection.sendMessage(
              new Message(Instruction.SEND_RESULT,
                          (String) data[0],
                          (String) data[1],
                          (DistributedJobResult) data[2]));
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
  public void onAction(int action, String runnableID) {
  }
  
  public void onBinaryReceived(String runnableID) {
  }
  
  public void onInitialParameterReceived(String runnableID) {
  }
  
  public void onJobExecutionStart(String runnableID) {
  }
  
  public void onJobExecutionDone(String runnableID, DistributedJobResult result, long exectime) {
    process(Transitions.JOB_DONE, runnableID, "", result, exectime);
  }
  
  public void onBinaryRequired(String taskID) {
    process(Transitions.BINARY_REQUIRED, taskID);
  }
}
