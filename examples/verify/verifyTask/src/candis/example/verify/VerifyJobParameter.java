package candis.example.verify;

import candis.distributed.DistributedJobParameter;

/**
 * Serializable task parameter for MiniTask.
 */
public class VerifyJobParameter implements DistributedJobParameter {

  public int data;

  public VerifyJobParameter(int param) {
    data = param;
  }
}
