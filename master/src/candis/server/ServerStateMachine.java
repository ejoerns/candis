package candis.server;

import candis.common.Instruction;
import candis.common.Message;
import candis.common.RandomID;
import candis.common.Settings;
import candis.common.Utilities;
import candis.common.fsm.ActionHandler;
import candis.common.fsm.FSM;
import candis.common.fsm.StateEnum;
import candis.common.fsm.StateMachineException;
import candis.common.fsm.Transition;
import candis.distributed.DistributedResult;
import candis.distributed.droid.StaticProfile;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.security.SecureRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Enrico Joerns
 */
public class ServerStateMachine extends FSM {

	private static final String TAG = "ClientStateMachine";
	private static final Logger LOGGER = Logger.getLogger(TAG);
	protected final Connection mConnection;
	protected final DroidManager mDroidManager;
	protected final ServerCommunicationIO mCommunicationIO;
	//private RandomID mCurrentID;

	protected enum ServerStates implements StateEnum {

		UNCONNECTED,
		CHECK,
		PROFILE_REQUESTED,
		PROFILE_VALIDATE,
		CHECKCODE_REQUESTED,
		CHECKCODE_VALIDATE,
		CONNECTED,
		BINARY_SENT,
		BINARY_SENT_DONE,
		INIT_SENT,
		INIT_SENT_DONE,
		JOB_SENT,
		JOB_SENT_DONE;
	}

	public enum ServerTrans implements Transition {

		CLIENT_BLACKLISTED,
		CLIENT_NEW,
		CLIENT_NEW_CHECK,
		CLIENT_ACCEPTED,
		CHECKCODE_VALID,
		CHECKCODE_INVALID,
		PROFILE_VALID,
		PROFILE_INVALID,
		POST_JOB,
		CLIENT_INVALID,
		CLIENT_DISCONNECTED,
		SEND_INITAL,
		SEND_BINARY,
		SEND_JOB;
	}

	public ServerStateMachine(
					final Connection connection,
					final DroidManager droidManager,
					final ServerCommunicationIO comIO) {
		super();
		mConnection = connection;
		mDroidManager = droidManager;
		mCommunicationIO = comIO;
		init();
	}

	protected void init() {
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
		addState(ServerStates.CHECKCODE_REQUESTED)
						.addTransition(
						Instruction.SEND_CHECKCODE,
						ServerStates.CHECKCODE_VALIDATE,
						new ValidateCheckcodeHandler());
		addState(ServerStates.CHECKCODE_VALIDATE)
						.addTransition(
						ServerTrans.CHECKCODE_VALID,
						ServerStates.PROFILE_REQUESTED,
						new ProfileRequestHandler())
						.addTransition(
						ServerTrans.CHECKCODE_INVALID,
						ServerStates.UNCONNECTED,
						null);
		addState(ServerStates.PROFILE_REQUESTED)
						.addTransition(
						Instruction.SEND_PROFILE,
						ServerStates.PROFILE_VALIDATE,
						new ReceivedProfileHandler());
		addState(ServerStates.PROFILE_VALIDATE)
						.addTransition(
						ServerTrans.PROFILE_VALID,
						ServerStates.CONNECTED,
						new ClientConnectedHandler())
						.addTransition(
						ServerTrans.PROFILE_INVALID,
						ServerStates.UNCONNECTED,
						new ConnectionRejectedHandler());
		addState(ServerStates.CONNECTED)
						.addTransition(
						ServerTrans.SEND_BINARY,
						ServerStates.BINARY_SENT,
						new SendBinaryHandler());
		addState(ServerStates.BINARY_SENT)
						.addTransition(
						Instruction.ACK,
						ServerStates.BINARY_SENT_DONE,
						new ClientBinarySentHandler());
		addState(ServerStates.BINARY_SENT_DONE)
						.addTransition(
						ServerTrans.SEND_INITAL,
						ServerStates.INIT_SENT,
						new SendInitialParameterHandler())
						.addTransition(
						ServerTrans.SEND_BINARY,
						ServerStates.BINARY_SENT,
						null);
		addState(ServerStates.INIT_SENT)
						.addTransition(
						Instruction.ACK,
						ServerStates.INIT_SENT_DONE,
						new ClientInitalParameterSentHandler());
		addState(ServerStates.INIT_SENT_DONE)
						.addTransition(
						ServerTrans.SEND_JOB,
						ServerStates.JOB_SENT,
						new SendJobHandler())
						.addTransition(
						ServerTrans.SEND_BINARY,
						ServerStates.BINARY_SENT,
						null)
						.addTransition(
						ServerTrans.SEND_INITAL,
						ServerStates.INIT_SENT,
						null);
		addState(ServerStates.JOB_SENT)
						.addTransition(
						Instruction.ACK,
						ServerStates.JOB_SENT_DONE,
						null);
		addState(ServerStates.JOB_SENT_DONE)
						.addTransition(
						Instruction.SEND_RESULT,
						ServerStates.INIT_SENT_DONE,
						new ClientJobDonedHandler());

		addGlobalTransition(// TODO: 
						ServerTrans.CLIENT_DISCONNECTED,
						ServerStates.UNCONNECTED,
						new ClientDisconnectedHandler());
		addGlobalTransition(
						Instruction.DISCONNECT,
						ServerStates.UNCONNECTED,
						new ClientDisconnectedHandler());

		setState(ServerStates.UNCONNECTED);
	}

