package candis.client.comm;

import candis.client.ClientStateMachine;
import candis.common.Message;
import candis.common.fsm.FSM;
import candis.common.fsm.StateMachineException;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Processes incoming data from socket.
 *
 * Designed to be tun in a sepearte thread.
 *
 * @author Enrico Joerns
 */
public class CommRequestBroker implements Runnable {

	private static final String TAG = "CRB";
	private static final Logger LOGGER = Logger.getLogger(TAG);
	private boolean isStopped;
	private FSM fsm;
	private final InputStream instream;
	private ObjectInputStream mObjInstream = null;
	private InputStream mInstream;

	public CommRequestBroker(final InputStream instream, final FSM fsm) {
		isStopped = false;
		this.instream = instream;
		this.fsm = fsm;
		mInstream = instream;
	}

	@Override
	public void run() {
		try {
			mObjInstream = new ObjectInputStream(mInstream);
		}
		catch (StreamCorruptedException ex) {
			LOGGER.log(Level.ALL, "StreamCorruptedException");
//			Logger.getLogger(CommRequestBroker.class.getName()).log(Level.SEVERE, null, ex);
		}
		catch (IOException ex) {
			LOGGER.log(Level.ALL, "IOException1");
//			Logger.getLogger(CommRequestBroker.class.getName()).log(Level.SEVERE, null, ex);
		}
		try {
			fsm.process(ClientStateMachine.ClientTrans.SOCKET_CONNECTED);
		}
		catch (StateMachineException ex) {
			LOGGER.log(Level.ALL, "StateMachineException");
//			Logger.getLogger(CommRequestBroker.class.getName()).log(Level.SEVERE, null, ex);
		}

		Message m = null;
		while (!isStopped) {
			try {
				Object o = readObject();
				if (o instanceof Message) {
					LOGGER.log(Level.INFO, String.format(
									"Read new Message: %s", ((Message) o).getRequest().toString()));
					try {
						System.out.println("fsm.process() " + ((Message) o).getRequest());
						if (((Message) o).getData() == null) {
							fsm.process(((Message) o).getRequest());
						}
						else {
							fsm.process(((Message) o).getRequest(), ((Message) o).getData());
						}
						System.out.println("fsm.process() done");
					}
					catch (StateMachineException ex) {
						LOGGER.log(Level.ALL, "StateMachineException");
						//						LOGGER.log(Level.SEVERE, null, ex);
					}
				}
				else {
					LOGGER.log(Level.WARNING, "Received data of unknown type!" + o);
					instream.close();
					isStopped = true;
				}
			}
			catch (IOException ex) {
				LOGGER.log(Level.ALL, "IOException2");
				LOGGER.log(Level.SEVERE, null, ex);
			}
		}
	}

	public Object readObject() throws IOException {
		Object rec = null;

		if (mObjInstream == null) {
			LOGGER.warning("InputStream is null");
			return null;
		}

		try {
			rec = mObjInstream.readObject();
		}
		catch (EOFException ex) {
			LOGGER.log(Level.WARNING, "Connection to server was terminated.");
			isStopped = true;
			mObjInstream.close();
			// todo quit loop!
		}
		catch (OptionalDataException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
		catch (ClassNotFoundException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}

		return rec;
	}
}
