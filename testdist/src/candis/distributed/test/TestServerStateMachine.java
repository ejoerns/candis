package candis.distributed.test;

import candis.common.Instruction;
import candis.common.Message;
import candis.common.fsm.ActionHandler;
import candis.server.ClientConnection;
import candis.server.DroidManager;
import candis.server.JobDistributionIOServer;
import candis.server.ServerStateMachine;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Sebastian Willenborg
 */
public class TestServerStateMachine extends ServerStateMachine {

	private static final Logger LOGGER = Logger.getLogger(TestServerStateMachine.class.getName());

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
			mCommunicationIO.onDroidConnected((String) o[0], mConnection);
		}
	}

	/**
	 * Sends binary file to droid.
	 */
	public class SendBinaryHandler implements ActionHandler {

		@Override
		public void handle(final Object... binary) {
			System.out.println("SendBinaryHandler() called");
			try {

				// ID is currently just a serial number
				taskID++;

				mConnection.sendMessage(
								new Message(Instruction.SEND_BINARY, String.format("%05d", taskID), null));
			}
			catch (IOException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			}
		}
	}
}
