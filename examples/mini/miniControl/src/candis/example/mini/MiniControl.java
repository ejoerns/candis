package candis.example.mini;

import candis.distributed.DistributedControl;
import candis.distributed.DistributedParameter;
import candis.distributed.DistributedResult;
import candis.distributed.ResultReceiver;
import candis.distributed.Scheduler;
import candis.distributed.SimpleScheduler;

/**
 * Minimalistic example how to initialize a set of tasks and receive its
 * results.
 */
public class MiniControl implements DistributedControl, ResultReceiver {

	MiniInitParameter init;
	Scheduler scheduler;

	@Override
	public Scheduler initScheduler() {
		scheduler = new SimpleScheduler();

		// Register ResultReceiver
		scheduler.addResultReceiver(this);

		// Set initial Parameters
		init = new MiniInitParameter(23);
		scheduler.setInitialParameter(init);

		// Create some tasks
		for (int i = 0; i < 10; i++) {
			scheduler.addParameter(new MiniParameter(i, 3.5f));
		}

		return scheduler;
	}

	@Override
	public void onSchedulerDone() {
		// Now all tasks are completed successfully
		System.out.println("done!");
	}

	@Override
	public void onReceiveResult(DistributedParameter param, DistributedResult result) {
		/// One result is finished and we can use it, somehow ...
		MiniResult miniResult = (MiniResult) result;
		System.out.println(String.format("%.3f", miniResult.foobar));

	}
}