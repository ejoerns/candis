/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package candis.server;

import candis.common.Instruction;
import candis.common.Message;
import candis.common.fsm.FSM;
import candis.common.fsm.State;
import candis.common.fsm.StateMachineException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author enrico
 */
public class Connection implements Runnable {

	private static final Logger logger = Logger.getLogger(Server.class.getName());
	private Socket socket;
	private ObjectInputStream ois;
	private ObjectOutputStream oos;
	private boolean isStopped;
	private FSM ssm;

	public Connection(Socket socket) throws IOException {
		this.socket = socket;
		logger.log(Level.INFO, "Client connected...");
//		ssm = ServerStateMachine.UNCONNECTED;
	}

	@Override
	public void run() {

		try {
			ois = new ObjectInputStream(socket.getInputStream());
			oos = new ObjectOutputStream(socket.getOutputStream());
			if ((ois == null) || (oos == null)) {
				logger.log(Level.SEVERE, "Failed creating Input/Output stream!");
			}
		} catch (IOException ex) {
			Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
		}

		while (!isStopped) {
			try {

				Message rec_msg = (Message) ois.readObject();
				logger.log(Level.INFO, "Client request: {0}", rec_msg.getRequest());
				try {
					ssm.process(rec_msg.getRequest());
				} catch (StateMachineException ex) {
					logger.log(Level.SEVERE, null, ex);
				}

				try {
					Thread.sleep(10);// todo: improve
					//
				} catch (InterruptedException ex) {
					Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
				}
			} catch (IOException ex) {
				logger.log(Level.SEVERE, null, ex);
			} catch (ClassNotFoundException ex) {
				logger.log(Level.SEVERE, null, ex);
			} finally {
				isStopped = true;
				try {
					socket.close();
				} catch (IOException ex) {
					Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		}


//		System.out.println("Transfering file...");
//		File myFile = new File("example_app-debug.apk");
//		byte[] mybytearray = new byte[(int) myFile.length()];
//		try {
//			in = new DataInputStream(socket.getInputStream());
//
//			System.out.println("Done");
//		} catch (FileNotFoundException ex) {
//			Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
//		} catch (IOException ex) {
//			Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
//		} finally {
//			try {
//				socket.close();
//			} catch (IOException ex) {
//				Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
//			}
//		}
	}
}
