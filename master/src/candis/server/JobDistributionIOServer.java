package candis.server;

import candis.common.ClassLoaderWrapper;
import candis.common.DroidID;
import candis.common.Utilities;
import candis.common.fsm.StateMachineException;
import candis.distributed.DistributedControl;
import candis.distributed.DistributedJobParameter;
import candis.distributed.DistributedJobResult;
import candis.distributed.DroidData;
import candis.distributed.JobDistributionIO;
import candis.distributed.Scheduler;
import candis.distributed.SchedulerStillRuningException;
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
public class JobDistributionIOServer implements JobDistributionIO, Runnable { // TODO: rename!

	private static final Logger LOGGER = Logger.getLogger(JobDistributionIOServer.class.getName());
	protected final DroidManager mDroidManager;
	protected DistributedControl mDistributedControl;
	protected Scheduler mScheduler;
	private Thread mQueueThread;
	private final List<Runnable> mComIOQueue = new LinkedList<Runnable>();
	private CDBLoader mCDBLoader;
	/// Holds all available classloaders.
	private LinkedList<ClassLoader> mClassLoderList = new LinkedList<ClassLoader>();

	public JobDistributionIOServer(final DroidManager manager) {
		mDroidManager = manager;
	}

	public void setDistributedControl(DistributedControl dc) throws SchedulerStillRuningException {
		if (mScheduler == null || mScheduler.isDone()) {
			mDistributedControl = dc;
			mScheduler = dc.initScheduler();
			mScheduler.setCommunicationIO(this);
			mQueueThread = new Thread(this);
			mQueueThread.start();
			System.out.println(mQueueThread);
		}
		else {
			throw new SchedulerStillRuningException("Scheduler still running");
		}
	}

	protected Connection getDroidConnection(String droidID) {
		return mDroidManager.getConnectedDroids().get(droidID);
	}

//	public byte[] getDroidBinary() {
//		return mDroidBinary;
//	}
	@Override
	public void startJob(String id, DistributedJobParameter param) {
		Connection d = getDroidConnection(id);
		try {
			d.getStateMachine().process(ServerStateMachine.ServerTrans.SEND_JOB, param);
		}
		catch (StateMachineException ex) {
			Logger.getLogger(JobDistributionIOServer.class.getName()).log(Level.SEVERE, null, ex);
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
	public void sendInitialParameter(String droidID, DistributedJobParameter parameter) {
		System.out.println(droidID);
		Connection d = getDroidConnection(droidID);
		try {
			d.getStateMachine().process(ServerStateMachine.ServerTrans.SEND_INITAL, parameter);
		}
		catch (StateMachineException ex) {
			Logger.getLogger(JobDistributionIOServer.class.getName()).log(Level.SEVERE, null, ex);
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

	public void addToQueue(Runnable task) {
		synchronized (mComIOQueue) {
			mComIOQueue.add(task);
			mComIOQueue.notify();
		}

	}

	public void startScheduler() throws SchedulerStillRuningException {
		if (mScheduler != null || mScheduler.isDone()) {
			addToQueue(new Runnable() {
				@Override
				public void run() {
					mScheduler.start();
				}
			});
		}
		else {
			throw new SchedulerStillRuningException("Scheduler still running");
		}
	}

	public void onJobDone(final String droidID, final DistributedJobResult result) {
		addToQueue(new Runnable() {
			@Override
			public void run() {
				mScheduler.onJobDone(droidID, result);
			}
		});

	}

	public void onDroidConnected(final DroidID rid, final Connection connection) {
		onDroidConnected(Utilities.toSHA1String(rid.getBytes()), connection);
	}

	public void onDroidConnected(final String droidID, final Connection connection) {
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

	public void loadCDB(final File cdbFile, final ClassLoaderWrapper clw) throws Exception {
		mCDBLoader = new CDBLoader(cdbFile, clw);
		setDistributedControl(mCDBLoader.getDistributedControl());
	}

	public CDBLoader getCDBLoader() {
		return mCDBLoader;
	}
}
