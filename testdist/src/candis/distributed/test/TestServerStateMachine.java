package candis.distributed.test;

import candis.common.Instruction;
import candis.common.fsm.ActionHandler;
import candis.common.fsm.FSM;
import candis.common.fsm.StateEnum;
import candis.common.fsm.Transition;
import candis.server.Connection;
import candis.server.DroidManager;

/**
 *
 * @author Sebastian Willenborg
 */
public class TestServerStateMachine extends FSM {
	private final Connection mConnection;
	private final DroidManager mDroidManager;

	private enum TestServerStates implements StateEnum {

		UNCONNECTED,
		CONNECTED,
		JOB_SENT;
	}

	public enum TestServerTrans implements Transition {

		CLIENT_CONNECTED,
		CLIENT_DISCONNECTED,
		POST_JOB,
		GOT_RESULT;
	}

	public TestServerStateMachine(final Connection connection, final DroidManager droidManager) {
		super();
		mConnection = connection;
		mDroidManager = droidManager;
		init();
	}

	private void init() {
		addState(TestServerStates.UNCONNECTED)
			.addTransition(
			TestServerTrans.CLIENT_CONNECTED,
			TestServerStates.CONNECTED,
			new ClientConnectedHandler())
			;

		addState(TestServerStates.CONNECTED)
			.addTransition(
			TestServerTrans.POST_JOB,
			TestServerStates.JOB_SENT,
			null)
			;

		addState(TestServerStates.JOB_SENT)
			.addTransition(
			Instruction.SEND_JOB,
			TestServerStates.CONNECTED,
			null)
			;

		setState(TestServerStates.UNCONNECTED);

	}

	private class ClientConnectedHandler implements ActionHandler {

		@Override
		public void handle(final Object o) {
			//mDroidManager.connectDroid(mCurrentlyConnectingID, mConnection);
		}
	}

}
