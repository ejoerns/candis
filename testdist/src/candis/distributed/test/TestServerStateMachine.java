package candis.distributed.test;

import candis.common.Instruction;
import candis.common.Message;
import candis.common.fsm.ActionHandler;
import candis.server.Connection;
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

	public TestServerStateMachine(final Connection connection, final DroidManager droidManager, final JobDistributionIOServer comIO) {
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
								new Message(Instruction.SEND_BINARY, String.format("%05d",taskID), null));
			}
			catch (IOException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			}
		}
	}
}
