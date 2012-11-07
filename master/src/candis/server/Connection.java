/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package candis.server;

import candis.common.Instruction;
import candis.common.Message;
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

	public Connection(Socket socket) throws IOException {
		this.socket = socket;
		logger.log(Level.INFO, "Client connected...");
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

				Message send_msg;
				switch (rec_msg.getRequest()) {
					case GET_INFO:
						System.out.println("Yeah, GET_INFO erkannt!");
						send_msg = new Message(Instruction.SEND_INFO, null);
						oos.writeObject(send_msg);
						break;
					case GET_CERTIFICATE:
						System.out.println("Super, GET_CERTIFICATE erkannt!");
						send_msg = new Message(Instruction.SEND_CERTIFICATE, null);
						break;
					case GET_TRUSTSTORE:
						logger.log(Level.INFO, "Client requested truststore");
						Runtime.getRuntime().exec("keytool -import -v -alias clientCert -keystore sometruststore.bks -storetype bks -file ../../../test_server/test.cert -provider org.bouncycastle.jce.provider.BouncyCastleProvider -providerpath \"../../../bcprov-jdk16-146.jar\"");
						break;
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
