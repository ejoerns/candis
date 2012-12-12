package candis.example.mini;

import candis.distributed.DistributedControl;
import candis.distributed.DistributedParameter;
import candis.distributed.DistributedResult;
import candis.distributed.ResultReceiver;
import candis.distributed.Scheduler;
import candis.distributed.SimpleScheduler;

/**
 * Minimalistic example how to initialize a set of Tasks.
 */
public class MiniControl implements DistributedControl, ResultReceiver {

	MiniInitParameter init;
	Scheduler scheduler;
	@Override
	public Scheduler initScheduler() {
		init = new MiniInitParameter(23);

		scheduler = new SimpleScheduler();
		scheduler.addResultReceiver(this);
		scheduler.setInitialParameter(init);

		for (int i = 0; i < 10; i++) {
			scheduler.addParameter(new MiniParameter(i, 3.5f));
		}
		return scheduler;
	}

	@Override
	public void schedulerDone() {
		System.out.println("done!");
	}

	@Override
	public void onReceiveResult(DistributedParameter param, DistributedResult result) {
		MiniResult miniResult = (MiniResult) result;
		System.out.println(String.format("%.3f", miniResult.foobar));

	}

}