package candis.distributed.test;

import candis.distributed.DistributedResult;
import java.io.Serializable;

/**
 *
 * @author Sebastian Willenborg
 */
public class TestResult extends DistributedResult implements Serializable{
	public final int value;
	public TestResult(int n) {
		value = n;
	}
}
