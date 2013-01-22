package candis.example.mini;

import candis.distributed.DistributedControl;
import candis.distributed.DistributedJobParameter;
import candis.distributed.DistributedJobResult;
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
			scheduler.addParameter(new MiniJobParameter(i, 3.5f));
		}

		return scheduler;
	}

	@Override
	public void onSchedulerDone() {
		// Now all tasks are completed successfully
		System.out.println("done!");
	}

	@Override
	public void onReceiveResult(DistributedJobParameter param, DistributedJobResult result) {
		/// One result is finished and we can use it, somehow ...
		MiniJobResult miniResult = (MiniJobResult) result;
		System.out.println(String.format("Got Result: %.3f", miniResult.foobar));

	}
}
