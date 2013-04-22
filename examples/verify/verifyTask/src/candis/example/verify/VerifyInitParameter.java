package candis.example.verify;

import candis.distributed.DistributedJobParameter;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Serializable initial parameter for "global" settings.
 */
public class VerifyInitParameter implements DistributedJobParameter {

  public int data;

  /**
   * Constructor stores xslt file in byte array
   *
   * @param xsltfile
   */
  public VerifyInitParameter(int param) {
    data = param;
  }
}
