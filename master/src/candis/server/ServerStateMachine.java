package candis.server;

import candis.common.Instruction;
import candis.common.Message;
import candis.common.RandomID;
import candis.common.Settings;
import candis.common.Utilities;
import candis.common.fsm.ActionHandler;
import candis.common.fsm.FSM;
import candis.common.fsm.HandlerID;
import candis.common.fsm.StateEnum;
import candis.common.fsm.StateMachineException;
import candis.common.fsm.Transition;
import candis.distributed.CommunicationIO;
import candis.distributed.droid.StaticProfile;
import candis.server.gui.CheckCodeShowDialog;
import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

/**
 *
 * @author Enrico Joerns
 */
public class ServerStateMachine extends FSM {

	private static final String TAG = "ClientStateMachine";
	private static final Logger LOGGER = Logger.getLogger(TAG);
	private final Connection mConnection;
	protected final DroidManager mDroidManager;
	protected final CommunicationIO mCommunicationIO;
	RandomID mCurrentlyConnectingID;


	private enum ServerStates implements StateEnum {

		UNCONNECTED,
		CHECK,
		PROFILE_REQUESTED,
		CHECKCODE_REQUESTED,
		CONNECTED,
		JOB_SENT;
	}

	public enum ServerTrans implements Transition {

		CLIENT_BLACKLISTED,
		CLIENT_NEW,
		CLIENT_NEW_CHECK,
		CLIENT_ACCEPTED,
		POST_JOB,
		CLIENT_INVALID;
	}

	private enum ClientHandlerID implements HandlerID {

		MY_ID;
	}

	public ServerStateMachine(final Connection connection, final DroidManager droidManager, final CommunicationIO comIO) {
		super();
		mConnection = connection;
		mDroidManager = droidManager;
		mCommunicationIO = comIO;
		init();
	}

	private void init() {
		// TODO: add default transition? "else Transition"?
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
						ServerTrans.CLIENT_NEW_CHECK,
						ServerStates.CHECKCODE_REQUESTED,
						new CheckCodeRequestedHandler())
						.addTransition(
						ServerTrans.CLIENT_ACCEPTED,
						ServerStates.CONNECTED,
						new ClientConnectedHandler())
						.addTransition(
						ServerTrans.CLIENT_INVALID,
						ServerStates.UNCONNECTED,
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

	/**
	 * Invoked if server got connection from a client.
	 */
	private class ConnectionRequestedHandler implements ActionHandler {

		@Override
		public void handle(Object obj) {
			System.out.println("ConnectionRequestedHandler called");
			if (obj == null) {
				LOGGER.log(Level.WARNING, "Missing payload data (expected RandomID)");
				return;
			}
			mCurrentlyConnectingID = ((RandomID) obj);
			Transition trans;
			Instruction instr;
			// catch invalid messages
			if (mCurrentlyConnectingID == null) {
				trans = ServerTrans.CLIENT_INVALID;
				instr = Instruction.ERROR;
				// check if droid is known in db
			} else if (mDroidManager.isDroidKnown(mCurrentlyConnectingID)) {
				if (mDroidManager.isDroidBlacklisted(mCurrentlyConnectingID)) {
					trans = ServerTrans.CLIENT_BLACKLISTED;
					instr = Instruction.REJECT_CONNECTION;
				} else {
					trans = ServerTrans.CLIENT_ACCEPTED;
					instr = Instruction.ACCEPT_CONNECTION;
				}
			} else {
				// check if option 'check code auth' is active
				if (Settings.getBoolean("pincode_auth")) {
					trans = ServerTrans.CLIENT_NEW_CHECK;
					instr = Instruction.REQUEST_CHECKCODE;
				} else {
					trans = ServerTrans.CLIENT_NEW;
					instr = Instruction.REQUEST_PROFILE;
				}
			}
			try {
				LOGGER.log(Level.INFO, String.format("Server reply: %s", instr));
				process(trans);
			} catch (StateMachineException ex) {
				Logger.getLogger(ServerStateMachine.class.getName()).log(Level.SEVERE, null, ex);
//			} catch (IOException ex) {
//				LOGGER.log(Level.SEVERE, null, ex);
			}
		}
	}

	// TODO: make static?
	/**
	 * Invoked if server received a clients profile data.
	 */
	private class ReceivedProfileHandler implements ActionHandler {

		@Override
		public void handle(final Object obj) {
			System.out.println("ReceivedProfileHandler called");
			try {
				// store profile data
				if (!(obj instanceof StaticProfile)) {
					LOGGER.log(Level.WARNING, "EMPTY PROFILE DATA!");
				}
				mDroidManager.addDroid(mCurrentlyConnectingID, (StaticProfile) obj);
				mDroidManager.store(new File(Settings.getString("droiddb.file")));
				mDroidManager.connectDroid(mCurrentlyConnectingID, mConnection);
				LOGGER.log(Level.INFO, String.format("Server reply: %s", Instruction.ACCEPT_CONNECTION));
				LOGGER.log(Level.INFO, String.format("Client %s connected", mCurrentlyConnectingID));
			} catch (IOException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			}
		}
	}

	private class ClientConnectedHandler implements ActionHandler {

		@Override
		public void handle(final Object o) {
			mDroidManager.connectDroid(mCurrentlyConnectingID, mConnection);
		}
	}

	private class CheckCodeRequestedHandler implements ActionHandler {

		@Override
		public void handle(final Object o) {
			// generate 6digit string
			final SecureRandom random = new SecureRandom();
			final byte[] byteCode = new byte[3];
			random.nextBytes(byteCode);

			StringBuffer buf = new StringBuffer();
			int len = byteCode.length;
			for (int i = 0; i < len; i++) {
				Utilities.byte2hex(byteCode[i], buf);
			}
			mDroidManager.showCheckCode(buf.toString());
		}
	}
}
