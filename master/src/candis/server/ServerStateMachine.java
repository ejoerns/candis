package candis.server;

import candis.common.CandisLog;
import candis.common.ClassloaderObjectInputStream;
import candis.common.DroidID;
import candis.common.Instruction;
import candis.common.Message;
import candis.common.fsm.ActionHandler;
import candis.common.fsm.FSM;
import candis.common.fsm.StateEnum;
import candis.common.fsm.Transition;
import candis.distributed.DistributedJobParameter;
import candis.distributed.DistributedJobResult;
import candis.distributed.droid.StaticProfile;
import candis.server.ClientConnection.Status;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of the server side communication and interaction protocol FSM.
 *
 * @author Enrico Joerns
 */
public class ServerStateMachine extends FSM implements ClientConnection.Receiver, DroidManager.DroidHandler {

	private static final String TAG = ServerStateMachine.class.getName();
	private static final Logger LOGGER = Logger.getLogger(TAG);
	/// Connection used for sending data
	protected final ClientConnection mConnection;
	/// Droid manager used for managing connections
	protected final DroidManager mDroidManager;
	/// Job distributor used for managing tasks and jobs
	protected final JobDistributionIOServer mJobDistIO;
	/// Holds last states for known droid to allow restore of last state.
	private static final Map<String, StateEnum> mDroidStateMap = new HashMap<String, StateEnum>();
	// Holds device profile of droid
	private StaticProfile mReceivedProfile;
	// ID of associated Droid
	private String mDroidID;

	/**
	 * All availbale states for the FSM.
	 */
	protected enum ServerStates implements StateEnum {

		INITIAL,
		REGISTERED,
		//		UNCONNECTED,
		CHECK,
		//		PROFILE_REQUESTED,
		//		PROFILE_VALIDATE,
		CHECKCODE_REQUESTED,
		CHECKCODE_VALIDATE,
		CONNECTED,
		JOB_SENT,
		JOB_BINARY_SENT,
		//		JOB_INIT_SENT,
		INIT_SENT,
		//		INIT_BINARY_SENT,
		JOB_PROCESSING;
	}

	/**
	 * All server-side transition for the FSM.
	 */
	protected enum ServerTrans implements Transition {

		CLIENT_BLACKLISTED,
		// (droidID, checkcodeID)
		REQUEST_CHECKCODE,
		CLIENT_ACCEPTED,
		CLIENT_REJECTED,
		CHECKCODE_VALID,
		CHECKCODE_INVALID,
		//		PROFILE_VALID,
		//		PROFILE_INVALID,
		//		POST_JOB,
		CLIENT_INVALID,
		CLIENT_DISCONNECTED,
		SEND_JOB,
		SEND_INITAL,// TODO...
		STOP_JOB,// TODO...
		//		SEND_PING,
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
		addState(ServerStates.INITIAL)
						.addTransition(
						Instruction.REGISTER,
						ServerStates.CHECK,
						new RegistrationRequestHandler());
		addState(ServerStates.CHECK)
						.addTransition(
						ServerTrans.CLIENT_BLACKLISTED,
						ServerStates.INITIAL,
						null)
						.addTransition(
						ServerTrans.REQUEST_CHECKCODE,
						ServerStates.CHECKCODE_REQUESTED,
						new CheckCodeRequestHandler())
						.addTransition(
						ServerTrans.CLIENT_ACCEPTED,
						ServerStates.REGISTERED,
						new SendAckHandler())
						.addTransition(
						ServerTrans.CLIENT_INVALID,
						ServerStates.INITIAL,
						null);
		addState(ServerStates.CHECKCODE_REQUESTED)
						.addTransition(
						Instruction.SEND_CHECKCODE,
						ServerStates.CHECKCODE_VALIDATE,
						new CheckcodeReceivedHandler());
		addState(ServerStates.CHECKCODE_VALIDATE)
						.addTransition(
						ServerTrans.CHECKCODE_VALID,
						ServerStates.REGISTERED,
						new SendAckHandler())
						.addTransition(
						ServerTrans.CHECKCODE_INVALID,
						ServerStates.INITIAL,
						new SendNackHandler());
		addState(ServerStates.REGISTERED)
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
						ServerStates.JOB_PROCESSING,
						null);
		addState(ServerStates.JOB_PROCESSING)
						.addTransition(
						Instruction.SEND_RESULT,
						ServerStates.REGISTERED,
						new ResultHandler());
		addState(ServerStates.INIT_SENT)
						.addTransition(
						Instruction.ACK,
						ServerStates.REGISTERED);

