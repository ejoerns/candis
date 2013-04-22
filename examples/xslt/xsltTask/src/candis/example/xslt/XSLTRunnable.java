package candis.example.xslt;

import candis.distributed.DistributedJobParameter;
import candis.distributed.DistributedJobResult;
import candis.distributed.DistributedRunnable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * Example Task.
 * Multiplies MiniParameter.foo with MiniParamter.bar
 */
public class XSLTRunnable implements DistributedRunnable {

  private byte[] mXSLTData;
  private static final int TEST_LOOPS = 10;

  @Override
  public void stopJob() {
  }

  @Override
  public DistributedJobResult execute(DistributedJobParameter parameter) {
    TransformerFactory factory = TransformerFactory.newInstance();

    // get xslt
    ByteArrayInputStream bais = new ByteArrayInputStream(mXSLTData);
    Source xslt = new StreamSource(bais);
    Transformer transformer = null;
    try {
      transformer = factory.newTransformer(xslt);
    }
    catch (TransformerConfigurationException ex) {
      Logger.getLogger(XSLTRunnable.class.getName()).log(Level.SEVERE, null, ex);
      return null;
    }

    XSLTJobParameter xmlInput = (XSLTJobParameter) parameter;

    InputStream is;
    ByteArrayOutputStream buffer = null;
    byte[] unzipedXMLData = null;
    try {
      is = new GZIPInputStream(new ByteArrayInputStream(xmlInput.data));
      buffer = new ByteArrayOutputStream();

      int nRead;
      byte[] data = new byte[16384];

      while ((nRead = is.read(data, 0, data.length)) != -1) {
        buffer.write(data, 0, nRead);
      }
      buffer.flush();
      unzipedXMLData = buffer.toByteArray();
    }
    catch (IOException ex) {
      Logger.getLogger(XSLTRunnable.class.getName()).log(Level.SEVERE, null, ex);
    }

    // get xml from gzip
    InputStream insteam = null;
    ByteArrayOutputStream baos = null;
    for (int i = 0; i < TEST_LOOPS; i++) {
      try {
        insteam = new ByteArrayInputStream(unzipedXMLData);
        Source text = new StreamSource(insteam);

        // transform
        baos = new ByteArrayOutputStream();
        try {
          transformer.transform(text, new StreamResult(baos));
        }
        catch (TransformerException ex) {
          Logger.getLogger(XSLTRunnable.class.getName()).log(Level.SEVERE, null, ex);
          return null;
        }
        insteam.close();
      }
      catch (IOException ex) {
        Logger.getLogger(XSLTRunnable.class.getName()).log(Level.SEVERE, null, ex);
      }
    }

    return new XSLTJobResult(baos.toByteArray());
  }

  @Override
  public void setInitialParameter(DistributedJobParameter parameter) {
    mXSLTData = ((XSLTInitParameter) parameter).data;
  }
}