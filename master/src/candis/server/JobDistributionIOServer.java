package candis.server;

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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Sebastian Willenborg
 */
public class JobDistributionIOServer implements JobDistributionIO, Runnable {

	private static final Logger LOGGER = Logger.getLogger(JobDistributionIOServer.class.getName());
	protected final DroidManager mDroidManager;
	private final CDBLoader mCDBLoader;
	/// The control, scheduler and id for the task currently processed
	protected DistributedControl mDistributedControl;
	protected Scheduler mCurrentScheduler;
	private String mCurrentTaskID = "";
	private int mCurrenJobID = 0;
	/// Thread and list for execution queue
	private Thread mQueueThread;
	private final List<Runnable> mComIOQueue = new LinkedList<Runnable>();
	/// Holds all registered handlers
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
	/* Get methods                                                              */
	/*--------------------------------------------------------------------------*/
	public String getCurrentTaskID() {
		return mCurrentTaskID;
	}

	public Scheduler getCurrentScheduler() {
		return mCurrentScheduler;
	}

	public CDBLoader getCDBLoader() {
		return mCDBLoader;
	}

	// Called by Scheduler.
	@Override
	public DroidData getDroidData(final String droidID) {
		return mDroidManager.getKnownDroids().get(droidID);
	}
//	protected ClientConnection getDroidConnection(String droidID) {
//		return mDroidManager.getConnectedDroids().get(droidID);
//	}
	/*--------------------------------------------------------------------------*/
	/* Scheduler callback methods for Droid control                             */
	/*--------------------------------------------------------------------------*/

	@Override
	public void startJob(String droidID, DistributedJobParameter param) {
		assert param != null;
		System.out.println("startJob(" + droidID + ", " + param + ")");
		mCurrenJobID++;
		mDroidManager.getDroidHandler(droidID).onSendJob(mCurrentTaskID, String.valueOf(mCurrenJobID), param);
		invoke(JobDistributionIOHandler.Event.JOB_SENT);
	}

	@Override
	public void stopJob(final String droidID) {
		mDroidManager.getDroidHandler(droidID).onStopJob(String.valueOf(mCurrenJobID), mCurrentTaskID);
	}

	/*--------------------------------------------------------------------------*/
	/* External control input methods, invoked by FSM                           */
	/*--------------------------------------------------------------------------*/
	public void onJobDone(final String droidID, final DistributedJobResult result) {
		addToQueue(new Runnable() {
			@Override
			public void run() {
				mCurrentScheduler.onJobDone(droidID, result);
				invoke(JobDistributionIOHandler.Event.JOB_DONE);
			}
		});
	}

	public void onDroidConnected(final String droidID) {
		addToQueue(new Runnable() {
			@Override
			public void run() {
				mCurrentScheduler.registerDroid(droidID);
			}
		});
	}

	/*--------------------------------------------------------------------------*/
	/* Execution queue methods                                                  */
	/*--------------------------------------------------------------------------*/
	@Override
	public void run() {
		try {
			while (!(isQueueEmpty() && mCurrentScheduler.isDone())) {

				while (!isQueueEmpty()) {
					Runnable task;
					synchronized (mComIOQueue) {
						task = mComIOQueue.remove(0);
					}
					task.run();
				}
				if (!mCurrentScheduler.isDone()) {
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
		if (mQueueThread != null) {
			mQueueThread.join();
		}
	}
	/*--------------------------------------------------------------------------*/
	/* Scheduler control functions                                              */
	/*--------------------------------------------------------------------------*/

	public void initScheduler(String taskID) throws SchedulerStillRuningException {
		if ((mCurrentScheduler == null) || (mCurrentScheduler.isDone())) {
			mCurrentTaskID = taskID;
			// init scheduler and set self as callback
			mDistributedControl = mCDBLoader.getDistributedControl(taskID);
			mCurrentScheduler = mDistributedControl.initScheduler();
			mCurrentScheduler.setJobDistributionIO(this);
			mCurrenJobID = 0;
		}
		else {
			throw new SchedulerStillRuningException("Scheduler still running");
		}
	}

	/**
	 *
	 * @param droidID
	 * @param p
	 */
	@Override
	public void sendInitialParameter(String droidID, DistributedJobParameter p) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Starts the assigned Scheduler.
	 *
	 * @throws SchedulerStillRuningException
	 */
	public void startScheduler() throws SchedulerStillRuningException {
		if (mCurrentScheduler != null || mCurrentScheduler.isDone()) {
			addToQueue(new Runnable() {
				@Override
				public void run() {
					mCurrentScheduler.registerDroids(mDroidManager.getRegisteredDroids());
				}
			});
			addToQueue(new Runnable() {
				@Override
				public void run() {
					mCurrentScheduler.start();
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
				mCurrentScheduler.abort();
			}
		});
	}
}