	/**
	 * Invoked if server got connection from a client.
	 */
	public class ConnectionRequestedHandler implements ActionHandler {

		@Override
		public void handle(Object obj) {
			System.out.println("ConnectionRequestedHandler called");
			if (obj == null) {
				LOGGER.log(Level.WARNING, "Missing payload data (expected RandomID)");
				return;
			}
			RandomID currentID = ((RandomID) obj);
			Transition trans;
			Instruction instr;
			// catch invalid messages
			if (currentID == null) {
				trans = ServerTrans.CLIENT_INVALID;
				instr = Instruction.ERROR;
				// check if droid is known in db
			}
			else {
				mConnection.setDroidID(currentID.toSHA1());
				if (mDroidManager.isDroidKnown(currentID)) {
					System.out.println("Droid ist known: " + currentID.toSHA1());
					if (mDroidManager.isDroidBlacklisted(currentID)) {
						System.out.println("Droid ist blacklisted");
						trans = ServerTrans.CLIENT_BLACKLISTED;
						instr = Instruction.REJECT_CONNECTION;
					}
					else {
						trans = ServerTrans.CLIENT_ACCEPTED;
						instr = Instruction.ACCEPT_CONNECTION;
					}
				}
				else {
					System.out.println("Droid ist unknown: " + currentID.toSHA1());
					// check if option 'check code auth' is active
					if (Settings.getBoolean("pincode_auth")) {
						trans = ServerTrans.CLIENT_NEW_CHECK;
						instr = Instruction.REQUEST_CHECKCODE;
					}
					else {
						trans = ServerTrans.CLIENT_NEW;
						instr = Instruction.REQUEST_PROFILE;
					}
				}
			}
			try {
				mConnection.sendMessage(new Message(instr));
				LOGGER.log(Level.INFO, String.format("Server reply: %s", instr));
				process(trans);
			}
			catch (StateMachineException ex) {
				Logger.getLogger(ServerStateMachine.class.getName()).log(Level.SEVERE, null, ex);
			}
			catch (IOException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			}
		}
	}

	/**
	 * Invoked if server received a clients profile data.
	 *
	 * Checks if profile data is valid and processes either CHECKCODE_VALID or
	 * CHECKCODE_INVALID
	 */
	public class ReceivedProfileHandler implements ActionHandler {

		@Override
		public void handle(final Object obj) {
			System.out.println("ReceivedProfileHandler called");
			try {
				// store profile data
				if (!(obj instanceof StaticProfile)) {
					LOGGER.log(Level.WARNING, "EMPTY PROFILE DATA!");
					process(ServerTrans.PROFILE_INVALID);
				}
				mDroidManager.addDroid(mConnection.getDroidID(), (StaticProfile) obj);
				mDroidManager.store(new File(Settings.getString("droiddb.file")));
				process(ServerTrans.PROFILE_VALID);
			}
			catch (StateMachineException ex) {
				Logger.getLogger(ServerStateMachine.class.getName()).log(Level.SEVERE, null, ex);
			}
			catch (IOException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			}
		}
	}

	/**
	 * Connects droid to DroidManager.
	 */
	public class ClientConnectedHandler implements ActionHandler {

