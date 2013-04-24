package candis.server;

import candis.distributed.DistributedControl;
import candis.distributed.DistributedJobParameter;
import candis.distributed.DistributedJobResult;
import candis.distributed.DroidData;
import candis.distributed.JobDistributionIO;
import candis.distributed.ResultReceiver;
import candis.distributed.Scheduler;
import candis.distributed.SchedulerBinder;
import candis.common.WorkerQueue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

/**
 *
 * @author Sebastian Willenborg
 */
public class JobDistributionIOServer implements JobDistributionIO, SchedulerBinder, JobHandler {

	private static final Logger LOGGER = Logger.getLogger(JobDistributionIOServer.class.getName());
	protected final DroidManager mDroidManager;
	private final CDBLoader mCDBLoader;
	/// The control, scheduler and id for the task currently processed
	protected DistributedControl mDistributedControl;
	protected Scheduler mCurrentScheduler;
	private String mCurrentTaskID = "";
	private int mCurrenJobID = 0;
	/// Thread and list for execution queue
	private WorkerQueue mWorker;
	/// Holds all registered handlers
	private List<OnJobSentListener> mJobSentListeners = new LinkedList<OnJobSentListener>();
	///
	private List<OnJobDoneListener> mJobDoneListeners = new LinkedList<OnJobDoneListener>();
	///
	private List<OnTaskDoneListener> mTaskDoneListeners = new LinkedList<OnTaskDoneListener>();
	/// Holds all registered receivers
	protected final List<ResultReceiver> mReceivers = new LinkedList<ResultReceiver>();
	/// Max. time [ms] to wait for an ACK
	private long mAckTimeout = 1000;
	/// Max. time [ms] to wait for a Result
	private long mJobTimeout = 60000;
	/// Parameter cache
	private final Queue<DistributedJobParameter> mParamCache = new ConcurrentLinkedQueue<DistributedJobParameter>();
	/// Holds all active AckTimerTasks
	private final Map<String, TimerTask> mAckTimers = new ConcurrentHashMap<String, TimerTask>();
	/// Holds all active AckTimerTasks
	private final Map<String, TimerTask> mJobTimers = new ConcurrentHashMap<String, TimerTask>();
	/// Holds all sent parameters for that no result is available yet
	private final Map<String, DistributedJobParameter[]> mProcessingParams = new ConcurrentHashMap<String, DistributedJobParameter[]>();
	/// Holds ID of all Droids that are available and idle
	private final Set<String> mIdleDroids = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
	/// Timer to run TimerTasks
	private final Timer mTimer = new Timer();
	///
	private DistributedControl mControl;

