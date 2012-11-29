package candis.server;

import candis.common.Instruction;
import candis.common.Message;
import candis.common.fsm.FSM;
import candis.common.fsm.StateMachineException;
import candis.distributed.CommunicationIO;
import candis.distributed.DistributedParameter;
import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Designed to run in seperate thread for every connected client.
 *
 * @author enrico
 */
public class Connection implements Runnable {

	private static final Logger LOGGER = Logger.getLogger(Server.class.getName());
	private final Socket socket;
//	private ObjectOutputStream oos;
	private boolean isStopped;
	// each connection has its own state machine
	protected FSM fsm = null;
	protected final DroidManager mDroidManager;
	protected final CommunicationIO mCommunicationIO;
	protected ObjectOutputStream oos = null;
	protected ObjectInputStream ois = null;

	public Connection(final Socket socket, final DroidManager droidmanager, final CommunicationIO comIO) {
		this.socket = socket;
		mDroidManager = droidmanager;
		mCommunicationIO = comIO;
	}

	public void sendMessage(final Message msg) throws IOException {
		oos.writeObject(msg);
	}

	public void sendJob(final DistributedParameter param) throws IOException {
		try {
			fsm.process(Instruction.SEND_JOB);
			sendMessage(new Message(Instruction.SEND_JOB, param));
		}
		catch (StateMachineException ex) {
			Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	protected void initConnection() {
		try {
			LOGGER.log(Level.INFO, "Client {0} connected...", socket.getInetAddress());
			oos = new ObjectOutputStream(socket.getOutputStream());
			ois = new ObjectInputStream(socket.getInputStream());
			fsm = new ServerStateMachine(this, mDroidManager, mCommunicationIO);
			if (ois == null) {
				LOGGER.log(Level.SEVERE, "Failed creating Input/Output stream!");
			}
		}
		catch (IOException ex) {
			Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	protected boolean isSocketClosed() {
		return socket.isClosed();
	}

	protected void closeSocket() throws IOException {
		socket.close();
	}

	public String getDroidID() {
		return null;
	}

	@Override
	public void run() {
		initConnection();

		// Handle incoming client requests
		try {
			while ((!isStopped) && (!isSocketClosed())) {
				try {


					Message rec_msg = (Message) ois.readObject();
					LOGGER.log(Level.INFO, "Client request: {0}", rec_msg.getRequest());
					try {
						fsm.process(rec_msg.getRequest(), rec_msg.getData());
					}
					catch (StateMachineException ex) {
						LOGGER.log(Level.SEVERE, null, ex);
					}
				}
				catch (EOFException ex) {
					LOGGER.log(Level.SEVERE, "EOF detected, closing socket ...");
					// TODO: add handler?
					// terminate connection to client
					isStopped = true;
					try {
						closeSocket();
					}
					catch (IOException e) {
						LOGGER.log(Level.SEVERE, null, ex);
					}
				}
				catch (InterruptedIOException ex) {
					isStopped = true;
					try {
						closeSocket();
					}
					catch (IOException e) {
						LOGGER.log(Level.SEVERE, null, ex);
					}
				}

			}
		}
		catch (IOException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
		catch (ClassNotFoundException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
		try {
			LOGGER.log(Level.INFO, "Connection terminated, closing streams");
			oos.close();
			ois.close();
		}
		catch (IOException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
	}
}
