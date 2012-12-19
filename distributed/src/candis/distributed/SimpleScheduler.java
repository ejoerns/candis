package candis.distributed;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple Scheduler to assign tasks to Droids without special predictions.
 *
 * Every Droid will get DistributedParameters, if a Droid finishes it's task or
 * a new Droid appears it will get is next DistributedParameter
 *
 * @author Sebastian Willenborg
 */
public class SimpleScheduler extends Scheduler {


	private enum DroidState {
		NEW,
		BINARY,
		INITIAL;
	}

	private static final Logger LOGGER = Logger.getLogger(SimpleScheduler.class.getName());
	private JobDistributionIO comIO;
	private DistributedJobParameter initalParameter = null;
	private Stack<DistributedJobParameter> params = new Stack<DistributedJobParameter>();
	private final Map<String, DistributedJobParameter> running = new HashMap<String, DistributedJobParameter>();
	private final Map<DistributedJobParameter, DistributedJobResult> done = new HashMap<DistributedJobParameter, DistributedJobResult>();
	private final Map<String, DroidState> droidStates = new HashMap<String, DroidState>();
	private boolean started = false;

	public SimpleScheduler() {
	}

	public void setCommunicationIO(JobDistributionIO io) {
		comIO = io;
	}

	@Override
	public Map<DistributedJobParameter, DistributedJobResult> getResults() {
		return done;
	}
	
	public void start() {
		started = true;
		assignTasks();
	}

	private void assignTasks() {
		LOGGER.log(Level.INFO, "Assigning to {0} possible Droids", comIO.getDroidCount());
		for (String droidID : comIO.getConnectedDroids()) {
			assignTask(droidID);
		}
	}

	private boolean assignTask(String droidID) {
		if (params.isEmpty()) {
			return false;
		}
		if (running.containsKey(droidID)) {
			return false;
		}
		DroidState state = DroidState.NEW;
		if(droidStates.containsKey(droidID)) {
			state = droidStates.get(droidID);
		}
		switch(state) {
			case NEW:
				comIO.sendBinary(droidID);
				break;
			case BINARY:
				comIO.sendInitialParameter(droidID, initalParameter);
				break;
			case INITIAL:
				DistributedJobParameter param = params.pop();
				comIO.startJob(droidID, param);
				running.put(droidID, param);
				break;
			default:
				throw new AssertionError(state.name());
		}
		return true;
	}

	public void abort() {
		for (String droidID : running.keySet()) {
			comIO.stopJob(droidID);
		}
	}

	public void onNewDroid(String droidID) {
		if (!started) {
			return;
		}
		LOGGER.log(Level.INFO, "Got new Droid {0}", droidID);
		droidStates.put(droidID, DroidState.NEW);
		assignTask(droidID);
	}

	public void onJobDone(String droidID, DistributedJobResult result) {
		if (running.containsKey(droidID)) {
			DistributedJobParameter p = running.remove(droidID);
			done.put(p, result);
			releaseResult(p, result);
			LOGGER.log(Level.INFO, "Param {0} on {1} done with {2}", new Object[]{p, droidID, result});
		}
		assignTask(droidID);
	}

	public void onDroidError(String id, DistributedJobError error) {
		LOGGER.log(Level.SEVERE, "Droid {0}, Error {1}", new Object[]{id, error});

		// removed Droids won't stay in the id List
		// therefore it is not neccesary to check, for DROID_LOST in error
		if (running.containsKey(id)) {
			DistributedJobParameter p = running.get(id);
			params.push(p);
		}
		assignTasks();
	}

	public void addParameter(DistributedJobParameter param) {
		params.push(param);
	}

	public void addParameters(DistributedJobParameter[] params) {
		for (DistributedJobParameter p : params) {
			this.params.push(p);
		}
	}

	public boolean isDone() {
		LOGGER.log(Level.FINE, "running {0}, params {1}, available Droids {2}", new Object[]{running.size(), params.size(), comIO.getDroidCount()});
		return (running.size() + params.size()) == 0;
	}

	public void setInitialParameter(DistributedJobParameter param) {
		initalParameter = param;
	}

	public void onBinaryRecieved(String droidID) {
		droidStates.put(droidID, DroidState.BINARY);
		// Send inital Parameter
		assignTask(droidID);

	}

	public void onInitParameterRecieved(String droidID) {
		droidStates.put(droidID, DroidState.INITIAL);
		// Send Job
		assignTask(droidID);
	}
}