	public JobDistributionIOServer(final DroidManager manager) {
		mCDBLoader = new CDBLoader();
		mDroidManager = manager;
		mWorker = new WorkerQueue();
		new Thread(mWorker).start();
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

	@Override
	public DistributedControl getControl() {
		if (mControl == null) {
			LOGGER.severe("Control is null");
		}
		return mControl;
	}

	/*--------------------------------------------------------------------------*/
	/* Scheduler control functions                                              */
	/*--------------------------------------------------------------------------*/
	@Override
	public void startJob(String droidID, DistributedJobParameter[] params) {
		DroidManager.DroidHandler mHandler = mDroidManager.getDroidHandler(droidID);
		// if no handler is available, put parameters back to cache and exit.
		if (mHandler == null) {
			LOGGER.severe(String.format("startJob() failed, lost handler for droid %s...", droidID.substring(0, 9)));
			mParamCache.addAll(Arrays.asList(params));
			return;
		}

		// remove droid from list of available
		mIdleDroids.remove(droidID);

		mCurrenJobID++;

		LOGGER.info(String.format(
						"Start Job %d on Droid %s with %d params",
						mCurrenJobID,
						droidID.substring(0, 9),
						params.length));

		// add parameters to list of unAcked
		if (mProcessingParams.containsKey(droidID)) {
			LOGGER.warning(String.format(
							"Droid %s has already parameters assigned. Will be overwritten.",
							droidID.substring(0, 9)));
		}
		mProcessingParams.put(droidID, params);

		// start timeout timers
		TimerTask ackTask = new AckTimerTask(String.valueOf(mCurrenJobID));
		TimerTask jobTask = new JobTimerTask(String.valueOf(mCurrenJobID));
		mAckTimers.put(String.valueOf(mCurrenJobID), ackTask);
		mJobTimers.put(String.valueOf(mCurrenJobID), jobTask);
		mTimer.schedule(ackTask, mAckTimeout);
		mTimer.schedule(jobTask, mJobTimeout);

		// actually start job
		mHandler.onSendJob(mCurrentTaskID, String.valueOf(mCurrenJobID), params);

		invokeOnJobSent(droidID, String.valueOf(mCurrenJobID), mCurrentTaskID, params.length);
	}

	@Override
	public void stopJob(final String droidID) {
		DroidManager.DroidHandler mHandler = mDroidManager.getDroidHandler(droidID);
		if (mHandler == null) {
			LOGGER.severe(String.format("stopJob() failed, lost handler for droid %s", droidID));
			return;
		}

		mHandler.onStopJob(String.valueOf(mCurrenJobID), mCurrentTaskID);
		// cancel timers
		// TODO: jobID!
		if (mAckTimers.containsKey(String.valueOf(mCurrenJobID))) {
			mAckTimers.remove(String.valueOf(mCurrenJobID)).cancel();
		}
		if (mJobTimers.containsKey(String.valueOf(mCurrenJobID))) {
			mJobTimers.remove(String.valueOf(mCurrenJobID)).cancel();
		}
	}

	@Override
	public String[] getAvailableDroids() {
		return mIdleDroids.toArray(new String[mIdleDroids.size()]);
	}

	@Override
	public DroidData getDroidData(final String droidID) {
		return mDroidManager.getKnownDroids().get(droidID);
	}

	@Override
	public DistributedJobParameter[] getParameters(int n) {
		int count = 0;
		ArrayList<DistributedJobParameter> params = new ArrayList<DistributedJobParameter>();
		// first get from internal cache
		while ((mParamCache.size() > 0) && (count < n)) {
			params.add(mParamCache.poll());
			count++;
		}
		n -= count;
		count = 0;
		// try to load remaining parameters from scheduler
		while (count < n) {
			if (!mControl.hasParametersLeft()) {
				break;
			}
			DistributedJobParameter param = mControl.getParameter();
			params.add(param);
			count++;
		}

		return params.toArray(new DistributedJobParameter[params.size()]);
	}

	@Override
	public void setControl(String taskID) {
		mCurrentTaskID = taskID;
		mControl = mCDBLoader.getDistributedControl(taskID);
		System.out.println("Control loaded: " + mControl.getClass());
		mControl.init();
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

	@Override
	public void setAckTimeout(long millis) {
		mAckTimeout = millis;
	}

	@Override
	public void setJobTimeout(long millis) {
		mJobTimeout = millis;
	}

	@Override
	public void bindScheduler(Scheduler scheduler) {
		mCurrentScheduler = scheduler;
		mCurrentScheduler.setJobDistIO(this);
	}

	@Override
	public void unbindScheduler(Scheduler scheduler) {
		mCurrentScheduler = null;
	}

	@Override
	public void onDroidConnected(String droidID) {
		mIdleDroids.add(droidID);
		// if a scheduler is available, notify about new droid
		if (mCurrentScheduler != null) {
			mCurrentScheduler.doNotify();
		}
	}

	@Override
	public void onJobReceived(String droidID, String jobID) {
		LOGGER.fine(String.format("onJobReceived from %s... for job %s", droidID.substring(0, 9), jobID));
		// cancel droids Ack timeout timer
		if (mAckTimers.containsKey(jobID)) {
			mAckTimers.remove(jobID).cancel();
		}
		else {
			LOGGER.warning(String.format("No AckTimer found for job %s", droidID.substring(0, 9)));
		}
	}

	// TODO: synchronize
	@Override
	public void onJobDone(String droidID, String jobID, final DistributedJobResult[] results, long exectime) {
		LOGGER.fine(String.format("onJobDone from %s... for job %s", droidID.substring(0, 9), jobID));
		// cancel droids Job timeout timer
		if (mJobTimers.containsKey(jobID)) {
			mJobTimers.remove(jobID).cancel();
		}
		else {
			LOGGER.warning(String.format("No JobTimer found for job %s", droidID.substring(0, 9)));
		}
		// 
		if (!mProcessingParams.containsKey(droidID)) {
			LOGGER.severe(String.format(
							"Received unknown result from droid %s",
							droidID.substring(0, 9)));
			// inform scheduler that droid is now available again
			mIdleDroids.add(droidID);
			mCurrentScheduler.doNotify();
			return;
		}

		final DistributedJobParameter[] params = mProcessingParams.remove(droidID);
		// inform scheduler that droid is now available again
		mIdleDroids.add(droidID);
		System.out.println("Notifying current Scheduler");
		mCurrentScheduler.doNotify();

		// notify receivers
		invokeOnJobDone(droidID, jobID, mCurrentTaskID, results.length, exectime);
		for (int i = 0; i < results.length; i++) {
			releaseResult(params[i], results[i]);
		}

		// check if task is done
		if (!mControl.hasParametersLeft() && mParamCache.isEmpty() && mProcessingParams.isEmpty()) {
			// terminate scheduler and notify listeners
			mCurrentScheduler.stop();
			invokeOnTaskDone(mCurrentTaskID);
		}
	}

	/**
	 * Adds a listener for Results.
	 *
	 * @param receiver
	 */
	public void addResultReceiver(ResultReceiver receiver) {
		mReceivers.add(receiver);
	}

	/**
	 * Called by Scheduler to publish results.
	 *
	 * @param param
	 * @param result
	 */
	protected void releaseResult(final DistributedJobParameter param, final DistributedJobResult result) {
		// TODO: use thread pool?
		new Thread(new Runnable() {
			@Override
			public void run() {
				for (ResultReceiver receiver : mReceivers) {
					receiver.onReceiveResult(param, result);
				}
			}
		}).start();
	}

	/*--------------------------------------------------------------------------*/
	/* Callback functionality                                                   */
	/*--------------------------------------------------------------------------*/
	@Override
	public void addJobSentListener(OnJobSentListener listener) {
		mJobSentListeners.add(listener);
	}

	@Override
	public void addJobDoneListener(OnJobDoneListener listener) {
		mJobDoneListeners.add(listener);
	}

	@Override
	public void addTaskDoneListener(OnTaskDoneListener listener) {
		mTaskDoneListeners.add(listener);
	}

	private void invokeOnJobSent(
					final String droidID,
					final String jobID,
					final String taskID,
					final int params) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				for (OnJobSentListener listener : mJobSentListeners) {
					listener.onJobSent(droidID, jobID, taskID, params);
				}
			}
		}).start();
	}

	private void invokeOnJobDone(
					final String droidID,
					final String jobID,
					final String taskID,
					final int results,
					final long exectime) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				for (OnJobDoneListener listener : mJobDoneListeners) {
					listener.onJobDone(droidID, jobID, taskID, results, exectime);
				}
			}
		}).start();
	}

	private void invokeOnTaskDone(
					final String taskID) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				for (OnTaskDoneListener listener : mTaskDoneListeners) {
					listener.onTaskDone(taskID);
				}
			}
		}).start();
	}

	/**
	 * Invoked if no ACK for Job was received within timeout interval.
	 */
	private class AckTimerTask extends TimerTask {

		private String mJobID;

		AckTimerTask(String jobID) {
			mJobID = jobID;
		}

		@Override
		public void run() {
			DroidManager.DroidHandler mHandler = mDroidManager.getDroidHandler(mJobID);
			LOGGER.warning(String.format(
							"Timeout while waiting for ACK for Job %s...",
							mJobID));
			// tell droid to stop
			if (mHandler != null) {
				mHandler.onStopJob(null, mJobID);// TODO: add taskID
			}
			// remove parameters from processing list and add back to cache
			if (mProcessingParams.containsKey(mJobID)) {
				mParamCache.addAll(Arrays.asList(mProcessingParams.remove(mJobID)));
			}
		}
	}

	/**
	 * Invoked if no Result for Job was received within timeout interval.
	 */
	private class JobTimerTask extends TimerTask {

		private String mJobID;

		JobTimerTask(String jobID) {
			mJobID = jobID;
		}

		@Override
		public void run() {
			DroidManager.DroidHandler mHandler = mDroidManager.getDroidHandler(mJobID);
			LOGGER.warning(String.format(
							"Timeout while waiting for Result for Job %s",
							mJobID));
			// tell droid to stop
			if (mHandler != null) {
				mHandler.onStopJob(null, mJobID);// TODO: add taskID
			}
			// remove parameters from processing list and add back to cache
			if (mProcessingParams.containsKey(mJobID)) {
				mParamCache.addAll(Arrays.asList(mProcessingParams.remove(mJobID)));
			}
		}
	}
}