		@Override
		public void handle(final Object o) {
			mDroidManager.connectDroid(mConnection.getDroidID(), mConnection);
			LOGGER.log(Level.INFO, String.format("Client %s connected", mConnection.getDroidID()));
			System.out.println("ClientConnectedHandler() called");
			mCommunicationIO.onDroidConnected(mConnection.getDroidID(), mConnection);
			//mDroidManager.connectDroid(mCurrentID, mConnection);
		}
	}

	/**
	 * Gets called if client should be rejected.
	 */
	public class ConnectionRejectedHandler implements ActionHandler {

		@Override
		public void handle(final Object o) {
			System.out.println("ConnectionRejectedHandler() called");
			try {
				mConnection.sendMessage(new Message(Instruction.REJECT_CONNECTION));
			}
			catch (IOException ex) {
				Logger.getLogger(ServerStateMachine.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

	/**
	 * Disconnects droid from DroidManager.
	 */
	public class ClientDisconnectedHandler implements ActionHandler {

		@Override
		public void handle(final Object o) {
			System.out.println("ClientDisconnectedHandler() called");
			mDroidManager.disconnectDroid(mConnection.getDroidID());
		}
	}

	/**
	 * Compares received check code from droid with server generated one.
	 */
	public class ValidateCheckcodeHandler implements ActionHandler {

		@Override
		public void handle(final Object o) {
			System.out.println("ValidateCheckcodeHandler() called");
			try {
				if (mDroidManager.validateCheckCode((String) o)) {
					process(ServerTrans.CHECKCODE_VALID);
				}
				else {
					process(ServerTrans.CHECKCODE_INVALID);
				}
			}
			catch (StateMachineException ex) {
				Logger.getLogger(ServerStateMachine.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

	/**
	 * Requests profile without check.
	 */
	public class ProfileRequestHandler implements ActionHandler {

		@Override
		public void handle(final Object o) {
			System.out.println("ProfileRequestHandler() called");
			try {
				mConnection.sendMessage(new Message(Instruction.REQUEST_PROFILE));
			}
			catch (IOException ex) {
				Logger.getLogger(ServerStateMachine.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

	/**
	 * Generates new check code (6 digits) and shows it via DroidManager.
	 */
	public class CheckCodeRequestedHandler implements ActionHandler {

		@Override
		public void handle(final Object o) {
			System.out.println("CheckCodeRequestedHandler() called");
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

	/**
	 * Gets called if JOB_DONE was received.
	 */
	public class ClientJobDonedHandler implements ActionHandler {

		@Override
		public void handle(final Object o) {
			System.out.println("ClientJobDonedHandler() called");
			mCommunicationIO.onJobDone(mConnection.getDroidID(), (DistributedResult) o);
		}
	}

	/**
	 * Sends binary file to droid.
	 */
	public class SendBinaryHandler implements ActionHandler {

		@Override
		public void handle(final Object binary) {
			System.out.println("SendBinaryHandler() called");
			try {
				// TODO: test if empty
				mConnection.sendMessage(new Message(Instruction.SEND_BINARY, (Serializable) binary));
			}
			catch (IOException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			}
		}
	}

	/**
	 * Gets called if droid ACKs binary receive.
	 */
	public class ClientBinarySentHandler implements ActionHandler {

		@Override
		public void handle(final Object o) {
			System.out.println("ClientBinarySentHandler() called");
			mCommunicationIO.onBinarySent(mConnection.getDroidID());
		}
	}

	/**
	 * Sends the initial parameter to the droid.
	 */
	public class SendInitialParameterHandler implements ActionHandler {

		@Override
		public void handle(final Object param) {
			System.out.println("SendInitialParameterHandler() called");
			try {
				// TODO: test if empty
				mConnection.sendMessage(new Message(Instruction.SEND_INITAL, (Serializable) param));
			}
			catch (IOException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			}
		}
	}

	/**
	 * Gets called when the droid ACKed the initial parameter.
	 */
	public class ClientInitalParameterSentHandler implements ActionHandler {

		@Override
		public void handle(final Object o) {
			System.out.println("ClientInitalParameterSentHandler() called");
			mCommunicationIO.onInitalParameterSent(mConnection.getDroidID());
		}
	}

	/**
	 * Sends the job to the droid.
	 */
	public class SendJobHandler implements ActionHandler {

		@Override
		public void handle(final Object param) {
			System.out.println("SendJobHandler() called");
			try {
				mConnection.sendMessage(new Message(Instruction.SEND_JOB, (Serializable) param));
			}
			catch (IOException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			}
		}
	}
}
