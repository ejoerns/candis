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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Sebastian Willenborg
 */
public class TestServerStateMachine extends FSM {

	private final Connection mConnection;
	private final DroidManager mDroidManager;
	private final ServerCommunicationIO mCommunicationIO;
	private static final Logger LOGGER = Logger.getLogger(TestServerStateMachine.class.getName());

	private enum TestServerStates implements StateEnum {

		UNCONNECTED,
		CONNECTED,
		BINARY_SENT,
		BINARY_SENT_DONE,
		INIT_SENT,
		INIT_SENT_DONE,
		JOB_SENT,
		JOB_SENT_DONE;
	}

	public enum TestServerTrans implements Transition {

		CLIENT_CONNECTED,
		CLIENT_DISCONNECTED,
		GOT_RESULT;
	}

	public TestServerStateMachine(final Connection connection, final DroidManager droidManager, final ServerCommunicationIO comIO) {
		super();
		mConnection = connection;
		mDroidManager = droidManager;
		mCommunicationIO = comIO;
		init();
	}

	private void init() {
		addState(TestServerStates.UNCONNECTED)
						.addTransition(
						TestServerTrans.CLIENT_CONNECTED,
						TestServerStates.CONNECTED,
						new ClientConnectedHandler());

		addState(TestServerStates.CONNECTED)
						.addTransition(
						Instruction.SEND_BINARY,
						TestServerStates.BINARY_SENT,
						null);

		addState(TestServerStates.BINARY_SENT)
						.addTransition(
						Instruction.ACK,
						TestServerStates.BINARY_SENT_DONE,
						new ClientBinarySentHandler());

		addState(TestServerStates.BINARY_SENT_DONE)
						.addTransition(
						Instruction.SEND_INITAL,
						TestServerStates.INIT_SENT,
						null)
						.addTransition(
						Instruction.SEND_BINARY,
						TestServerStates.BINARY_SENT,
						null);

		addState(TestServerStates.INIT_SENT)
						.addTransition(
						Instruction.ACK,
						TestServerStates.INIT_SENT_DONE,
						new ClientInitalParameterSentHandler());

		addState(TestServerStates.INIT_SENT_DONE)
						.addTransition(
						Instruction.SEND_JOB,
						TestServerStates.JOB_SENT,
						null)
						.addTransition(
						Instruction.SEND_BINARY,
						TestServerStates.BINARY_SENT,
						null)
						.addTransition(
						Instruction.SEND_INITAL,
						TestServerStates.INIT_SENT,
						null);

		addState(TestServerStates.JOB_SENT)
						.addTransition(
						Instruction.ACK,
						TestServerStates.JOB_SENT_DONE,
						null);

		addState(TestServerStates.JOB_SENT_DONE)
						.addTransition(
						Instruction.SEND_RESULT,
						TestServerStates.INIT_SENT_DONE,
						new ClientJobDonedHandler());

		setState(TestServerStates.UNCONNECTED);

	}

	private class ClientConnectedHandler implements ActionHandler {

		@Override
		public void handle(final Object o) {
			mCommunicationIO.onDroidConnected((String) o, mConnection);
		}
	}

	private class ClientJobDonedHandler implements ActionHandler {

		@Override
		public void handle(final Object o) {
			mCommunicationIO.onJobDone(mConnection.getDroidID(), (DistributedResult) o);
		}
	}

	private class ClientBinarySentHandler implements ActionHandler {

		@Override
		public void handle(final Object o) {
			mCommunicationIO.onBinarySent(mConnection.getDroidID());
		}
	}

	private class ClientInitalParameterSentHandler implements ActionHandler {

		@Override
		public void handle(final Object o) {
			mCommunicationIO.onInitalParameterSent(mConnection.getDroidID());
		}
	}
}
