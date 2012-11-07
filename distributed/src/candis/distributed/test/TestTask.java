package candis.distributed.test;

import candis.distributed.DistributedParameter;
import candis.distributed.DistributedResult;
import candis.distributed.DistributedTask;

/**
 *
 * @author Sebastian Willenborg
 */
public class TestTask implements DistributedTask {

	@Override
	public DistributedResult run(DistributedParameter parameter) {
		TestParameter p = (TestParameter)parameter;
		//throw new UnsupportedOperationException("Not supported yet.");

		return new TestResult(p.number * p.number);
	}

	@Override
	public void stop() {
		//throw new UnsupportedOperationException("Not supported yet.");
	}



}
