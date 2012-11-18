package candis.distributed;

import candis.distributed.droid.Droid;
import java.util.Stack;

/**
 *
 * @author Sebastian Willenborg
 */
public class SimpleScheduler implements Scheduler {
	private CommunicationIO comIO;
	private Stack<DistributedParameter> params = new Stack<DistributedParameter>();

	public SimpleScheduler() {

	}

	public void setCommunicationIO(CommunicationIO io) {
		comIO = io;
	}

	public void start() {
		if(comIO != null) {
			return;
		}
		for(int i = comIO.getDroidCount(); i > 0; i--) {
			Droid d = comIO.getDroid(i);
			if(params.isEmpty()) {
				return;
			}
			DistributedParameter param = params.pop();
			comIO.startTask(param, d);
		}
	}

	public void abort() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void onNewDroid(Droid droid) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void onTaskDone(Droid droid, DistributedResult result) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void onDroidError(Droid droid, DistributedError error) {
		throw new UnsupportedOperationException("Not supported yet.");
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
