package candis.distributed.test;

import candis.common.fsm.ActionHandler;
import candis.server.ClientConnection;
import candis.server.DroidManager;
import candis.server.JobDistributionIOServer;
import candis.server.ServerStateMachine;

/**
 *
 * @author Sebastian Willenborg
 */
public class TestServerStateMachine extends ServerStateMachine {

	//private static final Logger LOGGER = Logger.getLogger(TestServerStateMachine.class.getName());

	public TestServerStateMachine(final ClientConnection connection, final DroidManager droidManager, final JobDistributionIOServer comIO) {
		super(connection, droidManager, comIO);
	}

	@Override
	public void init() {
		super.init();
	}

	private class ClientConnectedHandler implements ActionHandler {

		@Override
		public void handle(final Object... o) {
			mDroidManager.connectDroid((String) o[0], mConnection);
			mJobDistIO.onDroidConnected((String) o[0], mConnection);
		}
	}
}
