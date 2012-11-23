package candis.distributed.test;

import candis.common.Instruction;
import candis.common.Message;
import candis.common.fsm.StateMachineException;
import candis.distributed.CommunicationIO;
import candis.distributed.DistributedParameter;
import candis.distributed.DistributedResult;
import candis.distributed.DistributedTask;
import candis.distributed.droid.Droid;
import candis.server.Connection;
import candis.server.DroidManager;
import candis.server.ServerCommunicationIO;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Sebastian Willenborg
 */
public class TestCommunicationIO<T extends DistributedTask> extends ServerCommunicationIO {

	private LinkedList<TestDroid> droids;
	private LinkedList<Thread> droidThreads = new LinkedList<Thread>();
	private final TaskFactory<T> factory;


	public TestCommunicationIO(TaskFactory<T> fact, DroidManager manager) {
		super(manager);
		factory = fact;

	}
	public void initDroids() {
		initDroids(4);
	}

	public void initDroids(int number) {
		droids = new LinkedList<TestDroid>();
		for(int i=0; i<number; i++){
			initDroid(i);
		}
	}

	/**
	 * Initializes a new TestDroid and appends it to the internal TestDroid management list.
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

	private class TestConnection extends Connection {
		private TestDroid droid;
		private final Logger logger = Logger.getLogger(TestDroid.class.getName());

		public TestConnection(TestDroid droid, DroidManager manager) {
			super(null, manager);
			this.droid = droid;
		}

		@Override
		protected void initConnection() {
			ois = droid.ois;
			oos = droid.oos;
			fsm = new TestServerStateMachine(this, mDroidManager);
			try {
				fsm.process(TestServerStateMachine.TestServerTrans.CLIENT_CONNECTED);
			}
			catch (StateMachineException ex) {
				Logger.getLogger(TestCommunicationIO.class.getName()).log(Level.SEVERE, null, ex);
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

	private TestDroid getTestDroid(Droid droid) {
		for (TestDroid tdroid : droids) {
			if (tdroid.getDroid().equals(droid)) {
				return tdroid;
			}
		}
		return null;
	}

	@Override
	public void startTask(Droid droid, DistributedParameter p) {
		TestDroid d = getTestDroid(droid);
		Message m = new Message(Instruction.NO_MSG, p);
		d.sendMessage(new Message(Instruction.SEND_JOB, p));
	}

	@Override
	public void stopTask(Droid droid) {

	}
}
