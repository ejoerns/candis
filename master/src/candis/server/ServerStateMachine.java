package candis.server;

import candis.common.CandisLog;
import candis.common.DroidID;
import candis.common.Instruction;
import candis.common.Message;
import candis.common.Settings;
import candis.common.Utilities;
import candis.common.fsm.ActionHandler;
import candis.common.fsm.FSM;
import candis.common.fsm.StateEnum;
import candis.common.fsm.StateMachineException;
import candis.common.fsm.Transition;
import candis.distributed.DistributedJobParameter;
import candis.distributed.DistributedJobResult;
import candis.distributed.droid.StaticProfile;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.security.SecureRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of the server side communication and interaction protocol FSM.
 *
 * @author Enrico Joerns
 */
public class ServerStateMachine extends FSM {

	private static final String TAG = ServerStateMachine.class.getName();
	private static final Logger LOGGER = Logger.getLogger(TAG);
	/// Connection used for sending data
	protected final ClientConnection mConnection;
	/// Droid manager used for managing connections
	protected final DroidManager mDroidManager;
	/// Job distributor used for managing tasks and jobs
	protected final JobDistributionIOServer mJobDistIO;
//	protected Integer taskID = 0;

	/**
	 * All availbale states for the FSM.
	 */
	protected enum ServerStates implements StateEnum {

		UNCONNECTED,
		CHECK,
		PROFILE_REQUESTED,
		PROFILE_VALIDATE,
		CHECKCODE_REQUESTED,
		CHECKCODE_VALIDATE,
		CONNECTED,
		JOB_SENT,
		JOB_BINARY_SENT,
		JOB_INIT_SENT,
		INIT_SENT,
		INIT_BINARY_SENT,
		JOB_PROCESSING;
	}

	/**
	 * All server-side transition for the FSM.
	 */
	protected enum ServerTrans implements Transition {

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
		SEND_JOB,
		SEND_INITAL,// TODO...
		//		SEND_BINARY,
		STOP_JOB;// TODO...
	}

	public ServerStateMachine(
					final ClientConnection connection,
					final DroidManager droidManager,
					final JobDistributionIOServer comIO) {
		super();
		CandisLog.level(CandisLog.VERBOSE);
		mConnection = connection;
		mDroidManager = droidManager;
		mJobDistIO = comIO;
		init();
	}

	protected void init() {
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
						ServerTrans.SEND_JOB,
						ServerStates.JOB_SENT,
						new SendJobHandler())
						.addTransition(
						ServerTrans.SEND_INITAL,
						ServerStates.INIT_SENT,
						new SendInitialParameterHandler());
		addState(ServerStates.JOB_SENT)
						.addTransition(
						Instruction.ACK,
						ServerStates.JOB_PROCESSING)
						.addTransition(
						Instruction.REQUEST_BINARY,
						ServerStates.JOB_BINARY_SENT,
						new SendBinaryHandler());
		addState(ServerStates.JOB_BINARY_SENT)
						.addTransition(
						Instruction.ACK,
						ServerStates.JOB_INIT_SENT,
						new SendInitialParameterHandler());
		addState(ServerStates.JOB_INIT_SENT)
						.addTransition(
						Instruction.ACK,
						ServerStates.JOB_PROCESSING);
		addState(ServerStates.JOB_PROCESSING)
						.addTransition(
						Instruction.SEND_RESULT,
						ServerStates.CONNECTED,
						new ResultHandler());
		addState(ServerStates.INIT_SENT)
						.addTransition(
						Instruction.ACK,
						ServerStates.CONNECTED)
						.addTransition(
						Instruction.REQUEST_BINARY,
						ServerStates.INIT_BINARY_SENT,
						new SendBinaryHandler());
		addState(ServerStates.INIT_BINARY_SENT)
						.addTransition(Instruction.ACK, ServerStates.CONNECTED);

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
	private class ConnectionRequestedHandler implements ActionHandler {

