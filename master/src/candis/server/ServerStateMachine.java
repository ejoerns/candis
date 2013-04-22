package candis.server;

import candis.common.CandisLog;
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
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
		CHECK,
		CHECKCODE_REQUESTED,
		CHECKCODE_VALIDATE,
		CONNECTED,
		JOB_SENT,
		JOB_BINARY_SENT,
		INIT_SENT,
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
		CLIENT_INVALID,
		CLIENT_DISCONNECTED,
		SEND_JOB,
		SEND_INITAL,// TODO...
		STOP_JOB,// TODO...
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
	public final void init() {
		addState(ServerStates.INITIAL);
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
						new ClientAcceptedHandler())
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
						new ClientAcceptedHandler())
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
						Instruction.NACK,
						ServerStates.INITIAL,
						null) // TODO: handler?
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
						new ResultHandler())
						.addTransition(
						ServerTrans.STOP_JOB,
						ServerStates.REGISTERED,
						new StopJobHandler());
		addState(ServerStates.INIT_SENT)
						.addTransition(
						Instruction.ACK,
						ServerStates.REGISTERED);

		// we can register and unregister from every state
		addGlobalTransition(
						Instruction.REGISTER,
						ServerStates.CHECK,
						new RegistrationRequestHandler());
		addGlobalTransition(
						Instruction.UNREGISTER,
						ServerStates.INITIAL,
						new UnregisterHandler());

		// if something wents wrong, we simply disconenct client :)
		setErrorTransition(
						ServerStates.INITIAL,
						new ErrorHandler());

		setState(ServerStates.INITIAL);
	}

	/**
	 * Invoked if server got connection from a client.
	 */
	private class RegistrationRequestHandler extends ActionHandler {

		@Override
		public void handle(Object... obj) {
			gotCalled();
			final String droidID = ((DroidID) obj[0]).toSHA1();
			final StaticProfile profile = (StaticProfile) obj[1];

			mDroidID = droidID;
			mReceivedProfile = profile;

			// register...
			mDroidManager.register(mDroidID, mReceivedProfile, ServerStateMachine.this);
		}
	}

	/**
	 * Invoked if server got connection from a client.
	 */
	private class UnregisterHandler extends ActionHandler {

		@Override
		public void handle(Object... obj) {
			gotCalled();
			final String droidID = ((DroidID) obj[0]).toSHA1();

			// unregister...
			mDroidManager.unregister(droidID);
		}
	}

	/**
	 * Invoked if an error occurs
	 */
	private class ErrorHandler extends ActionHandler {

		@Override
		public void handle(Object... obj) {
			gotCalled();
			LOGGER.log(Level.SEVERE, "Invalid Transition, resetting StateMachine!");
			// TODO: error feedback?

			// unregister...
			mDroidManager.unregister(mDroidID);
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
	 * Gets called if client should be accepted.
	 */
	private class ClientAcceptedHandler extends ActionHandler {

		@Override
		public void handle(final Object... data) {
			gotCalled();
			mJobDistIO.onDroidConnected(mDroidID);
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
		public final void handle(final Object... obj) {
			final String taskID = (String) obj[0];
			final String jobID = (String) obj[1];
			final byte[] rawresult = (byte[]) (byte[]) obj[2];
			final long exectime = (Long) obj[3];
			gotCalled();

			// assure we have received result for the right task ID
			if (!taskID.equals(mJobDistIO.getCurrentTaskID())) {
				CandisLog.e(TAG, "Received result with invalid Task ID " + taskID);
				return;
			}

			CandisLog.v(TAG, "Deserializing result...");
			final DistributedJobResult[] results = mJobDistIO.getCDBLoader()
							.deserializeJob(taskID, rawresult);

			// Handle result
			mJobDistIO.onJobDone(mDroidID, jobID, results, exectime);
		}
	}

	/**
	 * Sends binary file to droid.
	 */
	protected class SendBinaryHandler extends ActionHandler {

		@Override
		public final void handle(final Object... data) {
			gotCalled();
			final String taskID = (String) data[0];

			CandisLog.v(TAG, "Sending binary for task ID " + mJobDistIO.getCurrentTaskID());
			if (!taskID.equals(mJobDistIO.getCurrentTaskID())) {
				CandisLog.e(TAG, "Invalid task ID " + taskID);
			}

			final byte[] binary = mJobDistIO.getCDBLoader().getDroidBinary((String) data[0]);

			mConnection.sendMessage(
							new Message(Instruction.SEND_BINARY,
													mJobDistIO.getCurrentTaskID(),
													binary, // runnable
													new DistributedJobParameter[]{mJobDistIO.getCurrentScheduler().getInitialParameter()}));
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
		public void handle(final Object... data) {
			String runnableID = (String) data[0];
			String jobID = (String) data[1];
			DistributedJobParameter[] params = (DistributedJobParameter[]) data[2];

			gotCalled();
			CandisLog.v(TAG, "Sending job for task ID " + runnableID);

			// Serialize to byte array
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos;
			try {
				oos = new ObjectOutputStream(baos);
				oos.writeObject(params);
				oos.close();
			}
			catch (IOException ex) {
				Logger.getLogger(ServerStateMachine.class.getName()).log(Level.SEVERE, null, ex);
			}
			byte[] bytes = baos.toByteArray();

			// send job
			mConnection.sendMessage(new Message(
							Instruction.SEND_JOB,
							runnableID,
							jobID,
							bytes));
		}
	}

	private class StopJobHandler extends ActionHandler {

		@Override
		public void handle(final Object... data) {
			gotCalled();
			throw new UnsupportedOperationException("Not supported yet.");
		}
	}
	//--

	@Override
	public void OnNewMessage(Message msg) {
		if (msg.getData() == null) {
			Logger.getLogger(ServerStateMachine.class.getName()).log(Level.FINE, "Got Message: " + msg.getRequest());
			process(((Message) msg).getRequest());
		}
		else {
			Logger.getLogger(ServerStateMachine.class.getName()).log(Level.FINE, "Got Message: " + msg.getRequest());
			process(msg.getRequest(), (Object[]) msg.getData());
		}
	}

	@Override
	public void OnStatusUpdate(Status status) {
	}

	//-- DroidManger Droid events
	@Override
	public void onSendJob(String taskID, String jobID, DistributedJobParameter[] params) {
		System.out.println("onSendJob(" + taskID + ", " + jobID + ", " + params + ")");
		process(ServerTrans.SEND_JOB, taskID, jobID, params);
	}

	@Override
	public void onStopJob(String jobID, String taskID) {
		process(ServerTrans.STOP_JOB, taskID, jobID);
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
