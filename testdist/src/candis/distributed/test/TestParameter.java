package candis.distributed.test;

import candis.distributed.DistributedParameter;
import java.io.Serializable;

/**
 *
 * @author Sebastian Willenborg
 */
public class TestParameter extends DistributedParameter implements Serializable {

	public final int number;

	public TestParameter(int n) {
		number = n;
	}
}
