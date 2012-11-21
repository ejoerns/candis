package candis.distributed.test;

import candis.common.Instruction;
import candis.common.Message;
import candis.distributed.CommunicationIO;
import candis.distributed.DistributedParameter;
import candis.distributed.DistributedResult;
import candis.distributed.DistributedTask;
import candis.distributed.droid.Droid;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;

/**
 *
 * @author Sebastian Willenborg
 */
public class TestCommunicationIO<T extends DistributedTask> extends CommunicationIO {

	private LinkedList<TestDroid> droids;
	private LinkedList<Thread> droidThreads = new LinkedList<Thread>();
	private final TaskFactory<T> factory;
	public TestCommunicationIO(TaskFactory<T> fact) {
		factory = fact;
	}
	public void initDroids()
	{
		initDroids(4);
	}

	public void initDroids(int number) {
		droids = new LinkedList<TestDroid>();
		for(int i=0; i<number; i++)
		{
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
		addDroid(d.getDroid());
		Thread t = new Thread(d);
		Thread l = new Thread(new DroidListener(d));
		droidThreads.add(t);
		droidThreads.add(l);
		t.start();
		l.start();
		return d;

	}

	private class DroidListener implements Runnable {
		TestDroid droid;
		private final Logger logger = Logger.getLogger(TestDroid.class.getName());
		public DroidListener(TestDroid droid) {
			this.droid = droid;
		}

		@Override
		public void run() {
			while(true) {
					Message m = null;
					try {
						m = (Message) droid.ois.readObject();

						if(m != null) {
							//TODO: switch thread
							scheduler.onTaskDone(droid.getDroid(), (DistributedResult) m.getData());
						}
					}
					catch (InterruptedIOException ex) {

						return;
					}
					catch (IOException ex) {
						logger.log(Level.SEVERE, null, ex);
					}
					catch (ClassNotFoundException ex) {
						logger.log(Level.SEVERE, null, ex);
					}
					if(m == null) {
						logger.log(Level.SEVERE, "message null");
					}
			}
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
