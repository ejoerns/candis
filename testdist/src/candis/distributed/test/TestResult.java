package candis.distributed.test;

import candis.distributed.DistributedResult;

/**
 *
 * @author Sebastian Willenborg
 */
public class TestResult implements DistributedResult{
	public int value;
	public TestResult(int n) {
		value = n;
	}
}