		@Override
		public void handle(Object... obj) {
			assert obj[0] instanceof DroidID;

			System.out.println("ConnectionRequestedHandler called");
			if (obj == null) {
				LOGGER.log(Level.WARNING, "Missing payload data (expected RandomID)");
				return;
			}
			DroidID currentID = ((DroidID) obj[0]);
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
	private class ReceivedProfileHandler implements ActionHandler {

		@Override
		public void handle(final Object... obj) {
			System.out.println("ReceivedProfileHandler called");
			try {
				// store profile data
				if (!(obj[0] instanceof StaticProfile)) {
					LOGGER.log(Level.WARNING, "EMPTY PROFILE DATA!");
					process(ServerTrans.PROFILE_INVALID);
				}
				mDroidManager.addDroid(mConnection.getDroidID(), (StaticProfile) obj[0]);
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
	private class ClientConnectedHandler implements ActionHandler {

		@Override
		public void handle(final Object... o) {
			mDroidManager.connectDroid(mConnection.getDroidID(), mConnection);
			LOGGER.log(Level.INFO, String.format("Client %s connected", mConnection.getDroidID()));
			System.out.println("ClientConnectedHandler() called");
			mJobDistIO.onDroidConnected(mConnection.getDroidID(), mConnection);
			//mDroidManager.connectDroid(mCurrentID, mConnection);
		}
	}

	/**
	 * Gets called if client should be rejected.
	 */
	private class ConnectionRejectedHandler implements ActionHandler {

		@Override
		public void handle(final Object... o) {
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
	private class ClientDisconnectedHandler implements ActionHandler {

		@Override
		public void handle(final Object... o) {
			System.out.println("ClientDisconnectedHandler() called");
			mDroidManager.disconnectDroid(mConnection.getDroidID());
		}
	}

	/**
	 * Compares received check code from droid with server generated one.
	 */
	private class ValidateCheckcodeHandler implements ActionHandler {

		@Override
		public void handle(final Object... o) {
			System.out.println("ValidateCheckcodeHandler() called");
			try {
				if (mDroidManager.validateCheckCode((String) o[0])) {
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
	private class ProfileRequestHandler implements ActionHandler {

		@Override
		public void handle(final Object... o) {
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
	private class CheckCodeRequestedHandler implements ActionHandler {

		@Override
		public void handle(final Object... o) {
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
	private class ResultHandler implements ActionHandler {

		@Override
		public void handle(final Object... o) {
			assert o[0] instanceof String;
			assert o[1] instanceof DistributedJobResult;

			System.out.println("ClientJobDonedHandler() called");
			CandisLog.v(TAG, "Getting result...");
			DistributedJobResult result = null;
			if (o == null) {
				LOGGER.log(Level.WARNING, "Received Result: null");
			}
			else {
				LOGGER.log(Level.INFO, "Received Result");
				result = (DistributedJobResult) o[0];
			}
			mJobDistIO.onJobDone(mConnection.getDroidID(), result);
		}
	}

	/**
	 * Sends binary file to droid.
	 */
	private class SendBinaryHandler implements ActionHandler {

		@Override
		public void handle(final Object... binary) {
			assert binary[0] instanceof String;
			assert binary.length == 1;

			System.out.println("SendBinaryHandler() called");
			CandisLog.v(TAG, "Sending binary for task ID " + mJobDistIO.getCurrentTaskID());
			try {
				final File file = (File) mJobDistIO.getCDBLoader().getDroidBinary((String) binary[0]);// TODO:..
				int nRead;
				byte[] data = new byte[16384];
				byte[] outdata = new byte[(int) file.length()];

				// convert file to byte-array
				final InputStream is = new FileInputStream(file);
				final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

				while ((nRead = is.read(data, 0, data.length)) != -1) {
					buffer.write(data, 0, nRead);
				}
				buffer.flush();
				outdata = buffer.toByteArray();

				// ID is currently just a serial number
//				taskID++;
//
				mConnection.sendMessage(
								new Message(Instruction.SEND_BINARY,
														mJobDistIO.getCurrentTaskID(),
														//														"foo"));
														outdata));
//				mConnection.sendMessage(
//								new Message(Instruction.ACK));
			}
			catch (IOException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			}
		}
	}

//	/**
//	 * Gets called if droid ACKs binary receive.
//	 */
//	private class ClientBinarySentHandler implements ActionHandler {
//
//		@Override
//		public void handle(final Object... o) {
//			System.out.println("ClientBinarySentHandler() called");
//			mCommunicationIO.onBinarySent(mConnection.getDroidID());
//		}
//	}
	/**
	 * Sends the initial parameter to the droid. arguments: none (called by ACK)
	 */
	private class SendInitialParameterHandler implements ActionHandler {

		@Override
		public void handle(final Object... param) {
			assert param[0] == null;
//			assert param[0] instanceof String;
//			assert param[1] instanceof DistributedJobParameter;

			System.out.println("SendInitialParameterHandler() called");
			CandisLog.v(TAG, "Sending initial parameter for task ID " + mJobDistIO.getCurrentTaskID());
			try {
				assert mJobDistIO.getScheduler().getInitialParameter() != null;
				mConnection.sendMessage(new Message(Instruction.SEND_INITIAL, mJobDistIO.getCurrentTaskID(), mJobDistIO.getScheduler().getInitialParameter()));
			}
			catch (IOException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			}
		}
	}

	/**
	 * Gets called when the droid ACKed the initial parameter.
	 */
//	private class ClientInitalParameterSentHandler implements ActionHandler {
//
//		@Override
//		public void handle(final Object... o) {
//			System.out.println("ClientInitalParameterSentHandler() called");
//			mCommunicationIO.onInitalParameterSent(mConnection.getDroidID());
//		}
//	}
	/**
	 * Sends the job to the droid.
	 */
	private class SendJobHandler implements ActionHandler {

		@Override
		public void handle(final Object... params) {
			assert params[0] instanceof String;
			assert params[1] instanceof DistributedJobParameter;
			assert params.length == 2;

//			Serializable myparam = params[1];

			System.out.println("SendJobHandler() called");
			CandisLog.v(TAG, "Sending job for task ID " + params[0]);
			try {
				mConnection.sendMessage(new Message(
								Instruction.SEND_JOB,
								(String) params[0],
								(Serializable) params[1])); // TODO: param FAILS!
//				mConnection.sendMessage(new Message(Instruction.SEND_INITIAL, mJobDistIO.getCurrentTaskID(), mJobDistIO.getScheduler().getInitialParameter()));
			}
			catch (IOException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			}
		}
	}
}
