package candis.distributed.test;

import candis.common.fsm.StateMachineException;
import candis.distributed.CommunicationIO;
import candis.distributed.DistributedTask;
import candis.server.Connection;
import candis.server.DroidManager;
import candis.server.ServerCommunicationIO;
import candis.server.ServerStateMachine;
import candis.server.ServerStateMachine.ServerTrans;
import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Sebastian Willenborg
 */
public class TestCommunicationIO<T extends DistributedTask> extends ServerCommunicationIO {

	/// List of all TestDroid and Communication threads
	private LinkedList<Thread> droidThreads = new LinkedList<Thread>();
	private final TaskFactory<T> factory;
	/// Default number of droids generated if not specified otherwise
	private static final int DEFAULT_DROIDAMOUNT = 1;
	private boolean isClosed = false;

	public TestCommunicationIO(TaskFactory<T> fact, DroidManager manager) {
		super(manager);
		factory = fact;

	}

	public void initDroids() {
		initDroids(DEFAULT_DROIDAMOUNT);
	}

	/**
	 * Creates list of 'number' generated TestDroids.
	 *
	 * @param number Number of TestDroids to generate
	 */
	public void initDroids(int number) {
		for (int i = 0; i < number; i++) {
			initDroid(i);
		}
	}

	/**
	 * Initializes a new TestDroid and appends it to the internal TestDroid
	 * management list.
	 *
	 * Both Droid and Connection threads are started immediately.
	 *
	 * @param id Id of the generated Droid
	 * @return The new TestDroid
	 */
	private TestDroid initDroid(int id) {

		TestDroid d = new TestDroid(id, factory.createTask());
		mDroidManager.addDroid((new Integer(id)).toString(), d);

		Thread t = new Thread(d);
		Thread l = new Thread(new TestConnection(d, mDroidManager, this));
		droidThreads.add(t);
		droidThreads.add(l);
		t.start();
		l.start();
		return d;
	}

	/**
	 * Simulates Connection local streams and a security-simplified FSM.
	 */
	private class TestConnection extends Connection {

		private TestDroid droid;
		private final Logger LOGGER = Logger.getLogger(TestConnection.class.getName());

		public TestConnection(TestDroid droid, final DroidManager manager, final ServerCommunicationIO comIO) {
			super(null, manager, comIO);
			this.droid = droid;
		}

		@Override
		protected void initConnection() {
			oos = droid.getOutputStream();
			ois = droid.getInputStream();
			mStateMachine = new TestServerStateMachine(this, mDroidManager, mCommunicationIO);
			try {
				mStateMachine.process(ServerTrans.CLIENT_NEW, droid.getId());
			}
			catch (StateMachineException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			}
		}

		@Override
		protected boolean isSocketClosed() {
			return isClosed;
		}

		@Override
		protected void closeSocket() throws IOException {
			isClosed = true;
		}

		@Override
		public String getDroidID() {
			return this.droid.getId();
		}
	}

	/**
	 * Stops all running TestDroid and DroidListener Threads
	 */
	public void stopDroids() {
		for (Thread t : droidThreads) {
			if (t.isAlive() && !t.isInterrupted()) {
				t.interrupt();
			}
		}
	}
}
