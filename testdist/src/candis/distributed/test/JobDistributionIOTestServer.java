package candis.distributed.test;

import candis.distributed.Scheduler;
import candis.server.ClientConnection;
import candis.server.DroidManager;
import candis.server.JobDistributionIOServer;
import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Sebastian Willenborg
 */
public class JobDistributionIOTestServer extends JobDistributionIOServer {

	private static final Logger LOGGER = Logger.getLogger(JobDistributionIOTestServer.class.getName());
	/// List of all TestDroid and Communication threads
	private LinkedList<Thread> droidThreads = new LinkedList<Thread>();
	private LinkedList<TestConnection> mTestConnections = new LinkedList<TestConnection>();
	/// Default number of droids generated if not specified otherwise
	public static final int DEFAULT_DROIDAMOUNT = 4;
	private boolean isClosed = false;

	public JobDistributionIOTestServer(DroidManager manager) {
		super(manager);
	}

	public void initDroids(String jobID) throws IOException {
		initDroids(DEFAULT_DROIDAMOUNT, jobID);
	}

	/**
	 * Creates list of 'number' generated TestDroids.
	 *
	 * @param number Number of TestDroids to generate
	 */
	public void initDroids(int number, String jobID) throws IOException {
		for (int i = 0; i < number; i++) {
			initDroid(i, jobID);
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
	private TestDroid initDroid(int id, String jobID) throws IOException {
		TestDroid d = new TestDroid(id, this, jobID);
		mDroidManager.register((new Integer(id)).toString(), d.getProfile());

		Thread t = new Thread(d);
		TestConnection tc = new TestConnection(d, mDroidManager, this);
		mTestConnections.add(tc);
		Thread l = new Thread(tc);
		droidThreads.add(t);
		droidThreads.add(l);
		t.start();
		l.start();
		return d;
	}

	/**
	 * Simulates Connection local streams and a security-simplified FSM.
	 */
	private class TestConnection extends ClientConnection {

		private TestDroid droid;
		private final Logger LOGGER = Logger.getLogger(TestConnection.class.getName());

		public TestConnection(TestDroid droid, final DroidManager manager, final JobDistributionIOServer comIO) throws IOException {
			super(droid.getInputStream(), droid.getOutputStream(), manager, comIO);
			this.droid = droid;
			mStateMachine = new TestServerStateMachine(this, mDroidManager, mJobDistIO);
			mStateMachine.init();
		}

		@Override
		protected boolean isSocketClosed() {
			return isClosed;
		}

		@Override
		protected void closeSocket() throws IOException {
			isClosed = true;
			((TestServerStateMachine) mStateMachine).stop();
		}

		@Override
		public String getDroidID() {
			return this.droid.getId();
		}
	}

	/**
	 * Stops all running TestDroid and DroidListener Threads.
	 */
	public void stopDroids() {
		for (TestConnection tc : mTestConnections) {
			try {
				tc.closeSocket();
			}
			catch (IOException ex) {
			}
		}
		for (Thread t : droidThreads) {
			if (t.isAlive() && !t.isInterrupted()) {
				t.interrupt();

			}
		}
		Scheduler sch = getCurrentScheduler();
		if (sch != null) {
			sch.abort();
		}
	}
}
