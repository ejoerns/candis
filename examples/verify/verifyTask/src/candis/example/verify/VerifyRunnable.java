package candis.example.verify;

import candis.distributed.DistributedJobParameter;
import candis.distributed.DistributedJobResult;
import candis.distributed.DistributedRunnable;

/**
 * Verify Task.
 *
 * @author Enrico Joerns
 */
public class VerifyRunnable implements DistributedRunnable {

  private int mInitialParam;

  @Override
  public void stopJob() {
  }

  @Override
  public DistributedJobResult execute(DistributedJobParameter param) {
    VerifyJobParameter parameter = (VerifyJobParameter) param;
    return new VerifyJobResult(mInitialParam + parameter.data);
  }

  @Override
  public void setInitialParameter(DistributedJobParameter parameter) {
    mInitialParam = ((VerifyInitParameter) parameter).data;
  }
}