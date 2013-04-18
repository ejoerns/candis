package candis.example.xslt;

import candis.distributed.DistributedJobParameter;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Serializable task parameter for MiniTask.
 */
public class XSLTJobParameter implements DistributedJobParameter {

  public byte[] data;

  public XSLTJobParameter(File xmlfile) {
    FileInputStream fis = null;
    try {
      data = new byte[(int) xmlfile.length()];
      fis = new FileInputStream(xmlfile);
      BufferedInputStream bis = new BufferedInputStream(fis);
      bis.read(data, 0, data.length);
    }
    catch (FileNotFoundException ex) {
      Logger.getLogger(XSLTInitParameter.class.getName()).log(Level.SEVERE, null, ex);
    }
    catch (IOException ex) {
      Logger.getLogger(XSLTInitParameter.class.getName()).log(Level.SEVERE, null, ex);
    }
    finally {
      try {
        if (fis != null) {
          fis.close();
        }
      }
      catch (IOException ex) {
        Logger.getLogger(XSLTInitParameter.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }
}