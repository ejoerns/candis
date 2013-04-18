package candis.example.xslt;

import candis.distributed.DistributedJobResult;

/**
 * Serializable result for HashTask.
 */
public class XSLTJobResult implements DistributedJobResult {

  /// Some value
  public byte[] data;

  public XSLTJobResult(byte[] result) {
    data = result;
  }
}