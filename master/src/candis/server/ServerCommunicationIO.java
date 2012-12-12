package candis.server;

import candis.common.fsm.StateMachineException;
import candis.distributed.CommunicationIO;
import candis.distributed.DistributedControl;
import candis.distributed.DistributedParameter;
import candis.distributed.DistributedResult;
import candis.distributed.DistributedTask;
import candis.distributed.DroidData;
import candis.distributed.Scheduler;
import java.io.File;
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

	private static final Logger LOGGER = Logger.getLogger(ServerCommunicationIO.class.getName());
	protected final DroidManager mDroidManager;
	protected DistributedControl mDistributedControl;
	protected Scheduler mScheduler;
	private Thread mQueueThread;
	private final List<Runnable> mComIOQueue = new LinkedList<Runnable>();
//	private byte[] mDroidBinary;
	private CDBLoader mCDBLoader;

	public ServerCommunicationIO(final DroidManager manager) {
		mDroidManager = manager;
	}

	public void setDistributedControl(DistributedControl dc) {
		if (mScheduler == null || mScheduler.isDone()) {
			mDistributedControl = dc;
			mScheduler = dc.initScheduler();
			mScheduler.setCommunicationIO(this);
			mQueueThread = new Thread(this);
			mQueueThread.start();
		}
	}

	protected Connection getDroidConnection(String droidID) {
		return mDroidManager.getConnectedDroids().get(droidID);
	}

//	public byte[] getDroidBinary() {
//		return mDroidBinary;
//	}

	@Override
	public void startJob(String id, DistributedParameter param) {
		Connection d = getDroidConnection(id);
		try {
			d.getStateMachine().process(ServerStateMachine.ServerTrans.SEND_JOB, param);
		}
		catch (StateMachineException ex) {
			Logger.getLogger(ServerCommunicationIO.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	// Called by Scheduler.
	@Override
	public void sendBinary(String droidID) {
		Connection d = getDroidConnection(droidID);
		try {
			d.getStateMachine().process(
							ServerStateMachine.ServerTrans.SEND_BINARY,
							mCDBLoader.getDroidBinary());
		}
		catch (StateMachineException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
	}

	@Override
	public void sendInitialParameter(String droidID, DistributedParameter parameter) {
		Connection d = getDroidConnection(droidID);
		try {
			d.getStateMachine().process(ServerStateMachine.ServerTrans.SEND_INITAL, parameter);
		}
		catch (StateMachineException ex) {
			Logger.getLogger(ServerCommunicationIO.class.getName()).log(Level.SEVERE, null, ex);
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

	private boolean isQueueEmpty() {
		synchronized (mComIOQueue) {
			return mComIOQueue.isEmpty();
		}
	}

	@Override
	public void run() {
		try {
			while (!(isQueueEmpty() && mScheduler.isDone())) {

				while (!isQueueEmpty()) {
					Runnable task;
					synchronized (mComIOQueue) {
						task = mComIOQueue.remove(0);

					}
					task.run();
				}
				if (!mScheduler.isDone()) {
					synchronized (mComIOQueue) {
						if (mComIOQueue.isEmpty()) {
							mComIOQueue.wait();
						}
					}
				}

			}
		}
		catch (InterruptedException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
		mDistributedControl.onSchedulerDone();
		LOGGER.log(Level.INFO, "CommunicationIO done");
	}

	@Override
	public void join() throws InterruptedException {
		mQueueThread.join();
	}

	@Override
	public Set<String> getConnectedDroids() {
		return mDroidManager.getConnectedDroids().keySet();
	}

	protected void addToQueue(Runnable task) {

		synchronized (mComIOQueue) {
			mComIOQueue.add(task);
			mComIOQueue.notify();
		}
	}

	public void startScheduler() {
		addToQueue(new Runnable() {
			@Override
			public void run() {
				mScheduler.start();
			}
		});

	}

	public void onJobDone(final String droidID, final DistributedResult result) {
		addToQueue(new Runnable() {
			@Override
			public void run() {
				mScheduler.onJobDone(droidID, result);
			}
		});

	}

	public void onDroidConnected(final String droidID, final Connection connection) {
		mDroidManager.connectDroid(droidID, connection);
		addToQueue(new Runnable() {
			@Override
			public void run() {
				mScheduler.onNewDroid(droidID);
			}
		});
	}

	public void onBinarySent(final String droidID) {
		addToQueue(new Runnable() {
			@Override
			public void run() {
				mScheduler.onBinaryRecieved(droidID);
			}
		});
	}

	public void onInitalParameterSent(final String droidID) {
		addToQueue(new Runnable() {
			@Override
			public void run() {
				mScheduler.onInitParameterRecieved(droidID);
			}
		});
	}
	
	void loadCDB(final File cdbFile) {
		mCDBLoader = new CDBLoader(cdbFile);
	}
	
}
