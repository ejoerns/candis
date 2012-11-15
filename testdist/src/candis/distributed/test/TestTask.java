package candis.distributed.test;

import candis.distributed.DistributedParameter;
import candis.distributed.DistributedResult;
import candis.distributed.DistributedTask;

/**
 *
 * @author Sebastian Willenborg
 */
public class TestTask extends DistributedTask {


	public static DistributedTask newInstance() {
		return new TestTask();
	}

	@Override
	public void stop() {
		//throw new UnsupportedOperationException("Not supported yet.");
	}



	@Override
	public DistributedResult run(DistributedParameter parameter) {
		TestParameter p = (TestParameter)parameter;
		return new TestResult(p.number * p.number);
	}



}
