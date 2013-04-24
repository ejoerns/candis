package candis.example.hash;

import candis.distributed.DistributedJobResult;

/**
 * Serializable result for HashTask.
 */
public class TestHashJobResult implements DistributedJobResult {

  /// Some value
  public final boolean mDone;

  public TestHashJobResult(boolean done) {
    mDone = done;
  }
}