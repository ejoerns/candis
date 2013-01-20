package candis.server;

import candis.common.ClassLoaderWrapper;
import candis.common.MessageConnection;
import candis.common.Instruction;
import candis.common.Message;
import candis.common.fsm.FSM;
import candis.common.fsm.StateMachineException;
import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Enrico Joerns
 */
public class ClientConnection extends MessageConnection implements Runnable {

	private static final Logger LOGGER = Logger.getLogger(ClientConnection.class.getName());
	protected final DroidManager mDroidManager;
	protected final JobDistributionIOServer mJobDistIO;
	protected FSM mStateMachine = null;
	private boolean isStopped;
	private String droidID;

	public ClientConnection(
					final Socket socket,
					final DroidManager droidmanager,
					final JobDistributionIOServer jobDistIO) {
		super(socket, new ClassLoaderWrapper()); // TODO...
		mDroidManager = droidmanager;
		mJobDistIO = jobDistIO;
		mStateMachine = new ServerStateMachine(this, mDroidManager, mJobDistIO);
	}

	public FSM getStateMachine() {
		return mStateMachine;
	}

	public void setDroidID(final String droidID) {
		this.droidID = droidID;
	}

	public String getDroidID() {
		return droidID;
	}

	@Override
	public void run() {
		initConnection();

		// Handle incoming client requests
		while ((!isStopped) && (!isSocketClosed())) {
//			try {

			Message rec_msg = new Message(Instruction.NO_MSG);
			try {
				rec_msg = readMessage();
			}
			catch (IOException ex) {
				Logger.getLogger(ClientConnection.class.getName()).log(Level.SEVERE, null, ex);
			}

			LOGGER.log(Level.INFO, "Client request: {0}", rec_msg.getRequest());
			try {
				if (rec_msg.getRequest().len == 0) {
					mStateMachine.process(rec_msg.getRequest());
				}
				else {
					mStateMachine.process(rec_msg.getRequest(), (Object[]) rec_msg.getData());

				}
			}
			catch (StateMachineException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			}
//			}
//			catch (EOFException ex) {
//				LOGGER.log(Level.SEVERE, "EOF detected, closing socket ...");
//				try {
//					mStateMachine.process(ServerStateMachine.ServerTrans.CLIENT_DISCONNECTED);
//				}
//				catch (StateMachineException ex1) {
//					LOGGER.log(Level.SEVERE, null, ex1);
//				}
//				// terminate connection to client
//				isStopped = true;
//				try {
//					closeSocket();
//				}
//				catch (IOException e) {
//					LOGGER.log(Level.SEVERE, null, ex);
//				}
//			}
//			catch (InterruptedIOException ex) {
//				isStopped = true;
//				try {
//					closeSocket();
//				}
//				catch (IOException e) {
//					LOGGER.log(Level.SEVERE, null, ex);
//				}
//			}

		}
	}
}
