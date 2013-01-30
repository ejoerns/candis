package candis.server;

import candis.common.CandisLog;
import candis.common.ClassloaderObjectInputStream;
import candis.common.DroidID;
import candis.common.Instruction;
import candis.common.Message;
import candis.common.Settings;
import candis.common.Utilities;
import candis.common.fsm.ActionHandler;
import candis.common.fsm.FSM;
import candis.common.fsm.StateEnum;
import candis.common.fsm.Transition;
import candis.distributed.DistributedJobError;
import candis.distributed.DistributedJobParameter;
import candis.distributed.DistributedJobResult;
import candis.distributed.droid.StaticProfile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.security.SecureRandom;
import java.util.Random;
import java.util.Timer;
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
	///
	protected PingTimerTask mPingTimerTask;
	protected Timer mPingTimer;

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
		// (droidID, checkcodeID)
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
		STOP_JOB,// TODO...
		SEND_PING,
		ERROR;
	}

	public ServerStateMachine(
					final ClientConnection connection,
					final DroidManager droidManager,
					final JobDistributionIOServer comIO) {
		super();
		mConnection = connection;
		mDroidManager = droidManager;
		mJobDistIO = comIO;
	}

	@Override
	public void init() {
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
						null);// TODO...
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
						.addTransition(
						Instruction.ACK,
						ServerStates.CONNECTED);

		addGlobalTransition(// TODO:
						ServerTrans.CLIENT_DISCONNECTED,
						ServerStates.UNCONNECTED,
						new ClientDisconnectedHandler());
		addGlobalTransition(
						Instruction.DISCONNECT,
						ServerStates.UNCONNECTED,
						new ClientDisconnectedHandler());
		addGlobalTransition(
						ServerTrans.SEND_PING,
						null,
						new SendPingHandler());
		addGlobalTransition(
						Instruction.PONG,
						null,
						new PongHandler());

		setState(ServerStates.UNCONNECTED);
	}

	/**
	 * Invoked if server got connection from a client.
	 */
	private class ConnectionRequestedHandler extends ActionHandler {

		@Override
		public void handle(Object... obj) {
			assert obj != null;
			assert obj[0] instanceof DroidID : ((Instruction) obj[0]).toString();

			gotCalled();
			if (obj == null) {
				LOGGER.log(Level.WARNING, "Missing payload data (expected RandomID)");
				return;
			}

			DroidID currentID = ((DroidID) obj[0]);
			Transition trans;
			Instruction instr;

			// catch invalid messages
			if (currentID == null) {
				LOGGER.severe("Connection request with empty ID");
				trans = ServerTrans.CLIENT_INVALID;
				instr = Instruction.ERROR;
			}
			// check if droid is known in db
			else {
				mConnection.setDroidID(currentID.toSHA1());
				if (mDroidManager.isDroidKnown(currentID)) {
					System.out.println("Droid is known: " + currentID.toSHA1());
					// check if droid is blacklisted
					if (mDroidManager.isDroidBlacklisted(currentID)) {
						System.out.println("Droid is blacklisted");
						trans = ServerTrans.CLIENT_BLACKLISTED;
						instr = Instruction.REJECT_CONNECTION;
					}
					else {
						trans = ServerTrans.CLIENT_ACCEPTED;
						instr = Instruction.ACCEPT_CONNECTION;
					}
				}
				// seems to be a new droid
				else {
					System.out.println("Droid is unknown: " + currentID.toSHA1());
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
			// Send instruction
			try {
				// if checkcode is requested, generate a halfway unique id to let the
				// users identify their devices
				if (instr == Instruction.REQUEST_CHECKCODE) {
					Random random = new Random();
					byte[] bytes = new byte[4];
					random.nextBytes(bytes);
					String checkcodeID = Utilities.toHexString(bytes);
					mConnection.sendMessage(new Message(instr, checkcodeID));
					process(trans, currentID.toSHA1(), checkcodeID);
				}
				else {
					mConnection.sendMessage(new Message(instr));
					process(trans, currentID.toSHA1());
				}
			}
			catch (IOException ex) {
				process(ServerTrans.ERROR);
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
	private class ReceivedProfileHandler extends ActionHandler {

		@Override
		public void handle(final Object... obj) {
			gotCalled();
			try {
				// store profile data
				if (!(obj[0] instanceof StaticProfile)) {
					LOGGER.log(Level.WARNING, "EMPTY PROFILE DATA!");
					process(ServerTrans.PROFILE_INVALID);
				}
				mDroidManager.addDroid(mConnection.getDroidID(), (StaticProfile) obj[0]);
				mDroidManager.store(new File(Settings.getString("droiddb.file")));
				// send ACCEPT_CONNECTION to Droid
				mConnection.sendMessage(new Message(Instruction.ACCEPT_CONNECTION));
				process(ServerTrans.PROFILE_VALID);
			}
			catch (IOException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			}
		}
	}

	/**
	 * Connects droid to DroidManager.
	 */
	protected class ClientConnectedHandler extends ActionHandler {

		public ClientConnectedHandler() {
		}

		@Override
		public void handle(final Object... o) {
			gotCalled();
			mDroidManager.connectDroid(mConnection.getDroidID(), mConnection);
			mJobDistIO.onDroidConnected(mConnection.getDroidID(), mConnection);
			// Init ping timer
			mPingTimer = new Timer();
			mPingTimerTask = new PingTimerTask(mConnection);
			mPingTimer.scheduleAtFixedRate(mPingTimerTask, 3000, 3000);
		}
	}

	/**
	 * Invoked if pong is received. Simply clears PingTimerTask flag to indicate
	 * client is still alive.
	 */
	protected class PongHandler extends ActionHandler {

		public PongHandler() {
		}

		@Override
		public void handle(Object... obj) {
			mPingTimerTask.clearFlag();
		}
	}

	/**
	 * Gets called if client should be rejected.
	 */
	private class ConnectionRejectedHandler extends ActionHandler {

		@Override
		public void handle(final Object... o) {
			gotCalled();
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
	private class ClientDisconnectedHandler extends ActionHandler {

		@Override
		public void handle(final Object... o) {
			gotCalled();
			if (mPingTimer != null) {
				mPingTimerTask.cancel();
				mPingTimer.cancel();
			}
			mJobDistIO.getCurrentScheduler().onDroidError(mConnection.getDroidID(), DistributedJobError.DROID_LOST);
			mDroidManager.disconnectDroid(mConnection.getDroidID());
			// TODO: close socket?
		}
	}

	/**
	 * Compares received check code from droid with server generated one.
	 */
	private class ValidateCheckcodeHandler extends ActionHandler {

		@Override
		public void handle(final Object... data) {
			gotCalled();
			if (mDroidManager.validateCheckCode((String) data[1], (String) data[0])) {
				process(ServerTrans.CHECKCODE_VALID, data[0]);
			}
			else {
				try {
					mConnection.sendMessage(new Message(Instruction.INVALID_CHECKCODE));
					process(ServerTrans.CHECKCODE_INVALID, data[0]);
				}
				catch (IOException ex) {
					Logger.getLogger(ServerStateMachine.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		}
	}

	/**
	 * Requests profile without check.
	 */
	private class ProfileRequestHandler extends ActionHandler {

		@Override
		public void handle(final Object... o) {
			gotCalled();
			gotCalled();
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
	private class CheckCodeRequestedHandler extends ActionHandler {

		@Override
		public void handle(final Object... data) {
			gotCalled();
			// generate 6digit string
			final SecureRandom random = new SecureRandom();
			final byte[] byteCode = new byte[3];
			random.nextBytes(byteCode);

			StringBuffer buf = new StringBuffer();
			int len = byteCode.length;
			for (int i = 0; i < len; i++) {
				Utilities.byte2hex(byteCode[i], buf);
			}
			mDroidManager.showCheckCode((String) data[1], buf.toString(), (String) data[0]);
		}
	}

	/**
	 * Gets called if JOB_DONE was received.
	 */
	protected class ResultHandler extends ActionHandler {

		public ResultHandler() {
		}

		@Override
		public void handle(final Object... obj) {
			assert obj[0] instanceof String;
			gotCalled();

			ObjectInputStream objInstream;
			Object object = null;
			try {
				objInstream = new ClassloaderObjectInputStream(
								new ByteArrayInputStream((byte[]) obj[1]),
								mJobDistIO.getCDBLoader().getClassLoader((String) obj[0])); // TODO...
				object = objInstream.readObject();
				objInstream.close();
//      obj = new ClassloaderObjectInputStream(new ByteArrayInputStream(lastUnserializedJob), mClassLoaderWrapper).readObject();
			}
			catch (OptionalDataException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			}
			catch (ClassNotFoundException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			}
			catch (IOException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			}

			CandisLog.v(TAG, "Getting result...");
			DistributedJobResult result = (DistributedJobResult) object;


			// Handle result
			mJobDistIO.onJobDone(mConnection.getDroidID(), result);
		}
	}

	/**
	 * Sends binary file to droid.
	 */
	protected class SendBinaryHandler extends ActionHandler {

		public SendBinaryHandler() {
		}

		@Override
		public void handle(final Object... binary) {
			assert binary[0] instanceof String;
			assert binary.length == 1;

			gotCalled();
			CandisLog.v(TAG, "Sending binary for task ID " + mJobDistIO.getCurrentTaskID());
			try {
				final File file = (File) mJobDistIO.getCDBLoader().getDroidBinary((String) binary[0]);// TODO:..
				int nRead;
				byte[] data = new byte[16384];
				byte[] outdata;// = new byte[(int) file.length()];

				// convert file to byte-array
				final InputStream is = new FileInputStream(file);
				final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

				while ((nRead = is.read(data, 0, data.length)) != -1) {
					buffer.write(data, 0, nRead);
				}
				buffer.flush();
				outdata = buffer.toByteArray();

				mConnection.sendMessage(
								new Message(Instruction.SEND_BINARY,
														mJobDistIO.getCurrentTaskID(),
														outdata));
			}
			catch (IOException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			}
		}
	}

	/**
	 * Sends the initial parameter to the droid. arguments: none (called by ACK)
	 */
	protected class SendInitialParameterHandler extends ActionHandler {

		public SendInitialParameterHandler() {
		}

		@Override
		public void handle(final Object... param) {
//			assert param.length == 0;
//			assert param[0] instanceof String;
//			assert param[1] instanceof DistributedJobParameter;

			gotCalled();
			CandisLog.v(TAG, "Sending initial parameter for task ID " + mJobDistIO.getCurrentTaskID());
			try {
				assert mJobDistIO.getCurrentScheduler().getInitialParameter() != null;
				mConnection.sendMessage(new Message(Instruction.SEND_INITIAL, mJobDistIO.getCurrentTaskID(), mJobDistIO.getCurrentScheduler().getInitialParameter()));
			}
			catch (IOException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			}
		}
	}

	/**
	 * Sends the job to the droid.
	 */
	protected class SendJobHandler extends ActionHandler {

		public SendJobHandler() {
		}

		@Override
		public void handle(final Object... params) {
			assert params[0] instanceof String;
			assert params[1] instanceof DistributedJobParameter;
			assert params.length == 2;

			gotCalled();
			CandisLog.v(TAG, "Sending job for task ID " + params[0]);

			// Serialize to byte array
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos;
			try {
				oos = new ObjectOutputStream(baos);
				oos.writeObject(params[1]);
				oos.close();
			}
			catch (IOException ex) {
				Logger.getLogger(ServerStateMachine.class.getName()).log(Level.SEVERE, null, ex);
			}
			byte[] bytes = baos.toByteArray();

			// send job
			try {
				mConnection.sendMessage(new Message(
								Instruction.SEND_JOB,
								(String) params[0],
								bytes));
			}
			catch (IOException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			}
		}
	}

	/**
	 * Sends PING to client.
	 */
	private class SendPingHandler extends ActionHandler {

		@Override
		public void handle(Object... obj) {
			throw new UnsupportedOperationException("Not supported yet.");
		}
	}
}
