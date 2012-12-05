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
		JOB_SENT;
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
						Instruction.SEND_JOB,
						TestServerStates.JOB_SENT,
						null);

		addState(TestServerStates.JOB_SENT)
						.addTransition(
						Instruction.SEND_RESULT,
						TestServerStates.CONNECTED,
						new ClientJobDonedHandler());

		setState(TestServerStates.UNCONNECTED);

	}

	private class ClientConnectedHandler implements ActionHandler {

		@Override
		public void handle(final Object o) {
			//LOGGER.log(Level.INFO, "hilfe!");
			//mDroidManager.connectDroid((String) o, mConnection);
			mCommunicationIO.onDroidConnected((String) o, mConnection);
		}
	}

	private class ClientJobDonedHandler implements ActionHandler {

		@Override
		public void handle(final Object o) {
			//System.out.println(mConnection.getDroidID());
			//System.out.println(o);
			mCommunicationIO.onJobDone(mConnection.getDroidID(), (DistributedResult) o);
		}
	}
}
