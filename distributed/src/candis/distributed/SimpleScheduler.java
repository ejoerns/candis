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
public class SimpleScheduler implements Scheduler {

	private static final Logger LOGGER = Logger.getLogger(SimpleScheduler.class.getName());
	private CommunicationIO comIO;
	private Stack<DistributedParameter> params = new Stack<DistributedParameter>();

	private final Map<String, DistributedParameter> running = new HashMap<String, DistributedParameter>();
	private final Map<DistributedParameter, DistributedResult> done = new HashMap<DistributedParameter, DistributedResult>();

	public SimpleScheduler() {
	}

	public void setCommunicationIO(CommunicationIO io) {
		comIO = io;
	}

	public void start() {

		assignTasks();
	}

	private void assignTasks() {
		LOGGER.log(Level.INFO, "Assigning to {0} possible Droids", comIO.getDroidCount());
		for(String droidID: comIO.getConnectedDroids())
		{
			if (!running.containsKey(droidID)) {
				if (!assignTask(droidID)) {
					return;
				}
			}
		}
	}

	private boolean assignTask(String droidID) {
		if (params.isEmpty()) {
			return false;
		}
		DistributedParameter param = params.pop();
		comIO.startJob(droidID, param);
		running.put(droidID, param);
		return true;
	}

	public void abort() {
		for (String droidID : running.keySet()) {
			comIO.stopJob(droidID);
		}
	}

	public void onNewDroid(String droidID) {
		LOGGER.log(Level.INFO, "Got new Droid");
		assignTask(droidID);
	}

	public void onJobDone(String id, DistributedResult result) {
		if (running.containsKey(id)) {
			DistributedParameter p = running.get(id);
			done.put(p, result);
			LOGGER.log(Level.INFO, "Param {0} on {1} done with {2}", new Object[]{p, id, result});
		}
		assignTask(id);
	}

	public void onDroidError(String id, DistributedError error) {
		LOGGER.log(Level.SEVERE, "Droid {0}, Error {1}", new Object[]{id, error});
		// removed Droids won't stay in the id List
		// therefore it is not neccesary to check, for DROID_LOST in error
		if (running.containsKey(id)) {
			DistributedParameter p = running.get(id);
			params.push(p);
		}
		assignTasks();
	}

	public void addParameter(DistributedParameter param) {
		params.push(param);
	}

	public void addParameters(DistributedParameter[] params) {
		for (DistributedParameter p : params) {
			this.params.push(p);
		}
	}
}
