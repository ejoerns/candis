package candis.distributed.test;

import candis.distributed.DistributedResult;
import java.io.Serializable;

/**
 *
 * @author Sebastian Willenborg
 */
public class TestResult implements DistributedResult, Serializable{
	public int value;
	public TestResult(int n) {
		value = n;
	}
}
