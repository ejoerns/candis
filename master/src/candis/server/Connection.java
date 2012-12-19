package candis.server;

import candis.common.Message;
import candis.common.fsm.FSM;
import candis.common.fsm.StateMachineException;
import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Designed to run in separate thread for every connected client.
 *
 * @author enrico
 */
public class Connection implements Runnable {

	private static final Logger LOGGER = Logger.getLogger(Connection.class.getName());
	private final Socket mSocket;
	private boolean isStopped;
	// each connection has its own state machine
	protected FSM mStateMachine = null;
	protected final DroidManager mDroidManager;
	protected final ServerCommunicationIO mCommunicationIO;
	protected ObjectOutputStream oos = null;
	protected ObjectInputStream ois = null;
	private String droidID;

	public Connection(
					final Socket socket,
					final DroidManager droidmanager,
					final ServerCommunicationIO comIO) {
		mSocket = socket;
		mDroidManager = droidmanager;
		mCommunicationIO = comIO;
	}

	public FSM getStateMachine() {
		return mStateMachine;
	}

	public void sendMessage(final Message msg) throws IOException {
		oos.writeObject(msg);
	}
	
	protected void initConnection() {
		try {
			LOGGER.log(Level.INFO, "Client {0} connected...", mSocket.getInetAddress());
			oos = new ObjectOutputStream(mSocket.getOutputStream());
			ois = new ObjectInputStream(mSocket.getInputStream());
			mStateMachine = new ServerStateMachine(this, mDroidManager, mCommunicationIO);
			if (ois == null) {
				LOGGER.log(Level.SEVERE, "Failed creating Input/Output stream! (null)");
			}
		}
		catch (IOException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
	}

	protected boolean isSocketClosed() {
		return mSocket.isClosed();
	}

	protected void closeSocket() throws IOException {
		mSocket.close();
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
		try {
			while ((!isStopped) && (!isSocketClosed())) {
				try {


					final Message rec_msg = (Message) ois.readObject();

					LOGGER.log(Level.INFO, "Client request: {0}", rec_msg.getRequest());
					try {
						mStateMachine.process(rec_msg.getRequest(), rec_msg.getData());
					}
					catch (StateMachineException ex) {
						LOGGER.log(Level.SEVERE, null, ex);
					}
				}
				catch (EOFException ex) {
					LOGGER.log(Level.SEVERE, "EOF detected, closing socket ...");
					try {
						mStateMachine.process(ServerStateMachine.ServerTrans.CLIENT_DISCONNECTED);
					}
					catch (StateMachineException ex1) {
						LOGGER.log(Level.SEVERE, null, ex1);
					}
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
