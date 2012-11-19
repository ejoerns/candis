package candis.server;

import candis.common.ByteArray;
import candis.common.Instruction;
import candis.common.Message;
import candis.common.RandomID;
import candis.common.fsm.ActionHandler;
import candis.common.fsm.FSM;
import candis.common.fsm.HandlerID;
import candis.common.fsm.StateEnum;
import candis.common.fsm.StateMachineException;
import candis.common.fsm.Transition;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Enrico Joerns
 */
public class ServerStateMachine extends FSM {

	private static final String TAG = "ClientStateMachine";
	private static final Logger LOGGER = Logger.getLogger(TAG);
	private final ObjectOutputStream mOutStream;
	private final DroidManager mDroidManager = DroidManager.getInstance();

	private enum ServerStates implements StateEnum {

		UNCONNECTED,
		CHECK,
		PROFILE_REQUESTED,
		CONNECTED,
		JOB_SENT;
	}

	public enum ServerTrans implements Transition {

		CLIENT_BLACKLISTED,
		CLIENT_NEW,
		CLIENT_ACCEPTED,
		POST_JOB;
	}

	private enum ClientHandlerID implements HandlerID {

		MY_ID;
	}

	ServerStateMachine(final ObjectOutputStream outstream) {
		super();
		this.mOutStream = outstream;
		init();
	}

	private void init() {
		addState(ServerStates.UNCONNECTED)
						.addTransition(
						Instruction.REQUEST_CONNECTION,
						ServerStates.CHECK,
						new ConnectionRequestedHandler());
		addState(ServerStates.CHECK)
						.addTransition(
						ServerTrans.CLIENT_BLACKLISTED,
						ServerStates.UNCONNECTED,
						null)
						.addTransition(
						ServerTrans.CLIENT_NEW,
						ServerStates.PROFILE_REQUESTED,
						null)
						.addTransition(
						ServerTrans.CLIENT_ACCEPTED,
						ServerStates.CONNECTED,
						null);
		addState(ServerStates.PROFILE_REQUESTED)
						.addTransition(
						Instruction.SEND_PROFILE,
						ServerStates.CONNECTED,
						new ReceivedProfileHandler());
		addState(ServerStates.CONNECTED)
						.addTransition(
						ServerTrans.POST_JOB,
						ServerStates.JOB_SENT,
						null);
		addState(ServerStates.JOB_SENT)
						.addTransition(
						Instruction.SEND_RESULT,
						ServerStates.CONNECTED,
						null);
		setState(ServerStates.UNCONNECTED);
	}

	private class ConnectionRequestedHandler implements ActionHandler {

		@Override
		public void handle(Object obj) {
			System.out.println("ConnectionRequestedHandler called");
			if (obj == null) {
				LOGGER.log(Level.WARNING, "Missing payload data (expected RandomID)");
				return;
			}
			RandomID rand = ((RandomID) obj);
			Transition trans;
			Instruction instr;
			// check droid in db
			if (mDroidManager.isDroidKnown(rand)) {
				if (mDroidManager.isDroidBlacklisted(rand)) {
					trans = ServerTrans.CLIENT_BLACKLISTED;
					instr = Instruction.REJECT_CONNECTION;
				} else {
					trans = ServerTrans.CLIENT_ACCEPTED;
					instr = Instruction.ACCEPT_CONNECTION;
				}
			} else {
				trans = ServerTrans.CLIENT_NEW;
				instr = Instruction.REQUEST_PROFILE;
			}
			try {
				mOutStream.writeObject(new Message(instr));
				process(trans);
			} catch (StateMachineException ex) {
				Logger.getLogger(ServerStateMachine.class.getName()).log(Level.SEVERE, null, ex);
			} catch (IOException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			}
		}
	}

	// TODO: make static?
	private class ReceivedProfileHandler implements ActionHandler {

		@Override
		public void handle(Object obj) {
			System.out.println("ReceivedProfileHandler called");
			try {
				// store profile data
				mOutStream.writeObject(new Message(Instruction.ACCEPT_CONNECTION));
			} catch (IOException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			}
		}
	}
}