		setState(ServerStates.INITIAL);
	}

	/**
	 * Invoked if server got connection from a client.
	 */
	private class RegistrationRequestHandler extends ActionHandler {

		@Override
		public void handle(Object... obj) {
			assert obj != null;
			assert obj[0] instanceof DroidID : ((Instruction) obj[0]).toString();
			assert obj[1] instanceof StaticProfile : ((Instruction) obj[1]).toString();

			gotCalled();
			if (obj == null) {
				LOGGER.log(Level.WARNING, "Missing payload data (expected RandomID)");
				return;
			}

			mDroidID = ((DroidID) obj[0]).toSHA1();
			mReceivedProfile = ((StaticProfile) obj[1]);

			// register...
			mDroidManager.register(mDroidID, mReceivedProfile, ServerStateMachine.this);
		}
	}

	/**
	 * Gets called if client should be rejected.
	 */
	private class SendNackHandler extends ActionHandler {

		@Override
		public void handle(final Object... data) {
			gotCalled();
			mConnection.sendMessage(new Message(Instruction.NACK));
		}
	}

	/**
	 * Gets called if client should be rejected.
	 */
	private class SendAckHandler extends ActionHandler {

		@Override
		public void handle(final Object... data) {
			gotCalled();
			mConnection.sendMessage(new Message(Instruction.ACK));
		}
	}

	/**
	 * Compares received check code from droid with server generated one.
	 */
	private class CheckcodeReceivedHandler extends ActionHandler {

		@Override
		public void handle(final Object... data) {
			gotCalled();
			mDroidManager.verifyCheckcode((String) data[0], (String) data[1]);
		}
	}

	/**
	 * Generates new check code (6 digits) and shows it via DroidManager.
	 */
	private class CheckCodeRequestHandler extends ActionHandler {

		@Override
		public void handle(final Object... data) {
			gotCalled();
			mConnection.sendMessage(new Message(Instruction.REQUEST_CHECKCODE));
		}
	}

	/**
	 * Gets called if JOB_DONE was received.
	 */
	protected class ResultHandler extends ActionHandler {

		@Override
		public void handle(final Object... obj) {
			assert obj[0] instanceof String;
			gotCalled();

			ObjectInputStream objInstream;
			Object object = null;
			try {
				objInstream = new ClassloaderObjectInputStream(
								new ByteArrayInputStream((byte[]) obj[2]),
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
			mJobDistIO.onJobDone(mDroidID, result);
		}
	}

	/**
	 * Sends binary file to droid.
	 */
	protected class SendBinaryHandler extends ActionHandler {

		@Override
		public void handle(final Object... data) {
			assert data[0] instanceof String;
			assert data.length == 1;

			gotCalled();
			CandisLog.v(TAG, "Sending binary for task ID " + mJobDistIO.getCurrentTaskID());
			if (!((String) data[0]).equals(mJobDistIO.getCurrentTaskID())) {
				CandisLog.e(TAG, "Invalid task ID " + (String) data[0]);
			}
			try {
				final File file = (File) mJobDistIO.getCDBLoader().getDroidBinary((String) data[0]);
				// file to byte[]
				final RandomAccessFile rfile = new RandomAccessFile(file, "r");
				final byte[] buffer = new byte[(int) file.length()];
				rfile.read(buffer);

				mConnection.sendMessage(
								new Message(Instruction.SEND_BINARY,
														mJobDistIO.getCurrentTaskID(),
														buffer, // runnable
														mJobDistIO.getCurrentScheduler().getInitialParameter())); // initial
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

		@Override
		public void handle(final Object... param) {
//			assert param.length == 0;
//			assert param[0] instanceof String;
//			assert param[1] instanceof DistributedJobParameter;

			gotCalled();
			CandisLog.v(TAG, "Sending initial parameter for task ID " + mJobDistIO.getCurrentTaskID());
			assert mJobDistIO.getCurrentScheduler().getInitialParameter() != null;
			mConnection.sendMessage(new Message(Instruction.SEND_INITIAL, mJobDistIO.getCurrentTaskID(), mJobDistIO.getCurrentScheduler().getInitialParameter()));
		}
	}

	/**
	 * Sends the job to the droid.
	 */
	protected class SendJobHandler extends ActionHandler {

		@Override
		public void handle(final Object... params) {
			assert params[0] instanceof String;
			assert params[1] instanceof String;
			assert params[2] instanceof DistributedJobParameter;
			assert params.length == 3;

			gotCalled();
			CandisLog.v(TAG, "Sending job for task ID " + params[0]);

			// Serialize to byte array
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos;
			try {
				oos = new ObjectOutputStream(baos);
				oos.writeObject(params[2]);
				oos.close();
			}
			catch (IOException ex) {
				Logger.getLogger(ServerStateMachine.class.getName()).log(Level.SEVERE, null, ex);
			}
			byte[] bytes = baos.toByteArray();

			// send job
			mConnection.sendMessage(new Message(
							Instruction.SEND_JOB,
							(String) params[0],// runnableID
							(String) params[1],// jobID
							bytes));
		}
	}

	//--
	@Override
	public void OnNewMessage(Message msg) {
		if (msg.getData() == null) {
			Logger.getLogger(ServerStateMachine.class.getName()).log(Level.SEVERE, "Got Message: " + msg.getRequest());
			process(((Message) msg).getRequest());
		}
		else {
			Logger.getLogger(ServerStateMachine.class.getName()).log(Level.SEVERE, "Got Message: " + msg.getRequest());
			process(msg.getRequest(), (Object[]) msg.getData());
		}
	}

	@Override
	public void OnStatusUpdate(Status status) {
	}

	//-- DroidManger Droid events
	@Override
	public void onSendJob(String taskID, String jobID, DistributedJobParameter param) {
		System.out.println("onSendJob(" + taskID + ", " + jobID + ", " + param + ")");
		process(ServerTrans.SEND_JOB, taskID, jobID, param);
	}

	@Override
	public void onStopJob(String jobID, String taskID) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void onAccepted() {
		process(ServerTrans.CLIENT_ACCEPTED);
	}

	@Override
	public void onRejected() {
		process(ServerTrans.CLIENT_REJECTED);
	}

	@Override
	public void onRequireCheckcode() {
		process(ServerTrans.REQUEST_CHECKCODE);
	}
}
