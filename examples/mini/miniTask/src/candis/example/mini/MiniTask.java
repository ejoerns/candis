package candis.example.mini;

import candis.distributed.DistributedParameter;
import candis.distributed.DistributedResult;
import candis.distributed.DistributedTask;

/**
 *
 * @author Sebastian Willenborg
 */
public class MiniTask extends DistributedTask {

	public static DistributedTask newInstance() {
		return new MiniTask();
	}

	@Override
	public void stop() {
		//throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public DistributedResult run(DistributedParameter parameter) {
		//System.out.println("MiniTask: run()");
		MiniParameter p = (MiniParameter) parameter;
		return new MiniResult(p.foo * p.bar);
	}
}
