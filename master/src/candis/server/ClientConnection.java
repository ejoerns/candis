package candis.server;

import candis.common.Message;
import candis.common.MessageConnection;
import candis.common.QueuedMessageConnection;
import candis.common.fsm.FSM;
import candis.common.fsm.StateMachineException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Enrico Joerns
 */
public class ClientConnection implements Runnable {

	private static final Logger LOGGER = Logger.getLogger(ClientConnection.class.getName());
	private boolean isStopped;
	private String droidID;
	private Socket mSocket;
	private List<Receiver> receivers = new LinkedList<Receiver>();
	private QueuedMessageConnection mQueuedMessageConnection;

	public enum Status {

		CONNECTED,
		DISCONNECTED
	}

	public ClientConnection(
					final Socket socket,
					final DroidManager droidmanager,
					final JobDistributionIOServer jobDistIO) throws IOException {
		mSocket = socket;
		mQueuedMessageConnection = new QueuedMessageConnection(mSocket);
	}

	public ClientConnection(
					final InputStream in,
					final OutputStream out,
					final DroidManager droidmanager,
					final JobDistributionIOServer jobDistIO) throws IOException {
		mQueuedMessageConnection = new QueuedMessageConnection(in, out);// TODO...
	}

	public void setDroidID(final String droidID) {
		this.droidID = droidID;
	}

	public String getDroidID() {
		return droidID;
	}

	@Override
	public void run() {

		Thread receiver = new Thread(new Runnable() {
			@Override
			public void run() {
				// init state machine

				// Handle incoming client requests
				while ((!isStopped) && (!mSocket.isClosed())) {
					Message msg;
					try {
						msg = mQueuedMessageConnection.readMessage();
					}
					catch (InterruptedIOException ex) {
						LOGGER.info("ClientConnection thread interrupted");
						isStopped = true;
						break;
					}
					catch (SocketException ex) {
						LOGGER.warning("Socket ist closed, message will not be sent");
						isStopped = true;
						continue;
					}
					catch (IOException ex) {
						LOGGER.warning(ex.getMessage());
						//LOGGER.log(Level.SEVERE, null, ex);
						isStopped = true;
						break;
					}
					// notify listeners
					for (Receiver r : receivers) {
						r.OnNewMessage(msg);
					}

					LOGGER.log(Level.INFO, "Client request: {0}", msg.getRequest());
				}

				notifyListeners(Status.DISCONNECTED);
			}
		});
		receiver.start();

		mQueuedMessageConnection.run();

		receiver.interrupt();
	}

	/**
	 * Sends Message if connected.
	 *
	 * @param msg Message to send
	 */
	public final void sendMessage(Message msg) {
		try {
			mQueuedMessageConnection.sendMessage(msg);
		}
		catch (IOException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * Adds a receiver that will receive messages and connection status updates.
	 *
	 * @param rec Receiver to add
	 */
	public void addReceiver(Receiver rec) {
		if (rec != null) {
			System.out.println("Adding receiver... " + rec);
			receivers.add(rec);
		}
	}

	/**
	 * Notify listeners about status update.
	 *
	 * @param status Status to promote
	 */
	private void notifyListeners(Status status) {
		for (Receiver r : receivers) {
			System.out.println("Notifying... " + r);
			r.OnStatusUpdate(status);
		}
	}

	/*
	 * 
	 */
	public interface Receiver {

		/**
		 * Invoked when new message was received.
		 *
		 * @param msg
		 */
		public abstract void OnNewMessage(Message msg);

		/**
		 * Invoked when connection status changes.
		 *
		 * @param status
		 */
		public abstract void OnStatusUpdate(Status status);
	}
}
