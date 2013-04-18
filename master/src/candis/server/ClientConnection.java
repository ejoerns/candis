package candis.server;

import candis.common.Instruction;
import candis.common.Message;
import candis.common.QueuedMessageConnection;
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
	private final List<Receiver> receivers = new LinkedList<Receiver>();
	private final QueuedMessageConnection mQueuedMessageConnection;
	private Socket mSocket;
	private boolean isStopped;

	public enum Status {

		CONNECTED,
		DISCONNECTED
	}

	public ClientConnection(final Socket socket) throws IOException {
		mSocket = socket;
		mQueuedMessageConnection = new QueuedMessageConnection(mSocket);
	}

	public ClientConnection(final InputStream in, final OutputStream out) throws IOException {
		mQueuedMessageConnection = new QueuedMessageConnection(in, out);
	}

	@Override
	public void run() {

		Thread receiver = new Thread(new Runnable() {
			@Override
			public void run() {
				// set keepalive option to detect lost connection
				try {
					mSocket.setKeepAlive(true);
				}
				catch (SocketException ex) {
					Logger.getLogger(ClientConnection.class.getName()).log(Level.SEVERE, null, ex);
				}

				// Handle incoming client requests
				while ((!isStopped) && (!mSocket.isClosed())) {
					Message msg;
					try {
						msg = mQueuedMessageConnection.readMessage();

						// reply on ping message
						if (msg.getRequest() == Instruction.PING) {
							sendMessage(new Message(Instruction.PONG));
							continue;
						}

						// notify listeners
						for (Receiver r : receivers) {
							r.OnNewMessage(msg);
						}
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
		void OnNewMessage(Message msg);

		/**
		 * Invoked when connection status changes.
		 *
		 * @param status
		 */
		void OnStatusUpdate(Status status);
	}
}
