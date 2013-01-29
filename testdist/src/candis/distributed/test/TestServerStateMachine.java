package candis.distributed.test;

import candis.common.Instruction;
import candis.common.fsm.StateMachineException;
import candis.server.ClientConnection;
import candis.server.DroidManager;
import candis.server.JobDistributionIOServer;
import candis.server.ServerStateMachine;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Sebastian Willenborg
 */
public class TestServerStateMachine extends ServerStateMachine {

	private static final Logger LOGGER = Logger.getLogger(TestServerStateMachine.class.getName());
	private boolean inited = false;

	public TestServerStateMachine(final ClientConnection connection, final DroidManager droidManager, final JobDistributionIOServer comIO) {
		super(connection, droidManager, comIO);
	}

	@Override
	public void init() {
		if (inited) {
			return;
		}
		inited = true;
		addState(ServerStates.UNCONNECTED)
						.addTransition(
						ServerTrans.CLIENT_NEW,
						ServerStates.CONNECTED,
						new ClientConnectedHandler());
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
						Instruction.REQUEST_INITIAL,
						ServerStates.JOB_INIT_SENT,
						new SendInitialParameterHandler());
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

		addGlobalTransition(
						Instruction.PONG,
						null,
						new PongHandler());

		setState(ServerStates.UNCONNECTED);
		try {
			process(ServerTrans.CLIENT_NEW);

		}
		catch (StateMachineException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
	}

	public void stop() {
		if (mPingTimer != null) {
			mPingTimer.cancel();
		}
		if (mPingTimerTask != null) {
			mPingTimerTask.cancel();
		}
	}
}
