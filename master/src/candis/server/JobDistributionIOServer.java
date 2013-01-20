package candis.server;

import candis.common.Connection;
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
	/// Holds all registered classloaders.
//	private LinkedList<ClassLoader> mClassLoderList = new LinkedList<ClassLoader>();
	/// Holds all registered handlers
	private LinkedList<JobDistributionIOHandler> mHanderList = new LinkedList<JobDistributionIOHandler>();
	/// The id of the task that is currently processed
	private String mCurrentTaskID = "";

	public JobDistributionIOServer(final DroidManager manager) {
//		CandisLog.level(CandisLog.VERBOSE);
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
	/* Get methods                                                              */
	/*--------------------------------------------------------------------------*/
	public String getCurrentTaskID() {
		return mCurrentTaskID;
	}

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

	public CDBLoader getCDBLoader() {
		return mCDBLoader;
	}

	protected ClientConnection getDroidConnection(String droidID) {
		return mDroidManager.getConnectedDroids().get(droidID);
	}
	/*--------------------------------------------------------------------------*/
	/* Scheduler callback methods for Droid control                             */
	/*--------------------------------------------------------------------------*/

//	@Override
//	public void sendBinary(String droidID) {
//		FSM fsm = getDroidConnection(droidID).getStateMachine();
//		try {
//			fsm.process(
//							ServerStateMachine.ServerTrans.SEND_BINARY,
//							mCDBLoader.getDroidBinary());
//		}
//		catch (StateMachineException ex) {
//			LOGGER.log(Level.SEVERE, null, ex);
//		}
//	}
//	@Override
//	public void sendInitialParameter(String droidID, DistributedJobParameter parameter) {
//		FSM fsm = getDroidConnection(droidID).getStateMachine();
//		try {
//			fsm.process(
//							ServerStateMachine.ServerTrans.SEND_INITAL,
//							parameter);
//		}
//		catch (StateMachineException ex) {
//			LOGGER.log(Level.SEVERE, null, ex);
//		}
//	}
	@Override
	public void startJob(String droidID, DistributedJobParameter param) {
		assert param != null;
		FSM fsm = getDroidConnection(droidID).getStateMachine();
		try {
			fsm.process(
							ServerStateMachine.ServerTrans.SEND_JOB, mCurrentTaskID, param);// TODO: check ID
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
							ServerStateMachine.ServerTrans.STOP_JOB, mCurrentTaskID);
		}
		catch (StateMachineException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
	}

	/*--------------------------------------------------------------------------*/
	/* External control input methods, invoked by FSM                           */
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
				mScheduler.registerDroid(droidID);
			}
		});
	}

//	public void onBinarySent(final String droidID) {
//		addToQueue(new Runnable() {
//			@Override
//			public void run() {
//				mScheduler.onBinarySent(droidID);
//			}
//		});
//	}
//	public void onInitalParameterSent(final String droidID) {
//		addToQueue(new Runnable() {
//			@Override
//			public void run() {
//				mScheduler.onInitParameterSent(droidID);
//			}
//		});
//	}

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

	public void initScheduler(String taskID) throws SchedulerStillRuningException {
		if ((mScheduler == null) || (mScheduler.isDone())) {
			mCurrentTaskID = taskID;
			// init scheduler and set self as callback
			mDistributedControl = mCDBLoader.getDistributedControl(taskID);
			mScheduler = mDistributedControl.initScheduler();
			mScheduler.setJobDistributionIO(this);
		}
		else {
			throw new SchedulerStillRuningException("Scheduler still running");
		}
	}

	/**
	 * Starts the assigned Scheduler.
	 *
	 * @throws SchedulerStillRuningException
	 */
	public void startScheduler() throws SchedulerStillRuningException {
		if (mScheduler != null || mScheduler.isDone()) {
			addToQueue(new Runnable() {
				@Override
				public void run() {
					mScheduler.registerDroids(mDroidManager.getConnectedDroids().keySet());
				}
			});
			addToQueue(new Runnable() {
				@Override
				public void run() {
					mScheduler.start();
				}
			});
			// Starts worker queue
			mQueueThread = new Thread(this);
			mQueueThread.start();
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
}
