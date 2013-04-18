package candis.example.xslt;

import candis.distributed.DistributedControl;
import candis.distributed.DistributedJobParameter;
import candis.distributed.DistributedJobResult;
import candis.distributed.ResultReceiver;
import candis.distributed.Scheduler;
import candis.distributed.SimpleScheduler;
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
  private int mJobsDone = 0;
  private Scheduler mScheduler;
  private FileUserParameter mXSLTFile;
  private FileUserParameter mXMLFile;

  @Override
  public Scheduler initScheduler() {

    // input filenames
    UserParameterSet parameters = new UserParameterSet();

    mXSLTFile = new FileUserParameter("XSL file", "../examples/xslt/xsltControl/json.xslt", null);
    parameters.AddParameter(mXSLTFile);
    mXMLFile = new FileUserParameter("XML file", "../examples/xslt/xsltControl/json.xml", null);
    parameters.AddParameter(mXMLFile);

    UserParameterRequester.getInstance().request(parameters);

    mJobsDone = 0;

    mScheduler = new SimpleScheduler(this);

    DistributedJobParameter init = new XSLTInitParameter(new File((String) mXSLTFile.getValue()));
    mScheduler.setInitialParameter(init);

    mScheduler.addResultReceiver(this);

    return mScheduler;
  }

  @Override
  public void onReceiveResult(DistributedJobParameter param, DistributedJobResult result) {
    XSLTJobResult res = (XSLTJobResult) result;
    System.out.println(new String(res.data));
  }

  @Override
  public final void onSchedulerDone() {
  }

  @Override
  public final DistributedJobParameter getParameter() {
    mJobsDone++;
    return new XSLTJobParameter(new File((String) mXMLFile.getValue()));
  }

  @Override
  public final long getParametersLeft() {
    return TOTAL_JOBS - mJobsDone;
  }

  @Override
  public final boolean hasParametersLeft() {
    return (getParametersLeft() > 0);
  }
}
