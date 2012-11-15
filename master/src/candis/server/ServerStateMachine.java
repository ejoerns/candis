package candis.server;

import candis.common.Instruction;
import candis.common.fsm.ActionHandler;
import candis.common.fsm.FSM;
import candis.common.fsm.HandlerID;
import candis.common.fsm.StateEnum;
import candis.common.fsm.StateMachineException;
import candis.common.fsm.Transition;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Enrico Joerns
 */
public class ServerStateMachine extends FSM {

	private static final String TAG = "ClientStateMachine";
	private static final Logger logger = Logger.getLogger(TAG);

	private enum ServerStates implements StateEnum {

		UNCONNECTED,
		CHECK,
		PROFILE_REQUESTED,
		CONNECTED,
		JOB_SENT;
	}

	public enum ServerTrans implements Transition {

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

	ServerStateMachine() {
		init();
	}

	protected void init() {
		addState(ServerStates.UNCONNECTED);
		addState(ServerStates.CHECK);
		addState(ServerStates.PROFILE_REQUESTED);
		addState(ServerStates.CONNECTED);
		addState(ServerStates.JOB_SENT);



		// Run test
		try {
			setState(ServerStates.UNCONNECTED);
			System.out.println("State: " + getState());
			process(ServerTrans.SOCKET_CONNECTED);
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
			System.out.println("Handle :)");
		}
	}

	private class MySecondHandler implements ActionHandler {

		@Override
		public void handle() {
			System.out.println("Handle :)");
		}
	}
}
