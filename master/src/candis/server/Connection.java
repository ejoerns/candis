package candis.server;

import candis.common.Message;
import candis.common.fsm.StateMachineException;
import java.io.EOFException;
import java.io.IOException;
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
	private ServerStateMachine fsm = null;

	public Connection(final Socket socket) throws IOException {
		this.socket = socket;
		LOGGER.log(Level.INFO, "Client {0} connected...", socket.getInetAddress());
	}

	@Override
	public void run() {
		ObjectOutputStream oos = null;
		ObjectInputStream ois = null;

		try {
			oos = new ObjectOutputStream(socket.getOutputStream());
			ois = new ObjectInputStream(socket.getInputStream());
			fsm = new ServerStateMachine(oos);
			if (ois == null) {
				LOGGER.log(Level.SEVERE, "Failed creating Input/Output stream!");
			}
		} catch (IOException ex) {
			Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
		}

		// Handle incoming client requests
		while ((!isStopped) && (!socket.isClosed())) {
			try {

				Message rec_msg = (Message) ois.readObject();
				LOGGER.log(Level.INFO, "Client request: {0}", rec_msg.getRequest());
				try {
					fsm.process(rec_msg.getRequest(), rec_msg.getData());
				} catch (StateMachineException ex) {
					LOGGER.log(Level.SEVERE, null, ex);
				}

				try {
					Thread.sleep(10);// todo: improve
					//
				} catch (InterruptedException ex) {
					LOGGER.log(Level.SEVERE, null, ex);
				}
			} catch (EOFException ex) {
				LOGGER.log(Level.SEVERE, "EOF detected, closing socket ...");
				// TODO: add handler?
				// terminate connection to client
				isStopped = true;
				try {
					socket.close();
				} catch (IOException e) {
					LOGGER.log(Level.SEVERE, null, ex);
				}
			} catch (IOException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			} catch (ClassNotFoundException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			} finally {
			}
		}
		try {
			LOGGER.log(Level.INFO, "Connection terminated, closing streams");
			oos.close();
			ois.close();
		} catch (IOException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
	}
}
