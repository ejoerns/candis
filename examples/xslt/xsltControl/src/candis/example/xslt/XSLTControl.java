package candis.example.xslt;

import candis.distributed.DistributedControl;
import candis.distributed.DistributedJobParameter;
import candis.distributed.DistributedJobResult;
import candis.distributed.ResultReceiver;
import candis.distributed.Scheduler;
import candis.distributed.SimpleScheduler;
import java.io.File;

/**
 *
 * @author Sebastian Willenborg
 */
public class XSLTControl extends DistributedControl implements ResultReceiver {

  private static final String XSL_FILENAME = "../examples/xslt/xsltControl/student_text.xsl";
  private static final String[] XML_FILENAMES = {
    "../examples/xslt/xsltControl/student_directory.xml",
    "../examples/xslt/xsltControl/student_directory.xml",
    "../examples/xslt/xsltControl/student_directory.xml",
    "../examples/xslt/xsltControl/student_directory.xml",
    "../examples/xslt/xsltControl/student_directory.xml"
  };
  private int mFileCount = 0;
  private Scheduler mScheduler;

  @Override
  public Scheduler initScheduler() {

    mFileCount = 0;

    mScheduler = new SimpleScheduler(this);

    DistributedJobParameter init = new XSLTInitParameter(new File(XSL_FILENAME));
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
    mFileCount++;
    return new XSLTJobParameter(new File(XML_FILENAMES[mFileCount - 1]));
  }

  @Override
  public final long getParametersLeft() {
    return XML_FILENAMES.length - mFileCount;
  }

  @Override
  public final boolean hasParametersLeft() {
    return (getParametersLeft() > 0);
  }
}
