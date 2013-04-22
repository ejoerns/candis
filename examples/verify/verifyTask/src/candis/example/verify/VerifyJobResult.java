package candis.example.verify;

import candis.distributed.DistributedJobResult;

/**
 * Serializable result for HashTask.
 */
public class VerifyJobResult implements DistributedJobResult {

  /// Some value
  public int data;

  public VerifyJobResult(int result) {
    data = result;
  }
}