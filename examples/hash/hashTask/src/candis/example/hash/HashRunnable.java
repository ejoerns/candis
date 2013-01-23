package candis.example.hash;

import candis.distributed.DistributedJobParameter;
import candis.distributed.DistributedJobResult;
import candis.distributed.DistributedRunnable;

/**
 * Example Task.
 * Multiplies MiniParameter.foo with MiniParamter.bar
 */
public class HashRunnable implements DistributedRunnable {

	private HashInitParameter initial;


	@Override
	public void stopJob() {
		// Nothing to do here
	}

	@Override
	public DistributedJobResult runJob(DistributedJobParameter parameter) {
		// Cast incomming Parameter
		HashJobParameter p = (HashJobParameter) parameter;
		//new HashJobResult(true, "da");
		return new HashJobResult(false, null);
	}


	@Override
	public void setInitialParameter(DistributedJobParameter parameter) {
		initial = (HashInitParameter) parameter;
	}
}