package candis.distributed;


import candis.distributed.droid.Droid;
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
	private final Map<Droid, DistributedParameter> running = new HashMap<Droid, DistributedParameter>();
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
		LOGGER.log(Level.INFO, "Assigning to {0} possible Droids", new Object[] {comIO.getDroidCount()});
		for(int i = comIO.getDroidCount()-1; i >= 0; i--) {
			Droid d = comIO.getDroid(i);
			if(!running.containsKey(d)) {
				if(!assignTask(d)){
					return;
				}
			}
		}
	}

	private boolean assignTask(Droid d) {
		if(params.isEmpty())
		{
			return false;
		}
		DistributedParameter param = params.pop();
		comIO.startTask(d, param);
		running.put(d, param);
		return true;
	}

	public void abort() {
		for(Droid d: running.keySet()) {
			comIO.stopTask(d);
		}
	}

	public void onNewDroid(Droid droid) {
		LOGGER.log(Level.INFO, "Got new Droid");
		assignTask(droid);
	}

	public void onTaskDone(Droid droid, DistributedResult result) {
		if(running.containsKey(droid))
		{
			DistributedParameter p = running.get(droid);
			done.put(p, result);
			LOGGER.log(Level.INFO, "Param {0} on {1} done with {2}", new Object[] {p, droid, result});
		}
		assignTask(droid);
	}


	public void onDroidError(Droid droid, DistributedError error) {
		LOGGER.log(Level.SEVERE, "Droid {0}, Error {1}", new Object[] {droid, error});
		// removed Droids won't stay in the droid List
		// therefore it is not neccesary to check, for DROID_LOST in error
		if(running.containsKey(droid))
		{
			DistributedParameter p = running.get(droid);
			params.push(p);
		}
		assignTasks();
	}

	public void addParameter(DistributedParameter param) {
		params.push(param);
	}

	public void addParameters(DistributedParameter[] params) {
		for(DistributedParameter p: params) {
			this.params.push(p);
		}
	}
}
