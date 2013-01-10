package candis.client.comm;

import android.util.Log;
import candis.common.ClassLoaderWrapper;
import candis.client.ClientStateMachine;
import candis.common.Message;
import candis.common.fsm.FSM;
import candis.common.fsm.StateMachineException;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
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
	private final FSM mFSM;
	private final ClassLoaderWrapper mClassLoader;
	private ObjectInputStream mObjInstream = null;
	private InputStream mInstream;

	public CommRequestBroker(final InputStream instream, final FSM fsm, final ClassLoaderWrapper cl) {
		mInstream = instream;
		mFSM = fsm;
		mClassLoader = cl;
		isStopped = false;
	}

	@Override
	public void run() {
		try {
			mObjInstream = new DexloaderObjectInputStream(mInstream);
		}
		catch (StreamCorruptedException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
		catch (IOException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
		try {
			mFSM.process(ClientStateMachine.ClientTrans.SOCKET_CONNECTED);
		}
		catch (StateMachineException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}

		Message m = null;
		while (!isStopped) {
			try {
				Object o = readObject();
				if (o instanceof Message) {
					LOGGER.log(Level.INFO, String.format(
									"Received server Message: %s", ((Message) o).getRequest().toString()));
					try {
						if (((Message) o).getData() == null) {
							mFSM.process(((Message) o).getRequest());
						}
						else {
							mFSM.process(((Message) o).getRequest(), (Object[]) ((Message) o).getData());
						}
  					}
					catch (StateMachineException ex) {
						LOGGER.log(Level.SEVERE, null, ex);
					}
				}
				else {
					LOGGER.log(Level.WARNING, "Received data of unknown type!" + o);
					mInstream.close();
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
		catch (ClassNotFoundException ex) {
			LOGGER.log(Level.WARNING,
								 String.format("ClassNotFoundException: %s", ex.getMessage()));
      ex.printStackTrace();
		}
		catch (EOFException ex) {
			LOGGER.log(Level.WARNING, "Connection to server was terminated unexpected.");
			isStopped = true;
			mObjInstream.close();
			// todo quit loop!
		}
		catch (OptionalDataException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}

		return rec;
	}

	/**
	 * Resolves class with custom classloader.
	 */
	public class DexloaderObjectInputStream extends ObjectInputStream {

		@Override
		public Class resolveClass(ObjectStreamClass desc) throws IOException,
						ClassNotFoundException {

			try {
				return mClassLoader.get().loadClass(desc.getName());
			}
			catch (Exception e) {
			}

			return super.resolveClass(desc);
		}

		public DexloaderObjectInputStream(InputStream in) throws IOException {
			super(in);
		}
	}
}
