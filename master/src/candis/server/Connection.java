package candis.server;

import candis.common.ClassLoaderWrapper;
import candis.common.Message;
import candis.common.fsm.FSM;
import candis.common.fsm.StateMachineException;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
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
	// each connection has its own state machine
	protected final DroidManager mDroidManager;
	protected final JobDistributionIOServer mCommunicationIO;
	protected FSM mStateMachine = null;
	protected ObjectOutputStream oos = null;
	protected ObjectInputStream ois = null;
	private String droidID;
	private boolean isStopped;

	public Connection(
					final Socket socket,
					final DroidManager droidmanager,
					final JobDistributionIOServer comIO) {
		mSocket = socket;
		mDroidManager = droidmanager;
		mCommunicationIO = comIO;
	}

	public FSM getStateMachine() {
		return mStateMachine;
	}

	public void sendMessage(final Message msg) throws IOException {
		LOGGER.fine(String.format("##SEND to %s:%s: %s",
															mSocket.getInetAddress(),
															mSocket.getPort(),
															msg.getRequest()));
		oos.writeObject(msg);
		oos.flush();
	}

	protected void initConnection() {
		try {
			LOGGER.log(Level.INFO, "Client {0} connected...", mSocket.getInetAddress());
			oos = new ObjectOutputStream(mSocket.getOutputStream());
			ois = new ClassLoaderObjectInputStream(
							mSocket.getInputStream(),
							mCommunicationIO.getCDBLoader().getClassLoaderWrapper());
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
						mStateMachine.process(rec_msg.getRequest(), (Object[]) rec_msg.getData());
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

	/**
	 * ObjectInputStream that allows to specify custom ClassLoader.
	 */
	private class ClassLoaderObjectInputStream extends ObjectInputStream {

		private final ClassLoaderWrapper mClassLoaderWrapper;

		@Override
		public Class resolveClass(ObjectStreamClass desc) throws IOException,
						ClassNotFoundException {

			try {
				return mClassLoaderWrapper.get().loadClass(desc.getName());
			}
			catch (Exception e) {
				return super.resolveClass(desc);
			}
		}

		public ClassLoaderObjectInputStream(InputStream in, ClassLoaderWrapper cloaderwrap) throws IOException {
			super(in);
			mClassLoaderWrapper = cloaderwrap;
		}
	}
}
