package candis.distributed.test;

import candis.distributed.DistributedParameter;

/**
 *
 * @author Sebastian Willenborg
 */
public class TestParameter implements DistributedParameter{
	public int number;
	public TestParameter(int n) {
		number = n;
	}
}
