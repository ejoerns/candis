package candis.example.verify;

import candis.distributed.AnalyzerScheduler;
import candis.distributed.DistributedControl;
import candis.distributed.DistributedJobParameter;
import candis.distributed.DistributedJobResult;
import candis.distributed.ResultReceiver;
import candis.distributed.Scheduler;
import java.util.Stack;

/**
 *
 * @author Enrico Joerns
 */
public class VerifyControl extends DistributedControl implements ResultReceiver {

  private static final int TOTAL_JOBS = 400;
  private static final int INIT_PARAM_INT = 1000;
  private Scheduler mScheduler;
  private Stack<VerifyJobParameter> mParameterStack = new Stack<VerifyJobParameter>();
  private int mResultsCount;

  @Override
  public Scheduler initScheduler() {

    mParameterStack.clear();
    for (int i = 0; i < TOTAL_JOBS; i++) {
      mParameterStack.add(new VerifyJobParameter(i));
    }

    mResultsCount = 0;

    mScheduler = new AnalyzerScheduler(this);

    DistributedJobParameter init = new VerifyInitParameter(INIT_PARAM_INT);
    mScheduler.setInitialParameter(init);

    mScheduler.addResultReceiver(this);

    return mScheduler;
  }

  @Override
  public void onReceiveResult(DistributedJobParameter param, DistributedJobResult result) {
    mResultsCount++;
    doAssert(INIT_PARAM_INT + ((VerifyJobParameter) param).data == ((VerifyJobResult) result).data);
    doAssert(mResultsCount <= TOTAL_JOBS);
  }

  @Override
  public final void onSchedulerDone() {
    doAssert(mResultsCount == TOTAL_JOBS);
    System.out.println("<------ SCHEDULER IS DONE ------>");
  }

  @Override
  public final DistributedJobParameter getParameter() {
    return mParameterStack.pop();
  }

  @Override
  public final long getParametersLeft() {
    return mParameterStack.size();
  }

  @Override
  public final boolean hasParametersLeft() {
    return !mParameterStack.isEmpty();
  }

  private void doAssert(boolean value) {
    if (!value) {
      System.err.println("Assertion error!");
      throw new AssertionError(value);
    }
  }
}
