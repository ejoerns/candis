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
	private Scheduler mScheduler;
	private Stack<DistributedJobParameter> mJobs;
	
	@Override
	public final Scheduler initScheduler() {
		mScheduler = new SimpleScheduler(this);

		// Register ResultReceiver
		mScheduler.addResultReceiver(this);

		// Set initial Parameters
		mScheduler.setInitialParameter(new MiniInitParameter(23));

		// Create some tasks
		mJobs = new Stack<DistributedJobParameter>();
		for (int i = 0; i < JOBS; i++) {
			mJobs.add(new MiniJobParameter(i, 3.5f));
		}
		
		return mScheduler;
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
		System.out.println("getParameter " + mJobs.pop());
		return mJobs.pop();
	}
	
	@Override
	public final long getParametersLeft() {
		System.out.println("getParametersLeft " + mJobs.size());
		return mJobs.size();
	}
	
	@Override
	public final boolean hasParametersLeft() {
		System.out.println("hasParametersLeft " + mJobs.empty());
		return !mJobs.empty();
	}
}
