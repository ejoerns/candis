package candis.example.xslt;

import candis.distributed.DistributedControl;
import candis.distributed.DistributedJobParameter;
import candis.distributed.DistributedJobResult;
import candis.distributed.ResultReceiver;
import candis.distributed.parameter.FileUserParameter;
import candis.distributed.parameter.UserParameterRequester;
import candis.distributed.parameter.UserParameterSet;
import java.io.File;

/**
 *
 * @author Sebastian Willenborg
 */
public class XSLTControl extends DistributedControl implements ResultReceiver {

  private static final int TOTAL_JOBS = 100;
  private int mParametersSent = 0;
  private FileUserParameter mXSLTFile;
  private FileUserParameter mXMLFile;

  @Override
  public void init() {

    // input filenames
    UserParameterSet parameters = new UserParameterSet();

    mXSLTFile = new FileUserParameter("XSL file", "../examples/xslt/xsltControl/json.xslt", null);
    parameters.AddParameter(mXSLTFile);
    mXMLFile = new FileUserParameter("gzXML file", "../examples/xslt/xsltControl/json_more.xml.gz", null);
    parameters.AddParameter(mXMLFile);

    UserParameterRequester.getInstance().request(parameters);

    mParametersSent = 0;
  }

  @Override
  public void onReceiveResult(DistributedJobParameter param, DistributedJobResult result) {
    XSLTJobResult res = (XSLTJobResult) result;
//    System.out.println(new String(res.data));
  }

  @Override
  public final void onSchedulerDone() {
  }

  @Override
  public final DistributedJobParameter getParameter() {
    mParametersSent++;
    return new XSLTJobParameter(new File((String) mXMLFile.getValue()));
  }

  @Override
  public final DistributedJobParameter getInitialParameter() {
    return new XSLTInitParameter(new File((String) mXSLTFile.getValue()));
  }

  @Override
  public final long getParametersLeft() {
    return TOTAL_JOBS - mParametersSent;
  }

  @Override
  public final boolean hasParametersLeft() {
    return (getParametersLeft() > 0);
  }
}
