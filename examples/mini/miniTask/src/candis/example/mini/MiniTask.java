package candis.example.mini;

import candis.distributed.DistributedParameter;
import candis.distributed.DistributedResult;
import candis.distributed.DistributedTask;

/**
 * Example Task.
 * Multiplies MiniParameter.foo with MiniParamter.bar
 */
public class MiniTask extends DistributedTask {

	/**
	 * Gets called, when the Task should be aborted.
	 */
	@Override
	public void stop() {
		// Nothing to do here
	}

	/**
	 * Main code for this Task.
	 *
	 * @param parameter
	 * @return The generated MiniResult, when the task is finished
	 */
	@Override
	public DistributedResult run(DistributedParameter parameter) {
		// Cast incomming Parameter
		MiniParameter p = (MiniParameter) parameter;
		return new MiniResult(p.foo * p.bar);
	}
}
