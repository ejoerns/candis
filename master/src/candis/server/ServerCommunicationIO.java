package candis.server;

import candis.distributed.CommunicationIO;
import candis.distributed.DistributedParameter;
import candis.distributed.DistributedResult;
import candis.distributed.DroidData;
import candis.distributed.Scheduler;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Sebastian Willenborg
 */
public class ServerCommunicationIO implements CommunicationIO, Runnable {

	protected Scheduler scheduler;
	protected final DroidManager mDroidManager;
	private Thread queueThread;
	private static final Logger LOGGER = Logger.getLogger(ServerCommunicationIO.class.getName());
	private final List<Runnable> comIOQueue = new LinkedList<Runnable>();

	public ServerCommunicationIO(final DroidManager manager) {
		mDroidManager = manager;
	}

	public void setScheduler(Scheduler s) {
		if (scheduler == null || scheduler.isDone()) {
			scheduler = s;
			s.setCommunicationIO(this);
			queueThread = new Thread(this);
			queueThread.start();
		}


	}

	protected Connection getDroidConnection(String droidID) {
		return mDroidManager.getConnectedDroids().get(droidID);
	}

	@Override
	public void startJob(String id, DistributedParameter p) {

		Connection d = getDroidConnection(id);
		try {
			d.sendJob(p);
		}
		catch (IOException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}

	}

	@Override
	public void stopJob(final String id) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public int getDroidCount() {
		return mDroidManager.getConnectedDroids().size();
	}

	@Override
	public DroidData getDroidData(final String droidID) {
		return mDroidManager.getKnownDroids().get(droidID);
	}

	@Override
	public void run() {
		try {
			while (!comIOQueue.isEmpty() || !scheduler.isDone()) {

				while (!comIOQueue.isEmpty()) {
					Runnable task;
					synchronized (comIOQueue) {
						task = comIOQueue.remove(0);

					}
					task.run();
				}
				if (!scheduler.isDone()) {
					synchronized (comIOQueue) {
						comIOQueue.wait();
					}
				}

			}
		}
		catch (InterruptedException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
		LOGGER.log(Level.INFO, "CommunicationIO done");
	}

	public void join() throws InterruptedException {
		queueThread.join();
	}

	@Override
	public Set<String> getConnectedDroids() {
		return mDroidManager.getConnectedDroids().keySet();
	}

	protected void addToQueue(Runnable task) {
		synchronized (comIOQueue) {
			comIOQueue.add(task);
			comIOQueue.notify();
		}
	}

	public void startScheduler() {
		addToQueue(new Runnable() {
			@Override
			public void run() {
				scheduler.start();
			}
		});

	}

	public void onJobDone(final String droidID, final DistributedResult result) {
		addToQueue(new Runnable() {
			@Override
			public void run() {
				scheduler.onJobDone(droidID, result);
			}
		});

	}

	public void onDroidConnected(final String droidID, final Connection connection) {
		mDroidManager.connectDroid(droidID, connection);
		addToQueue(new Runnable() {
			@Override
			public void run() {
				scheduler.onNewDroid(droidID);
			}
		});
	}
}
