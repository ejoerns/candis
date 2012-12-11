package candis.distributed.test;

import candis.common.Instruction;
import candis.common.fsm.ActionHandler;
import candis.common.fsm.FSM;
import candis.common.fsm.StateEnum;
import candis.common.fsm.Transition;
import candis.distributed.CommunicationIO;
import candis.distributed.DistributedResult;
import candis.server.Connection;
import candis.server.DroidManager;
import candis.server.ServerCommunicationIO;
import candis.server.ServerStateMachine;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Sebastian Willenborg
 */
public class TestServerStateMachine extends ServerStateMachine {

	//private static final Logger LOGGER = Logger.getLogger(TestServerStateMachine.class.getName());

	public TestServerStateMachine(final Connection connection, final DroidManager droidManager, final ServerCommunicationIO comIO) {
		super(connection, droidManager, comIO);
	}

	@Override
	protected void init() {
		addState(ServerStates.UNCONNECTED)
						.addTransition(
						ServerTrans.CLIENT_NEW,
						ServerStates.CONNECTED,
						new ClientConnectedHandler());

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
						ServerStateMachine.ServerTrans.SEND_INITAL,
						ServerStates.INIT_SENT,
						new SendInitialParameterHandler())
						.addTransition(
						Instruction.SEND_BINARY,
						ServerStates.BINARY_SENT,
						new SendBinaryHandler());

		addState(ServerStates.INIT_SENT)
						.addTransition(
						Instruction.ACK,
						ServerStates.INIT_SENT_DONE,
						new ClientInitalParameterSentHandler());

		addState(ServerStates.INIT_SENT_DONE)
						.addTransition(
						ServerStateMachine.ServerTrans.SEND_JOB,
						ServerStates.JOB_SENT,
						new SendJobHandler())
						.addTransition(
						ServerStateMachine.ServerTrans.SEND_BINARY,
						ServerStates.BINARY_SENT,
						new SendBinaryHandler())
						.addTransition(
						ServerStateMachine.ServerTrans.SEND_INITAL,
						ServerStates.INIT_SENT,
						new SendInitialParameterHandler());

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

		setState(ServerStates.UNCONNECTED);

	}

	private class ClientConnectedHandler implements ActionHandler {

		@Override
		public void handle(final Object o) {
			mCommunicationIO.onDroidConnected((String) o, mConnection);
		}
	}
}
