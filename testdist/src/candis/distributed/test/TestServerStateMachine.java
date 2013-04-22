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
		super.init();
	}
}
