package candis.server;

import candis.common.ClassLoaderWrapper;
import candis.common.Instruction;
import candis.common.Message;
import candis.common.MessageConnection;
import candis.common.fsm.FSM;
import candis.common.fsm.StateMachineException;
import java.io.IOException;
import java.net.Socket;
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
		super(socket, jobDistIO.getCDBLoader().getClassLoaderWrapper()); // TODO...
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

			Message msg = new Message(Instruction.NO_MSG);
			try {
				msg = readMessage();
				try {
					if (msg.getData() == null) {
						System.out.println("getData is NULL");
						mStateMachine.process(msg.getRequest());
					}
					else {
						System.out.println("getData is not NULL");
						mStateMachine.process(msg.getRequest(), (Object[]) (msg.getData()));
					}
				}
				catch (StateMachineException ex) {
					Logger.getLogger(ClientConnection.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
			catch (IOException ex) {
				Logger.getLogger(ClientConnection.class.getName()).log(Level.SEVERE, null, ex);
			}

			LOGGER.log(Level.INFO, "Client request: {0}", msg.getRequest());
		}
	}
}
