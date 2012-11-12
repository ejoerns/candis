package candis.distributed.test;

import candis.distributed.DistributedParameter;
import java.io.Serializable;

/**
 *
 * @author Sebastian Willenborg
 */
public class TestParameter implements DistributedParameter, Serializable{
	public int number;
	public TestParameter(int n) {
		number = n;
	}
}
