package candis.client;

import candis.client.comm.CommRequestBroker;
import candis.client.comm.SecureConnection;
import candis.common.Instruction;
import candis.common.Message;
import candis.common.fsm.ActionHandler;
import candis.common.fsm.FSM;
import candis.common.fsm.HandlerID;
import candis.common.fsm.StateEnum;
import candis.common.fsm.StateMachineException;
import candis.common.fsm.Transition;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Enrico Joerns
 */
public final class ClientStateMachine extends FSM {

	private static final String TAG = "ClientStateMachine";
	private static final Logger logger = Logger.getLogger(TAG);
	private final SecureConnection sconn;

	private enum ClientStates implements StateEnum {

		UNCONNECTED,
		WAIT_ACCEPT,
		PROFILE_SENT,
		WAIT_FOR_JOB,
		RECEIVED_JOB;
	}

	public enum ClientTrans implements Transition {

		SOCKET_CONNECTED,
		//		ACCEPT_CONNECTION,
		//		REJECT_CONNECTION,
		//		REQUEST_PROFILE,
		//		SEND_JOB,
		JOB_FINISHED;
	}

	private enum ClientHandlerID implements HandlerID {

		MY_ID;
	}

	ClientStateMachine(SecureConnection sconn) {
		this.sconn = sconn;
		init();
	}

	protected void init() {
		addState(ClientStates.UNCONNECTED)
						.addTransition(
						ClientTrans.SOCKET_CONNECTED,
						ClientStates.WAIT_ACCEPT,
						new SocketConnectedHandler());
		addState(ClientStates.WAIT_ACCEPT)
						.addTransition(
						Instruction.REQUEST_PROFILE,
						ClientStates.PROFILE_SENT,
						new MySecondHandler())
						.addTransition(
						Instruction.ACCEPT_CONNECTION,
						ClientStates.WAIT_FOR_JOB,
						null);
		addState(ClientStates.PROFILE_SENT)
						.addTransition(
						Instruction.ACCEPT_CONNECTION,
						ClientStates.WAIT_FOR_JOB,
						null);
		addState(ClientStates.WAIT_FOR_JOB)
						.addTransition(
						Instruction.SEND_JOB,
						ClientStates.RECEIVED_JOB,
						null);
		addState(ClientStates.RECEIVED_JOB)
						.addTransition(
						ClientTrans.JOB_FINISHED,
						ClientStates.WAIT_FOR_JOB,
						null);


		// Run test
		try {
			setState(ClientStates.UNCONNECTED);
			System.out.println("State: " + getState());
			process(ClientTrans.SOCKET_CONNECTED);
			System.out.println("State: " + getState());
			process(Instruction.ACCEPT_CONNECTION);
			System.out.println("State: " + getState());
			process(Instruction.SEND_JOB);
			System.out.println("State: " + getState());
		} catch (StateMachineException ex) {
			logger.log(Level.SEVERE, null, ex);
		}
	}

	private class SocketConnectedHandler implements ActionHandler {

		@Override
		public void handle() {
			System.out.println("Handler knows that socket is connected :)");
			try {
				// todo: ID
				sconn.writeObject(new Message(Instruction.REQUEST_CONNECTION, null));
			} catch (IOException ex) {
				Logger.getLogger(ClientStateMachine.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

	private class MySecondHandler implements ActionHandler {

		@Override
		public void handle() {
			System.out.println("Handle :)");
		}
	}
}
