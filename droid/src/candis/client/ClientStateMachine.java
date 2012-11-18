package candis.client;

import candis.client.comm.CommRequestBroker;
import candis.client.comm.SecureConnection;
import candis.common.Instruction;
import candis.common.Message;
import candis.common.RandomID;
import candis.common.fsm.ActionHandler;
import candis.common.fsm.FSM;
import candis.common.fsm.HandlerID;
import candis.common.fsm.StateEnum;
import candis.common.fsm.Transition;
import candis.distributed.droid.StaticProfile;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Enrico Joerns
 */
public final class ClientStateMachine extends FSM {

	private static final String TAG = "ClientStateMachine";
	private static final Logger logger = Logger.getLogger(TAG);
	private ObjectOutputStream mOutStream = null;
	final RandomID rid;
	final StaticProfile profile;

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

	ClientStateMachine(SecureConnection sconn, final RandomID rid, final StaticProfile profile) {
		this.rid = rid;
		this.profile = profile;
		try {
			this.mOutStream = new ObjectOutputStream(sconn.getOutputStream());
		} catch (IOException ex) {
			logger.log(Level.SEVERE, null, ex);
		}
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
						new ProfileRequestHandler())
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
		setState(ClientStates.UNCONNECTED);
	}

	private class SocketConnectedHandler implements ActionHandler {

		@Override
		public void handle(Object obj) {
			System.out.println("SocketConnectedHandler() called");
			try {
				// todo: ID
				mOutStream.writeObject(new Message(Instruction.REQUEST_CONNECTION, rid));
			} catch (IOException ex) {
				Logger.getLogger(ClientStateMachine.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

	private class ProfileRequestHandler implements ActionHandler {

		@Override
		public void handle(Object obj) {
			System.out.println("ProfileRequestHandler() called");
			try {
				mOutStream.writeObject(new Message(Instruction.SEND_PROFILE, profile));
			} catch (IOException ex) {
				Logger.getLogger(ClientStateMachine.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}
}
