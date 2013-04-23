package candis.example.verify;

import candis.distributed.DistributedControl;
import candis.distributed.DistributedJobParameter;
import candis.distributed.DistributedJobResult;
import candis.distributed.ResultReceiver;
import java.util.Stack;

/**
 *
 * @author Enrico Joerns
 */
public class VerifyControl extends DistributedControl implements ResultReceiver {

  private static final int TOTAL_JOBS = 400;
  private static final int INIT_PARAM_INT = 1000;
  private Stack<VerifyJobParameter> mParameterStack = new Stack<VerifyJobParameter>();
  private int mResultsCount;

  @Override
  public void init() {
    System.out.println("VerifyControl.init() called");

    mParameterStack.clear();
    for (int i = 0; i < TOTAL_JOBS; i++) {
      mParameterStack.add(new VerifyJobParameter(i));
    }

    mResultsCount = 0;
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
  public DistributedJobParameter getInitialParameter() {
    return new VerifyInitParameter(INIT_PARAM_INT);
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
