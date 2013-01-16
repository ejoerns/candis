package candis.server;

import candis.common.fsm.FSM;
import candis.common.fsm.StateMachineException;
import candis.distributed.DistributedControl;
import candis.distributed.DistributedJobParameter;
import candis.distributed.DistributedJobResult;
import candis.distributed.DroidData;
import candis.distributed.JobDistributionIO;
import candis.distributed.JobDistributionIOHandler;
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
public class JobDistributionIOServer implements JobDistributionIO, Runnable {

	private static final Logger LOGGER = Logger.getLogger(JobDistributionIOServer.class.getName());
	protected final DroidManager mDroidManager;
	protected DistributedControl mDistributedControl;
	protected Scheduler mScheduler;
	private Thread mQueueThread;
	private final List<Runnable> mComIOQueue = new LinkedList<Runnable>();
	private final CDBLoader mCDBLoader;
	/// Holds all available classloaders.
	private LinkedList<ClassLoader> mClassLoderList = new LinkedList<ClassLoader>();
	private LinkedList<JobDistributionIOHandler> mHanderList = new LinkedList<JobDistributionIOHandler>();

	public JobDistributionIOServer(final DroidManager manager) {
		mCDBLoader = new CDBLoader();
		mDroidManager = manager;
	}
	/*--------------------------------------------------------------------------*/
	/* Callback functionality                                                   */
	/*--------------------------------------------------------------------------*/

	public void addHandler(JobDistributionIOHandler handler) {
		mHanderList.add(handler);
	}

	private void invoke(JobDistributionIOHandler.Event event) {
		for (JobDistributionIOHandler h : mHanderList) {
			h.onEvent(event);
		}
	}
	/*--------------------------------------------------------------------------*/

	public void initDistributedControl() throws SchedulerStillRuningException {
		if ((mScheduler == null) || (mScheduler.isDone())) {
			mScheduler = mDistributedControl.initScheduler();
			mScheduler.setJobDistributionIO(this);
			mQueueThread = new Thread(this);
			mQueueThread.start();
			System.out.println(mQueueThread);
		}
		else {
			throw new SchedulerStillRuningException("Scheduler still running");
		}
	}
	/*--------------------------------------------------------------------------*/
	/* Get methods                                                              */
	/*--------------------------------------------------------------------------*/

	// Called by Scheduler.
	@Override
	public int getDroidCount() {
		return mDroidManager.getConnectedDroids().size();
	}

	// Called by Scheduler.
	@Override
	public DroidData getDroidData(final String droidID) {
		return mDroidManager.getKnownDroids().get(droidID);
	}

	@Override
	public Set<String> getConnectedDroids() {
		return mDroidManager.getConnectedDroids().keySet();
	}

	public Scheduler getScheduler() {
		return mScheduler;
	}

	protected Connection getDroidConnection(String droidID) {
		return mDroidManager.getConnectedDroids().get(droidID);
	}
	/*--------------------------------------------------------------------------*/
	/* Scheduler callback methods for Droid control                             */
	/*--------------------------------------------------------------------------*/

	@Override
	public void sendBinary(String droidID) {
		FSM fsm = getDroidConnection(droidID).getStateMachine();
		try {
			fsm.process(
							ServerStateMachine.ServerTrans.SEND_BINARY,
							mCDBLoader.getDroidBinary());
		}
		catch (StateMachineException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
	}

	@Override
	public void sendInitialParameter(String droidID, DistributedJobParameter parameter) {
		FSM fsm = getDroidConnection(droidID).getStateMachine();
		try {
			fsm.process(
							ServerStateMachine.ServerTrans.SEND_INITAL,
							parameter);
		}
		catch (StateMachineException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
	}

	@Override
	public void startJob(String droidID, DistributedJobParameter param) {
		FSM fsm = getDroidConnection(droidID).getStateMachine();
		try {
			fsm.process(
							ServerStateMachine.ServerTrans.SEND_JOB, param);
		}
		catch (StateMachineException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
	}

	@Override
	public void stopJob(final String droidID) {
		FSM fsm = getDroidConnection(droidID).getStateMachine();
		try {
			fsm.process(
							ServerStateMachine.ServerTrans.STOP_JOB);
		}
		catch (StateMachineException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
	}
	/*--------------------------------------------------------------------------*/
	/* Execution queue methods                                                  */
	/*--------------------------------------------------------------------------*/

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
		invoke(JobDistributionIOHandler.Event.SCHEDULER_DONE);
		LOGGER.log(Level.INFO, "JobDistributionIOServer done");
	}

	/**
	 * Adds Runnable to Queue.
	 *
	 * @param task
	 */
	private void addToQueue(Runnable task) {
		synchronized (mComIOQueue) {
			mComIOQueue.add(task);
			mComIOQueue.notify();
		}
	}

	private boolean isQueueEmpty() {
		synchronized (mComIOQueue) {
			return mComIOQueue.isEmpty();
		}
	}

	public void join() throws InterruptedException {
		mQueueThread.join();
	}
	/*--------------------------------------------------------------------------*/
	/* Scheduler control functions                                              */
	/*--------------------------------------------------------------------------*/

	/**
	 * Starts the assigned Scheduler.
	 *
	 * @throws SchedulerStillRuningException
	 */
	public void startScheduler() throws SchedulerStillRuningException {
		System.out.println("startScheduler()");
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

	/**
	 * Stops the assigned Scheduler.
	 */
	public void stopScheduler() {
		addToQueue(new Runnable() {
			@Override
			public void run() {
				mScheduler.abort();
			}
		});
	}
	/*--------------------------------------------------------------------------*/
	/* External control input methods                                           */
	/*--------------------------------------------------------------------------*/

	public void onJobDone(final String droidID, final DistributedJobResult result) {
		addToQueue(new Runnable() {
			@Override
			public void run() {
				mScheduler.onJobDone(droidID, result);
			}
		});

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
	/*--------------------------------------------------------------------------*/
	/* CDB handling                                                             */
	/*--------------------------------------------------------------------------*/

	public int loadCDB(final File cdbFile) throws Exception {
		int cdbID = mCDBLoader.loadCDB(cdbFile);
		// TODO: merge...
		mDistributedControl = mCDBLoader.getDistributedControl();
		initDistributedControl();
		return cdbID;
	}

	public CDBLoader getCDBLoader() {
		if (mCDBLoader == null) {
		}
		return mCDBLoader;
	}
}
