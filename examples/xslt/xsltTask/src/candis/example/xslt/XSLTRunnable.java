package candis.example.xslt;

import candis.distributed.DistributedJobParameter;
import candis.distributed.DistributedJobResult;
import candis.distributed.DistributedRunnable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
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

  @Override
  public void stopJob() {
  }

  @Override
  public DistributedJobResult runJob(DistributedJobParameter parameter) {
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

    // get xml
    XSLTJobParameter xmlInput = (XSLTJobParameter) parameter;
    bais = new ByteArrayInputStream(xmlInput.data);
    Source text = new StreamSource(bais);

    // transform
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      transformer.transform(text, new StreamResult(baos));
    }
    catch (TransformerException ex) {
      Logger.getLogger(XSLTRunnable.class.getName()).log(Level.SEVERE, null, ex);
      return null;
    }

    return new XSLTJobResult(baos.toByteArray());
  }

  @Override
  public void setInitialParameter(DistributedJobParameter parameter) {
    mXSLTData = ((XSLTInitParameter) parameter).data;
  }
}