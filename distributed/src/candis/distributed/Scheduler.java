package candis.distributed;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Sebastian Willenborg
 */
public abstract class Scheduler {

	private static final Logger LOGGER = Logger.getLogger(Scheduler.class.getName());
	// List of callback receivers
	protected final List<ResultReceiver> mReceivers = new LinkedList<ResultReceiver>();
	protected final Stack<DistributedJobParameter> mParams = new Stack<DistributedJobParameter>();
	protected DistributedJobParameter mInitalParameter = null;
	protected JobDistributionIO mJobDistIO;
	/// Results
	private final Map<DistributedJobParameter, DistributedJobResult> mResults = new HashMap<DistributedJobParameter, DistributedJobResult>();
	/// Droids that were already processed, to prevent double initialization
	protected final Set<String> mKnownDroids = new HashSet<String>();
	/// Droids that are currently running
	protected final Map<String, DistributedJobParameter> mRunningDroidsList = new HashMap<String, DistributedJobParameter>();
	/// Droids that can be scheduled
	protected final Map<String, DroidData> mSchedulabeDroids = new HashMap<String, DroidData>();
	private boolean mEnabled = false;
	private Thread schedulerThread;

	/**
	 * Sets the JobDistributionIO.
	 * Obligatoric function
	 *
	 * @param io
	 */
	public void setJobDistributionIO(JobDistributionIO io) {
		mJobDistIO = io;
	}

	public void setInitialParameter(DistributedJobParameter param) {
		mInitalParameter = param;
	}

	public DistributedJobParameter getInitialParameter() {
		return mInitalParameter;
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
	protected void releaseResult(DistributedJobParameter param, DistributedJobResult result) {
		for (ResultReceiver receiver : mReceivers) {
			receiver.onReceiveResult(param, result);
		}
	}

	public void addParameter(DistributedJobParameter param) {
		mParams.push(param);
	}

	public void addParameters(DistributedJobParameter[] params) {
		for (DistributedJobParameter p : params) {
			this.mParams.push(p);
		}
	}

  public int getParametersLeft() {
    return mParams.size();
  }

	/**
	 * Implement this method to create your own nice and fancy Scheduler.
	 *
	 * @param droidList List of all schedulable Droids.
	 */
	protected abstract void schedule(Map<String, DroidData> droidList, JobDistributionIO jobDistIO);

	/**
	 * Used to do inital checks if the droid can be used for scheduling.
	 *
	 * @return true if accepted, false otherwise
	 */
	protected boolean checkAccepted() {
		return true;
	}

	public void start() {
		System.out.println("SS: start()");
		if (mEnabled) {
			return;
		}
		mEnabled = true;
		// start scheduler
		schedulerThread = new Thread(new Runnable() {
			public void run() {
				System.out.println("SS: run()");
				while (mEnabled) {
					synchronized (mSchedulabeDroids) {
						if (hasParameter()) {
							System.out.println("hasParameter -> schedule");
							schedule(mSchedulabeDroids, mJobDistIO);
						}
						try {
							mSchedulabeDroids.wait();
						}
						catch (InterruptedException ex) {
							if (mEnabled) {
								LOGGER.log(Level.SEVERE, null, ex);
							}
						}
					}
				}
			}
		});
		schedulerThread.start();
	}

	/**
	 * Called when a new droid connected.
	 *
	 * @param droidID
	 */
	public void registerDroid(String droidID) {
		System.out.println("SS: onNewDroid()");
		// check if client was already handled
		if (mKnownDroids.contains(droidID)) {
			return;
		}
		// Test if droid is acceptable according to our scheduling rules.
		if (!checkAccepted()) {
			LOGGER.log(Level.INFO, "Rejected Droid {0}", droidID);
			return;
		}
		LOGGER.log(Level.INFO, "Got new Droid {0}", droidID);
		// Send Binary
		mKnownDroids.add(droidID);
		synchronized (mSchedulabeDroids) {
			mSchedulabeDroids.put(droidID, mJobDistIO.getDroidData(droidID));
			mSchedulabeDroids.notifyAll();
		}
		//    mJobDistIO.sendBinary(droidID);
	}

	/**
	 *
	 * @param droidIDs
	 */
	public void registerDroids(Set<String> droidIDs) {
		for (String id : droidIDs) {
			registerDroid(id);
		}
	}

	/**
	 * Called when droid received binary.
	 *
	 * @param droidID
	 */
//  public void onBinarySent(String droidID) {
//    System.out.println("SS: onBinaryRecieved()");
//    // Send inital Parameter
//    mJobDistIO.sendInitialParameter(droidID, mInitalParameter);
//  }
	/**
	 * Called when droid received initial parameter.
	 *
	 * @param droidID
	 */
//  public void onInitParameterSent(String droidID) {
//    System.out.println("SS: onInitParameterRecieved()");
//    // add to list of schedulable tasks
//    synchronized (mSchedulabeDroids) {
//      mSchedulabeDroids.put(droidID, mJobDistIO.getDroidData(droidID));
//      mSchedulabeDroids.notifyAll();
//    }
//  }
	/**
	 * Is called when a job is done.
	 *
	 * @param droidID
	 * @param result
	 */
	// Job is done and new job can be assigned
	public void onJobDone(String droidID, DistributedJobResult result) {
		System.out.println("SS: onJobDone()");
		if (mRunningDroidsList.containsKey(droidID)) {
			DistributedJobParameter p = mRunningDroidsList.remove(droidID);
			mResults.put(p, result);
			releaseResult(p, result);
			LOGGER.log(Level.INFO, "Param {0} on {1} done with {2}", new Object[]{p, droidID, result});
      LOGGER.warning("-- RESULTS: " + mResults.size());
		}
		synchronized (mSchedulabeDroids) {
			mSchedulabeDroids.put(droidID, mJobDistIO.getDroidData(droidID));
			mSchedulabeDroids.notifyAll();
		}
	}

	/**
	 * Is called when an error occurred during computation.
	 *
	 * @param droidID
	 * @param error
	 */
	// ups..
	public void onDroidError(String id, DistributedJobError error) {
		LOGGER.log(Level.SEVERE, "Droid {0}, Error {1}", new Object[]{id, error});
		// removed Droids won't stay in the id List
		// therefore it is not neccesary to check, for DROID_LOST in error
		if (mRunningDroidsList.containsKey(id)) {
			DistributedJobParameter p = mRunningDroidsList.get(id);
			addParameter(p);
		}
	}

	public boolean isDone() {
    LOGGER.log(Level.FINE,
							 "running {0}, params {1}, available Droids {2}",
							 new Object[]{
							mRunningDroidsList.size(),
							mParams.size(),
							mSchedulabeDroids.size()});
		return (mRunningDroidsList.size() + mParams.size()) == 0;
	}

	public void abort() {
		for (String droidID : mSchedulabeDroids.keySet()) {
			if (mRunningDroidsList.containsKey(droidID)) {
				mRunningDroidsList.remove(droidID);
				mJobDistIO.stopJob(droidID);
			}
		}
		mEnabled = false;
		schedulerThread.interrupt();
	}

	public Map<DistributedJobParameter, DistributedJobResult> getResults() {
		return mResults;
	}

	/**
	 * Tests if parameter stack is empty
	 *
	 * @return
	 */
	protected boolean hasParameter() {
		return !mParams.empty();
	}

	/**
	 * Pops new parameter from the parameter list
	 *
	 * @return Parameter if available, null if no parameter found
	 */
	protected DistributedJobParameter popParameters() {
		if (mParams.empty()) {
			return null;
		}
		return mParams.pop();
	}
}
