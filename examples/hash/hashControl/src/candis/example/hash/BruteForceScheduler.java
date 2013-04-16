package candis.example.hash;

import candis.distributed.DistributedJobParameter;
import candis.distributed.DistributedJobResult;
import candis.distributed.DroidData;
import candis.distributed.JobDistributionIO;
import candis.distributed.ResultReceiver;
import candis.distributed.Scheduler;
import candis.example.hash.HashInitParameter.HashType;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Sebastian Willenborg
 */
public class BruteForceScheduler extends Scheduler {

  public String resultValue;
  private int mMaxDepth;
  private BruteForceStringGenerator mStringGenerator;
  private long mDone;
  private long mTotal;
  private int mClientDepth;
  int prepart = 0;
  int prepart2 = 0;


  public BruteForceScheduler(int startLen, char[] alphabet, int maxDepth, int clientDepth, HashType type, String hash) {
    super();

  }

  private void init() {
//    addResultReceiver(this);
  }

  @Override
  protected void schedule(Map<String, DroidData> droidList, JobDistributionIO jobDistIO) {
    Iterator<Map.Entry<String, DroidData>> it = droidList.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, DroidData> droid = it.next();
      DroidData data = droid.getValue();
      String id = droid.getKey();
      it.remove();
      DistributedJobParameter param = popParameters();
      mRunningDroidsList.put(id, param);
      jobDistIO.startJob(id, param);
    }
  }



  @Override
  protected boolean hasParametersLeft() {
    return getParametersLeft() > 0;
  }

  @Override
  public int getParametersLeft() {
    // Abort if result found
    if (resultValue != null) {
      return 0;
    }
    return (int) (mTotal - mDone); // TODO: return long?
  }

  @Override
  protected DistributedJobParameter popParameters() {
    try {
      if (prepart > 0) {
        prepart--;
        return new HashJobParameter(mStringGenerator.toString().getBytes("UTF-8"), prepart2 - prepart);
      }
      return new HashJobParameter(mStringGenerator.nextString().getBytes("UTF-8"), Math.min(mClientDepth, mMaxDepth));
    }
    catch (UnsupportedEncodingException ex) {
      Logger.getLogger(BruteForceScheduler.class.getName()).log(Level.SEVERE, null, ex);
    }
    return null;
  }
}
