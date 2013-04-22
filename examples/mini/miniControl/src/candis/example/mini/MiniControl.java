package candis.example.mini;

import candis.distributed.DistributedControl;
import candis.distributed.DistributedJobParameter;
import candis.distributed.DistributedJobResult;
import candis.distributed.ResultReceiver;
import candis.distributed.Scheduler;
import candis.distributed.SimpleScheduler;
import java.util.Stack;

/**
 * Minimalistic example how to initialize a set of tasks and receive its
 * results.
 */
public class MiniControl extends DistributedControl implements ResultReceiver {
	
	private static final int JOBS = 10;
	private Stack<DistributedJobParameter> mJobs;
	
	@Override
	public final Scheduler initScheduler() {
		Scheduler scheduler = new SimpleScheduler(this);

		// Register ResultReceiver
		scheduler.addResultReceiver(this);

		// Set initial Parameters
		scheduler.setInitialParameter(new MiniInitParameter(23));

		// Create some tasks
		mJobs = new Stack<DistributedJobParameter>();
		for (int i = 0; i < JOBS; i++) {
			mJobs.add(new MiniJobParameter(i, 3.5f));
		}
		
		return scheduler;
	}
	
	@Override
	public final void onSchedulerDone() {
		// Now all tasks are completed successfully
		System.out.println("done!");
	}
	
	@Override
	public final void onReceiveResult(DistributedJobParameter param, DistributedJobResult result) {
		/// One result is finished and we can use it, somehow ...
		MiniJobResult miniResult = (MiniJobResult) result;
		System.out.println(String.format("Got Result: %.3f", miniResult.foobar));
	}
	
	@Override
	public final DistributedJobParameter getParameter() {
		return mJobs.pop();
	}
	
	@Override
	public final long getParametersLeft() {
		return mJobs.size();
	}
	
	@Override
	public final boolean hasParametersLeft() {
		return !mJobs.empty();
	}
}
