package candis.distributed.test;

import candis.common.Instruction;
import candis.common.Message;
import candis.common.fsm.StateMachineException;
import candis.distributed.DistributedParameter;
import candis.distributed.DistributedTask;
import candis.server.Connection;
import candis.server.DroidManager;
import candis.server.ServerCommunicationIO;
import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Sebastian Willenborg
 */
public class TestCommunicationIO<T extends DistributedTask> extends ServerCommunicationIO {

	private LinkedList<TestDroid> droids;
	/// List of all TestDroid and Communication threads
	private LinkedList<Thread> droidThreads = new LinkedList<Thread>();
	private final TaskFactory<T> factory;
	/// Default number of droids generated if not specified otherwise
	private static final int DEFAULT_DROIDAMOUNT = 4;

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
		droids = new LinkedList<TestDroid>();
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
		droids.add(d);
		//addDroid(d.getDroid());
		Thread t = new Thread(d);
		Thread l = new Thread(new TestConnection(d, mDroidManager));
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
		private final Logger logger = Logger.getLogger(TestDroid.class.getName());

		public TestConnection(TestDroid droid, DroidManager manager) {
			super(null, manager);
			this.droid = droid;
		}

		@Override
		protected void initConnection() {
			oos = droid.getOutputStream();
			ois = droid.getInputStream();
			fsm = new TestServerStateMachine(this, mDroidManager);
			try {
				fsm.process(TestServerStateMachine.TestServerTrans.CLIENT_CONNECTED, droid.getId());
			} catch (StateMachineException ex) {
				logger.log(Level.SEVERE, null, ex);
			}
		}

		@Override
		protected boolean isSocketClosed() {
			return false;
		}

		@Override
		protected void closeSocket() throws IOException {
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

	private TestDroid getTestDroid(String droidID) {
		for (TestDroid tdroid : droids) {
			if (tdroid.getId().equals(droidID)) {
				return tdroid;
			}
		}
		return null;
	}

	@Override
	public void startTask(String id, DistributedParameter p) {
		System.out.println("startTask()");
		TestDroid d = getTestDroid(id);
		Message m = new Message(Instruction.NO_MSG, p);
		d.sendMessage(new Message(Instruction.SEND_JOB, p));
	}

	@Override
	public void stopTask(String id) {
		throw new UnsupportedOperationException();
	}
}
